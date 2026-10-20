package com.fatec.livraria.service;

import com.fatec.livraria.model.FormularioEnderecoCheckout;
import com.fatec.livraria.model.FormularioPagamento;
import com.fatec.livraria.model.FormularioTroca;
import com.fatec.livraria.repository.LivroRepository;
import com.fatec.livraria.repository.LogRepository;
import com.fatec.livraria.repository.PedidoRepository;

import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
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
class AutorizacaoTrocaTest {

    @Autowired LivroRepository livros;
    @Autowired PedidoRepository repository;
    @Autowired PedidoService pedidos;
    @Autowired ClienteService clientes;
    @Autowired FreteService frete;
    @Autowired UsuarioAtual usuario;
    @Autowired SolicitacaoTrocaService trocas;
    @Autowired AutorizacaoTrocaService autorizacao;
    @Autowired PedidoClienteService consulta;
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

        pedidoId = pedidos.finalizar(
            carrinho.listar(),
            checkout.obterEntrega().getEndereco(),
            pagamento
        );

        var itens = repository
            .buscarAdministrativo(pedidoId).itens();

        primeiro = itens.get(0).id();
        segundo = itens.get(1).id();
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

    @Test
    void autorizaParcialENotificaSemAlterarEstoqueOuCupons()
            throws Exception {

        entregar();

        Long trocaId = trocas.solicitar(
            pedidoId,
            formulario(Map.of(primeiro, "1"))
        );

        Long estoque = jdbc.queryForObject(
            "SELECT SUM(quantidade) FROM item_estoque",
            Long.class
        );
        int cupons = contar("cupom");

        var mvc = webAppContextSetup(contexto).build();

        mvc.perform(get("/admin/trocas"))
            .andExpect(status().isOk())
            .andExpect(content().string(
                containsString(
                    "data-troca-id=\"" + trocaId + "\""
                )
            ));

        mvc.perform(get("/admin/trocas/" + trocaId))
            .andExpect(status().isOk());

        mvc.perform(
                post("/admin/trocas/" + trocaId + "/analisar")
                    .param("decisao", "ACEITAR")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(
                "/admin/trocas/" + trocaId
            ))
            .andExpect(flash().attributeExists(
                "sucessoAutorizacao"
            ));

        var troca = trocas.listar(pedidoId).get(0);

        assertEquals("TROCA AUTORIZADA", troca.status());
        assertNotNull(troca.dataAutorizacao());

        assertEquals(
            "ENTREGUE",
            repository.buscarParaProcessar(pedidoId).status()
        );

        assertEquals(
            estoque,
            jdbc.queryForObject(
                "SELECT SUM(quantidade) FROM item_estoque",
                Long.class
            )
        );
        assertEquals(cupons, contar("cupom"));

        var notificacoes = consulta.notificacoes();

        assertEquals(1, notificacoes.size());
        assertEquals(1L, notificacoes.get(0).clienteId());
        assertFalse(notificacoes.get(0).lida());

        assertEquals(
            "A troca #" + trocaId
                + " do pedido #" + pedidoId
                + " foi autorizada.",
            notificacoes.get(0).mensagem()
        );

        assertEquals(
            1,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela = 'notificacao'
                  AND operacao = 'INSERT'
                """,
                Integer.class
            )
        );

        mvc.perform(get("/admin/trocas/" + trocaId))
            .andExpect(status().isOk())
            .andExpect(content().string(
                not(containsString(
                    "data-cy=\"autorizar-troca\""
                ))
            ));

        mvc.perform(get("/pedidos"))
            .andExpect(status().isOk())
            .andExpect(content().string(
                containsString(
                    notificacoes.get(0).mensagem()
                )
            ));
    }

    @Test
    void integralAtualizaCompraESoNotificaUmaVez() {
        entregar();

        Long trocaId = trocas.solicitar(
            pedidoId,
            formulario(Map.of(
                primeiro, "2",
                segundo, "1"
            ))
        );

        autorizacao.autorizar(trocaId, "ACEITAR");

        var data = trocas.listar(pedidoId)
            .get(0).dataAutorizacao();

        assertEquals(
            "TROCA AUTORIZADA",
            repository.buscarParaProcessar(pedidoId).status()
        );

        assertThrows(
            ValidacaoException.class,
            () -> autorizacao.autorizar(
                trocaId, "ACEITAR"
            )
        );

        assertEquals(
            data,
            trocas.listar(pedidoId)
                .get(0).dataAutorizacao()
        );

        assertEquals(1, contar("notificacao"));

        assertEquals(
            1,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela = 'troca'
                  AND operacao = 'UPDATE'
                  AND registro_id = ?
                """,
                Integer.class,
                trocaId
            )
        );

        assertEquals(
            "TROCA AUTORIZADA",
            consulta.historico(pedidoId)
                .get(0).getStatus()
        );
    }

    @Test
    void rejeitaDecisaoEEstadoInvalidos() {
        entregar();

        Long trocaId = trocas.solicitar(
            pedidoId,
            formulario(Map.of(primeiro, "1"))
        );

        assertThrows(
            ValidacaoException.class,
            () -> autorizacao.autorizar(
                trocaId, "NEGAR"
            )
        );

        assertEquals(
            "EM TROCA",
            trocas.listar(pedidoId).get(0).status()
        );

        jdbc.update(
            "UPDATE troca SET status = 'TROCADO' WHERE id = ?",
            trocaId
        );

        assertThrows(
            ValidacaoException.class,
            () -> autorizacao.autorizar(
                trocaId, "ACEITAR"
            )
        );

        assertEquals(0, contar("notificacao"));
    }

    @Test
    void trocaInexistenteRetorna404() throws Exception {
        var mvc = webAppContextSetup(contexto).build();

        mvc.perform(
                post("/admin/trocas/999999/analisar")
                    .param("decisao", "ACEITAR")
            )
            .andExpect(status().isNotFound());

        assertEquals(0, contar("notificacao"));
    }

    @Test
    void falhaNoLogDesfazAutorizacaoInteira() {
        entregar();

        Long trocaId = trocas.solicitar(
            pedidoId,
            formulario(Map.of(
                primeiro, "2",
                segundo, "1"
            ))
        );

        doThrow(
            new DataAccessResourceFailureException(
                "Falha simulada no log"
            )
        ).when(logs).registrar(
            argThat(log ->
                log != null
                && "notificacao".equals(log.getTabela())
            )
        );

        assertThrows(
            DataAccessResourceFailureException.class,
            () -> autorizacao.autorizar(
                trocaId, "ACEITAR"
            )
        );

        var troca = trocas.listar(pedidoId).get(0);

        assertEquals("EM TROCA", troca.status());
        assertNull(troca.dataAutorizacao());

        assertEquals(
            "EM TROCA",
            repository.buscarParaProcessar(pedidoId).status()
        );

        assertEquals(0, contar("notificacao"));

        assertEquals(
            0,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela = 'troca'
                  AND operacao = 'UPDATE'
                """,
                Integer.class
            )
        );
    }

    @Test
    void clienteNaoVeNotificacaoDeOutraPessoa()
            throws Exception {

        entregar();

        Long trocaId = trocas.solicitar(
            pedidoId,
            formulario(Map.of(primeiro, "1"))
        );
        autorizacao.autorizar(trocaId, "ACEITAR");

        jdbc.update("""
            INSERT INTO cliente (
                id, nome, genero, data_nascimento, cpf,
                telefone, email, senha_hash, ativo
            )
            SELECT
                2, 'Outro', genero, data_nascimento,
                '12345678901', telefone,
                'outro@teste.local', senha_hash, ativo
            FROM cliente
            WHERE id = 1
            """);

        jdbc.update("""
            INSERT INTO notificacao (
                cliente_id, mensagem, data_hora, lida
            )
            VALUES (
                2,
                'Mensagem exclusiva de outro cliente',
                '2026-10-20T10:00:00',
                0
            )
            """);

        assertEquals(1, consulta.notificacoes().size());
        assertEquals(
            1L,
            consulta.notificacoes().get(0).clienteId()
        );

        var mvc = webAppContextSetup(contexto).build();

        mvc.perform(get("/pedidos"))
            .andExpect(status().isOk())
            .andExpect(content().string(
                not(containsString(
                    "Mensagem exclusiva de outro cliente"
                ))
            ));
    }
}