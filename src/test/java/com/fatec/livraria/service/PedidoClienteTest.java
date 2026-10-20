package com.fatec.livraria.service;

import com.fatec.livraria.model.FormularioEnderecoCheckout;
import com.fatec.livraria.model.FormularioPagamento;
import com.fatec.livraria.repository.LivroRepository;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1"
})
@Sql("/cenario-checkout.sql")
class PedidoClienteTest {

    @Autowired LivroRepository livros;
    @Autowired PedidoService pedidos;
    @Autowired ClienteService clientes;
    @Autowired FreteService frete;
    @Autowired UsuarioAtual usuario;
    @Autowired PedidoClienteService consulta;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;

    private MockMvc mvc;
    private Long pedidoId;

    @BeforeEach
    void preparar() {
        mvc = webAppContextSetup(contexto).build();

        Long livroId = livros.consultar("O Hobbit", null)
            .get(0).getId();

        Long cartaoId = clientes.exigirCliente(1L)
            .getCartoes().get(0).getId();

        var carrinho = new CarrinhoService(livros);
        carrinho.adicionar(livroId, 1);

        var checkout = new CheckoutService(
            clientes, carrinho, frete, usuario
        );

        var endereco = new FormularioEnderecoCheckout();

        endereco.setEnderecoId(
            checkout.listarEnderecos().get(0).getId()
        );

        checkout.confirmarEndereco(endereco);

        var formulario = new FormularioPagamento();
        formulario.setValoresCartoes(
            Map.of(cartaoId, "47.00")
        );

        pedidoId = pedidos.finalizar(
            carrinho.listar(),
            checkout.obterEntrega().getEndereco(),
            formulario
        );
    }

    @Test
    void consultaDadosReaisEProtegePedidosDeOutroCliente()
            throws Exception {

        jdbc.update("""
            INSERT INTO cliente (
                id, nome, genero, data_nascimento, cpf,
                telefone, email, senha_hash, ativo
            )
            SELECT
                2, 'Outro cliente', genero, data_nascimento,
                '12345678901', telefone,
                'outro@teste.local', senha_hash, ativo
            FROM cliente
            WHERE id = 1
            """);

        jdbc.update("""
            INSERT INTO pedido (
                cliente_id, data_pedido, status,
                endereco_entrega_json, subtotal_centavos,
                frete_centavos, desconto_centavos, total_centavos
            )
            SELECT
                2, data_pedido, status,
                endereco_entrega_json, subtotal_centavos,
                frete_centavos, desconto_centavos, total_centavos
            FROM pedido
            WHERE id = ?
            """, pedidoId);

        Long outroId = jdbc.queryForObject(
            "SELECT MAX(id) FROM pedido",
            Long.class
        );

        assertEquals(
            List.of(pedidoId),
            consulta.listar().stream()
                .map(pedido -> pedido.id())
                .toList()
        );

        var pedido = consulta.exigir(pedidoId);

        assertEquals(1, pedido.itens().size());
        assertEquals(1, pedido.pagamentos().size());

        assertEquals(
            "EM PROCESSAMENTO",
            consulta.historico(pedidoId).get(0).getStatus()
        );

        mvc.perform(get("/pedidos"))
            .andExpect(status().isOk());

        mvc.perform(get("/pedidos/" + pedidoId))
            .andExpect(status().isOk())
            .andExpect(content().string(
                containsString("O Hobbit")
            ));

        mvc.perform(get("/pedidos/" + outroId))
            .andExpect(status().isNotFound());

        mvc.perform(get("/pedidos/999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void bloqueiaFormularioAntesDaEntrega()
            throws Exception {

        mvc.perform(get("/pedidos/" + pedidoId + "/troca"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(
                "/pedidos/" + pedidoId
            ));

        jdbc.update(
            "UPDATE pedido SET status = 'ENTREGUE' WHERE id = ?",
            pedidoId
        );

        mvc.perform(get("/pedidos/" + pedidoId + "/troca"))
            .andExpect(status().isOk());
    }
}