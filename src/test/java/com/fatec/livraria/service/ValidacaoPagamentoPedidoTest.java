package com.fatec.livraria.service;

import com.fatec.livraria.model.FormularioEnderecoCheckout;
import com.fatec.livraria.model.FormularioPagamento;
import com.fatec.livraria.repository.LivroRepository;
import com.fatec.livraria.repository.PedidoRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1"
})
@Transactional
@Sql("/cenario-checkout.sql")
class ValidacaoPagamentoPedidoTest {

    @Autowired
    PedidoRepository repository;

    @Autowired
    PedidoService pedidos;

    @Autowired
    LivroRepository livros;

    @Autowired
    ClienteService clientes;

    @Autowired
    FreteService frete;

    @Autowired
    UsuarioAtual usuario;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ValidacaoPagamentoPedidoService validacao;

    CarrinhoService carrinho;
    CheckoutService checkout;
    FormularioPagamento formulario;

    @BeforeEach
    void preparar() {
        carrinho = new CarrinhoService(livros);

        carrinho.adicionar(
            livros.consultar("O Hobbit", null)
                .get(0)
                .getId(),
            1
        );

        checkout = new CheckoutService(
            clientes,
            carrinho,
            frete,
            usuario
        );

        var endereco = new FormularioEnderecoCheckout();

        endereco.setEnderecoId(
            checkout.listarEnderecos().get(0).getId()
        );

        checkout.confirmarEndereco(endereco);

        formulario = new FormularioPagamento();

        formulario.setValoresCartoes(
            Map.of(cartaoId(), "47.00")
        );
    }

    private Long cartaoId() {
        return clientes.exigirCliente(1L)
            .getCartoes()
            .get(0)
            .getId();
    }

    private Long finalizar() {
        return pedidos.finalizar(
            carrinho.listar(),
            checkout.obterEntrega().getEndereco(),
            formulario
        );
    }

    private boolean validar(
            Long pedidoId,
            boolean aceite) {

        return validacao.validar(
            repository.buscarParaProcessar(pedidoId),
            repository.listarPagamentosParaProcessar(pedidoId),
            aceite
        );
    }

    private Long criarCupom(
            String tipo,
            int valorCentavos) {

        jdbc.update("""
            INSERT INTO cupom (
                codigo,
                tipo,
                cliente_id,
                valor_centavos,
                validade,
                ativo
            )
            VALUES (?, ?, 1, ?, '2099-12-31', 1)
            """,
            UUID.randomUUID().toString(),
            tipo,
            valorCentavos
        );

        return jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );
    }

    @Test
    void cartaoAceitoOuRecusadoNaoGravaDuranteValidacao() {
        Long id = finalizar();

        assertTrue(validar(id, true));
        assertFalse(validar(id, false));

        assertEquals(
            "EM PROCESSAMENTO",
            repository.buscarParaProcessar(id).status()
        );

        assertEquals(
            "PENDENTE",
            repository.listarPagamentosParaProcessar(id)
                .get(0)
                .status()
        );
    }

    @Test
    void revalidaVencimentoDoCupomDepoisDaFinalizacao() {
        Long cupom = criarCupom("PROMOCIONAL", 4200);

        formulario.setCuponsIds(List.of(cupom));
        formulario.setValoresCartoes(
            Map.of(cartaoId(), "5.00")
        );

        Long id = finalizar();

        assertTrue(validar(id, true));

        jdbc.update(
            "UPDATE cupom SET validade = '2000-01-01' WHERE id = ?",
            cupom
        );

        assertFalse(validar(id, true));
    }

    @Test
    void cupomTrocaComprometidoNestePedidoContinuaValido() {
        Long cupom = criarCupom("TROCA", 4700);

        formulario.setCuponsIds(List.of(cupom));
        formulario.setValoresCartoes(Map.of());

        Long id = finalizar();

        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT ativo FROM cupom WHERE id = ?",
                Integer.class,
                cupom
            )
        );

        assertTrue(validar(id, true));

        // Sem cartão, a compra não depende da operadora.
        assertTrue(validar(id, false));
    }

    @Test
    void recusaValorAlteradoOuCupomPromocionalInativo() {
        Long cupom = criarCupom("PROMOCIONAL", 4200);

        formulario.setCuponsIds(List.of(cupom));
        formulario.setValoresCartoes(
            Map.of(cartaoId(), "5.00")
        );

        Long id = finalizar();

        jdbc.update(
            "UPDATE cupom SET valor_centavos = 4300 WHERE id = ?",
            cupom
        );

        assertFalse(validar(id, true));

        jdbc.update("""
            UPDATE cupom
            SET valor_centavos = 4200,
                ativo = 0
            WHERE id = ?
            """,
            cupom
        );

        assertFalse(validar(id, true));
    }

    @Test
    void somaPagamentosDeveConferirComTotalEExcedente() {
        Long id = finalizar();

        jdbc.update("""
            UPDATE pagamento
            SET valor_centavos = 4600
            WHERE pedido_id = ?
            """,
            id
        );

        assertFalse(validar(id, true));
    }
}