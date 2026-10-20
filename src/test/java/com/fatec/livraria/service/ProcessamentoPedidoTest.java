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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1"
})
@Sql("/cenario-checkout.sql")
class ProcessamentoPedidoTest {

    @Autowired
    LivroRepository livros;

    @Autowired
    PedidoRepository repository;

    @Autowired
    PedidoService pedidos;

    @Autowired
    ClienteService clientes;

    @Autowired
    FreteService frete;

    @Autowired
    UsuarioAtual usuario;

    @Autowired
    ProcessamentoPedidoService processamento;

    @Autowired
    JdbcTemplate jdbc;

    CarrinhoService carrinho;
    CheckoutService checkout;
    FormularioPagamento formulario;

    Long livroId;
    Long cartaoId;

    @BeforeEach
    void preparar() {
        livroId = livros.consultar("O Hobbit", null)
            .get(0)
            .getId();

        cartaoId = clientes.exigirCliente(1L)
            .getCartoes()
            .get(0)
            .getId();

        carrinho = new CarrinhoService(livros);
        carrinho.adicionar(livroId, 1);

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
            Map.of(cartaoId, "47.00")
        );
    }

    private Long finalizar() {
        return pedidos.finalizar(
            carrinho.listar(),
            checkout.obterEntrega().getEndereco(),
            formulario
        );
    }

    private long estoqueFisico() {
        return jdbc.queryForObject("""
            SELECT COALESCE(SUM(quantidade), 0)
            FROM item_estoque
            WHERE livro_id = ?
            """,
            Long.class,
            livroId
        );
    }

    @Test
    void aprovacaoBaixaEstoqueUmaUnicaVez() {
        long antes = estoqueFisico();

        Long id = finalizar();

        assertEquals(
            "EM PROCESSAMENTO",
            repository.buscarParaProcessar(id).status()
        );

        // Finalizar ainda não debita o estoque.
        assertEquals(antes, estoqueFisico());

        assertEquals(
            "APROVADA",
            processamento.processar(id, "APROVADA")
        );

        assertEquals(
            "APROVADA",
            repository.buscarParaProcessar(id).status()
        );

        assertEquals(
            antes - 1,
            estoqueFisico()
        );

        assertEquals(
            "APROVADO",
            repository.listarPagamentosParaProcessar(id)
                .get(0)
                .status()
        );

        // Uma segunda tentativa não pode debitar novamente.
        assertThrows(
            ValidacaoException.class,
            () -> processamento.processar(id, "APROVADA")
        );

        assertEquals(
            antes - 1,
            estoqueFisico()
        );

        assertEquals(
            1,
            jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela = 'item_estoque'
                  AND operacao = 'UPDATE'
                """,
                Integer.class
            )
        );
    }

    @Test
    void reprovacaoPreservaEstoque() {
        long antes = estoqueFisico();

        Long id = finalizar();

        assertEquals(
            "REPROVADA",
            processamento.processar(id, "REPROVADA")
        );

        assertEquals(
            "REPROVADA",
            repository.buscarParaProcessar(id).status()
        );

        assertEquals(antes, estoqueFisico());

        assertEquals(
            "RECUSADO",
            repository.listarPagamentosParaProcessar(id)
                .get(0)
                .status()
        );

        assertEquals(
            0,
            jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela = 'item_estoque'
                """,
                Integer.class
            )
        );
    }

    @Test
    void aprovaCupomComCartaoDeCincoReais() {
        jdbc.update("""
            INSERT INTO cupom (
                codigo,
                tipo,
                cliente_id,
                valor_centavos,
                validade,
                ativo
            )
            VALUES (?, 'TROCA', 1, 4200, '2099-12-31', 1)
            """,
            UUID.randomUUID().toString()
        );

        Long cupomId = jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );

        formulario.setCuponsIds(List.of(cupomId));

        formulario.setValoresCartoes(
            Map.of(cartaoId, "5.00")
        );

        long antes = estoqueFisico();

        Long id = finalizar();

        assertEquals(
            "APROVADA",
            processamento.processar(id, "APROVADA")
        );

        assertEquals(
            antes - 1,
            estoqueFisico()
        );

        var registrados =
            repository.listarPagamentosParaProcessar(id);

        assertEquals(2, registrados.size());

        assertTrue(
            registrados.stream().allMatch(
                pagamento ->
                    "APROVADO".equals(pagamento.status())
            )
        );

        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT ativo FROM cupom WHERE id = ?",
                Integer.class,
                cupomId
            )
        );
    }
}