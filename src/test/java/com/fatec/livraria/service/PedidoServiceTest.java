package com.fatec.livraria.service;

import com.fatec.livraria.model.*;
import com.fatec.livraria.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1"
})
@Sql("/cenario-checkout.sql")
class PedidoServiceTest {

    @Autowired PedidoService pedidos;
    @Autowired ClienteService clientes;
    @Autowired LivroRepository livros;
    @Autowired FreteService frete;
    @Autowired UsuarioAtual usuario;
    @Autowired JdbcTemplate jdbc;

    @MockitoSpyBean
    LogRepository logs;

    CarrinhoService carrinho;
    CheckoutService checkout;
    FinalizacaoCheckoutService finalizacao;
    FormularioPagamento formulario;

    @BeforeEach
    void preparar() {
        reset(logs);

        carrinho = new CarrinhoService(livros);
        carrinho.adicionar(
            livros.consultar("O Hobbit", null).get(0).getId(),
            1
        );

        checkout = new CheckoutService(
            clientes, carrinho, frete, usuario
        );

        var endereco = new FormularioEnderecoCheckout();
        endereco.setEnderecoId(
            checkout.listarEnderecos().get(0).getId()
        );
        checkout.confirmarEndereco(endereco);

        finalizacao = new FinalizacaoCheckoutService(
            pedidos, carrinho, checkout
        );

        formulario = new FormularioPagamento();
        formulario.setValoresCartoes(Map.of(
            clientes.exigirCliente(1L).getCartoes().get(0).getId(),
            checkout.obterEntrega().getTotal().toPlainString()
        ));
    }

    int contar(String tabela) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + tabela,
            Integer.class
        );
    }

    Long cupom(BigDecimal valor) {
        jdbc.update("""
            INSERT INTO cupom (
                codigo, tipo, cliente_id,
                valor_centavos, validade, ativo
            )
            VALUES (?, 'TROCA', 1, ?, '2099-01-01', 1)
            """,
            UUID.randomUUID().toString(),
            valor.movePointRight(2).longValueExact()
        );

        return jdbc.queryForObject(
            "SELECT last_insert_rowid()", Long.class
        );
    }

    @Test
    void gravaPedidoItensPagamentoELogsSemAvancarStatus() {
        var estoqueAntes = jdbc.queryForList(
            "SELECT * FROM item_estoque ORDER BY id"
        );

        Long id = finalizacao.finalizar(formulario);

        assertEquals(
            "EM PROCESSAMENTO",
            pedidos.buscarConfirmacao(id).status()
        );

        assertEquals(1, contar("pedido"));
        assertEquals(1, contar("item_pedido"));
        assertEquals(1, contar("pagamento"));

        assertEquals(
            "PENDENTE",
            jdbc.queryForObject(
                "SELECT status FROM pagamento", String.class
            )
        );

        for (String tabela :
                List.of("pedido", "item_pedido", "pagamento")) {

            assertEquals(
                1,
                jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM log_transacao
                    WHERE tabela = ?
                      AND operacao = 'INSERT'
                      AND registro_id IS NOT NULL
                    """,
                    Integer.class,
                    tabela
                )
            );
        }

        assertEquals(
            estoqueAntes,
            jdbc.queryForList(
                "SELECT * FROM item_estoque ORDER BY id"
            )
        );

        assertTrue(carrinho.listar().isEmpty());
        assertNull(checkout.obterEntrega());

        assertThrows(
            ValidacaoException.class,
            () -> finalizacao.finalizar(formulario)
        );

        assertEquals(1, contar("pedido"));
    }

    @Test
    void excedenteEmiteCupomComTrintaDiasELogDeCadaEscrita() {
        BigDecimal total = checkout.obterEntrega().getTotal();

        Long utilizado = cupom(
            total.add(new BigDecimal("5.00"))
        );

        formulario.setCuponsIds(List.of(utilizado));
        formulario.setValoresCartoes(Map.of());

        Long id = finalizacao.finalizar(formulario);

        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT ativo FROM cupom WHERE id = ?",
                Integer.class,
                utilizado
            )
        );

        var novo = jdbc.queryForMap(
            "SELECT * FROM cupom WHERE pedido_origem_id = ?",
            id
        );

        assertEquals(
            500,
            ((Number) novo.get("valor_centavos")).intValue()
        );

        assertEquals(
            java.time.LocalDate.now().plusDays(30).toString(),
            novo.get("validade")
        );

        assertEquals(5, contar("log_transacao"));

        assertEquals(
            "EM PROCESSAMENTO",
            pedidos.buscarConfirmacao(id).status()
        );

        long recebido = jdbc.queryForObject(
            "SELECT SUM(valor_centavos) FROM pagamento",
            Long.class
        );

        assertEquals(
            total.movePointRight(2).longValueExact(),
            recebido - 500
        );
    }

    @Test
    void falhaNoUltimoLogDesfazTudoEConservaSessao() {
        Long utilizado = cupom(
            checkout.obterEntrega().getTotal()
                .add(new BigDecimal("5.00"))
        );

        formulario.setCuponsIds(List.of(utilizado));
        formulario.setValoresCartoes(Map.of());

        doAnswer(chamada -> {
            LogTransacao log = chamada.getArgument(0);

            if ("cupom".equals(log.getTabela())
                    && "INSERT".equals(log.getOperacao())) {
                throw new IllegalStateException(
                    "Falha simulada no último log"
                );
            }

            return chamada.callRealMethod();

        }).when(logs).registrar(any());

        assertThrows(
            IllegalStateException.class,
            () -> finalizacao.finalizar(formulario)
        );

        for (String tabela : List.of(
                "pedido",
                "item_pedido",
                "pagamento",
                "log_transacao")) {

            assertEquals(0, contar(tabela), tabela);
        }

        assertEquals(1, contar("cupom"));

        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT ativo FROM cupom WHERE id = ?",
                Integer.class,
                utilizado
            )
        );

        assertFalse(carrinho.listar().isEmpty());
        assertNotNull(checkout.obterEntrega());
    }

    @Test
    void revalidaEstoqueEValoresAntesDeGravar() {
        Long livro = carrinho.listar()
            .get(0).getLivro().getId();

        jdbc.update(
            "UPDATE item_estoque SET quantidade = 0 WHERE livro_id = ?",
            livro
        );

        assertThrows(
            ValidacaoException.class,
            () -> finalizacao.finalizar(formulario)
        );

        assertEquals(0, contar("pedido"));
        assertFalse(carrinho.listar().isEmpty());
    }

    @Test
    void confirmacaoInexistenteOuDeOutroClienteNaoRetornaPedido() {
        Long id = finalizacao.finalizar(formulario);

        assertNull(
            pedidos.buscarConfirmacao(Long.MAX_VALUE)
        );

        jdbc.update("""
            INSERT INTO cliente (
                nome, genero, data_nascimento, cpf,
                telefone, email, senha_hash
            )
            VALUES (
                'Outro', 'Outro', '2000-01-01', '12345678901',
                '11999990000', 'pedido-outro@teste.local', 'hash'
            )
            """
        );

        Long outro = jdbc.queryForObject(
            "SELECT last_insert_rowid()", Long.class
        );

        jdbc.update(
            "UPDATE pedido SET cliente_id = ? WHERE id = ?",
            outro,
            id
        );

        assertNull(pedidos.buscarConfirmacao(id));
    }
}