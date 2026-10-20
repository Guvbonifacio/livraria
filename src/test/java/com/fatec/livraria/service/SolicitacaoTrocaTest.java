package com.fatec.livraria.service;

import com.fatec.livraria.model.FormularioEnderecoCheckout;
import com.fatec.livraria.model.FormularioPagamento;
import com.fatec.livraria.model.FormularioTroca;
import com.fatec.livraria.repository.LivroRepository;
import com.fatec.livraria.repository.LogRepository;
import com.fatec.livraria.repository.PedidoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1"
})
@Sql("/cenario-checkout.sql")
class SolicitacaoTrocaTest {

    @Autowired LivroRepository livros;
    @Autowired PedidoRepository repository;
    @Autowired PedidoService pedidos;
    @Autowired ClienteService clientes;
    @Autowired FreteService frete;
    @Autowired UsuarioAtual usuario;
    @Autowired SolicitacaoTrocaService trocas;
    @Autowired ProcessamentoPedidoService processamento;
    @Autowired EntregaPedidoService entrega;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;

    @MockitoSpyBean
    LogRepository logs;

    private Long pedidoId;
    private Long primeiro;
    private Long segundo;

    @BeforeEach
    void preparar() {
        reset(logs);

        pedidoId = criarPedido();

        var itens = repository
            .buscarAdministrativo(pedidoId).itens();

        primeiro = itens.get(0).id();
        segundo = itens.get(1).id();
    }

    private Long criarPedido() {
        var lista = livros.consultar(null, null)
            .stream()
            .filter(livro -> livro.getEstoque() >= 4)
            .toList();

        var carrinho = new CarrinhoService(livros);
        carrinho.adicionar(lista.get(0).getId(), 2);
        carrinho.adicionar(lista.get(1).getId(), 1);

        var checkout = new CheckoutService(
            clientes, carrinho, frete, usuario
        );

        var endereco = new FormularioEnderecoCheckout();
        endereco.setEnderecoId(
            checkout.listarEnderecos().get(0).getId()
        );
        checkout.confirmarEndereco(endereco);

        Long cartaoId = clientes.exigirCliente(1L)
            .getCartoes().get(0).getId();

        var pagamento = new FormularioPagamento();

        pagamento.setValoresCartoes(
            Map.of(
                cartaoId,
                checkout.obterEntrega().getTotal().toPlainString()
            )
        );

        return pedidos.finalizar(
            carrinho.listar(),
            checkout.obterEntrega().getEndereco(),
            pagamento
        );
    }

    private void entregar() {
        processamento.processar(pedidoId, "APROVADA");
        entrega.transicionar(pedidoId, "EM TRANSPORTE");
        entrega.transicionar(pedidoId, "ENTREGUE");
    }

    private FormularioTroca formulario(
            Map<Long, String> valores) {

        var formulario = new FormularioTroca();

        formulario.setItensIds(
            new ArrayList<>(valores.keySet())
        );
        formulario.setQuantidades(valores);

        return formulario;
    }

    private int contar(String tabela) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + tabela,
            Integer.class
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "EM PROCESSAMENTO",
        "APROVADA",
        "REPROVADA",
        "EM TRANSPORTE"
    })
    void cenario8BloqueiaPedidoNaoEntregue(String situacao)
            throws Exception {

        jdbc.update(
            "UPDATE pedido SET status = ? WHERE id = ?",
            situacao,
            pedidoId
        );

        assertThrows(
            ValidacaoException.class,
            () -> trocas.solicitar(
                pedidoId,
                formulario(Map.of(primeiro, "1"))
            )
        );

        var mvc = webAppContextSetup(contexto).build();

        mvc.perform(
                post("/pedidos/" + pedidoId + "/troca")
                    .param("itensIds", primeiro.toString())
                    .param("quantidades[" + primeiro + "]", "1")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(
                flash().attributeExists("errosTroca")
            );

        assertEquals(0, contar("troca"));
        assertEquals(0, contar("item_troca"));
    }

    @Test
    void cenario9TrocaParcialMantemPedidoEntregue()
            throws Exception {

        entregar();

        var mvc = webAppContextSetup(contexto).build();

        // Sem motivo: ele é opcional.
        mvc.perform(
                post("/pedidos/" + pedidoId + "/troca")
                    .param("itensIds", primeiro.toString())
                    .param("quantidades[" + primeiro + "]", "1")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(
                flash().attributeExists("sucessoTroca")
            );

        assertEquals(
            "ENTREGUE",
            repository.buscarParaProcessar(pedidoId).status()
        );

        var troca = trocas.listar(pedidoId).get(0);

        assertEquals("EM TROCA", troca.status());
        assertEquals("", troca.motivo());
        assertEquals(1, contar("item_troca"));

        assertEquals(
            2,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela IN ('troca', 'item_troca')
                """,
                Integer.class
            )
        );

        mvc.perform(get("/pedidos/" + pedidoId))
            .andExpect(status().isOk());

        mvc.perform(get("/pedidos/" + pedidoId + "/troca"))
            .andExpect(status().isOk());
    }

    @Test
    void cenario9TrocaIntegralAtualizaTambemPedido() {
        entregar();

        Long trocaId = trocas.solicitar(
            pedidoId,
            formulario(
                Map.of(primeiro, "2", segundo, "1")
            )
        );

        assertEquals(
            "EM TROCA",
            repository.buscarParaProcessar(pedidoId).status()
        );

        assertEquals(
            "EM TROCA",
            trocas.listar(pedidoId).get(0).status()
        );

        assertEquals(2, contar("item_troca"));

        assertEquals(
            1,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela = 'troca'
                  AND registro_id = ?
                """,
                Integer.class,
                trocaId
            )
        );
    }

    @Test
    void impedeMesmoItemEmDuasSolicitacoes() {
        entregar();

        trocas.solicitar(
            pedidoId,
            formulario(Map.of(primeiro, "1"))
        );

        assertThrows(
            ValidacaoException.class,
            () -> trocas.solicitar(
                pedidoId,
                formulario(Map.of(primeiro, "1"))
            )
        );

        assertEquals(1, contar("troca"));
        assertEquals(1, contar("item_troca"));
    }

    @Test
    void rejeitaQuantidadesInvalidasEItemDeOutroPedido() {
        entregar();

        for (String valor : List.of(
                "0", "-1", "3", "1.5", "abc", "999999999999")) {

            assertThrows(
                ValidacaoException.class,
                () -> trocas.solicitar(
                    pedidoId,
                    formulario(Map.of(primeiro, valor))
                )
            );
        }

        Long outroPedido = criarPedido();

        Long outroItem = repository
            .buscarAdministrativo(outroPedido)
            .itens().get(0).id();

        assertThrows(
            ValidacaoException.class,
            () -> trocas.solicitar(
                pedidoId,
                formulario(Map.of(outroItem, "1"))
            )
        );

        assertThrows(
            ValidacaoException.class,
            () -> trocas.solicitar(
                pedidoId,
                formulario(Map.of())
            )
        );

        assertEquals(0, contar("troca"));
    }

    @Test
    void falhaNoLogDesfazTrocaEItens() {
        entregar();

        doThrow(
            new DataAccessResourceFailureException(
                "Falha simulada no log"
            )
        ).when(logs).registrar(
            argThat(log ->
                "item_troca".equals(log.getTabela())
            )
        );

        assertThrows(
            DataAccessResourceFailureException.class,
            () -> trocas.solicitar(
                pedidoId,
                formulario(
                    Map.of(primeiro, "2", segundo, "1")
                )
            )
        );

        assertEquals(0, contar("troca"));
        assertEquals(0, contar("item_troca"));

        assertEquals(
            "ENTREGUE",
            repository.buscarParaProcessar(pedidoId).status()
        );

        assertEquals(
            0,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela IN ('troca', 'item_troca')
                """,
                Integer.class
            )
        );
    }
}