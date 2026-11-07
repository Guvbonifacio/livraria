package com.fatec.livraria.service;

import com.fatec.livraria.model.CartaoCredito;
import com.fatec.livraria.model.FormularioEnderecoCheckout;
import com.fatec.livraria.model.FormularioPagamento;
import com.fatec.livraria.repository.LivroRepository;
import com.fatec.livraria.repository.PedidoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Sem @Transactional: verifica o rollback real do PedidoService.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1"
})
@Sql("/cenario-checkout.sql")
class NovoCartaoCheckoutTest extends TesteComSessao {

    @Autowired LivroRepository livros;
    @Autowired ClienteService clientes;
    @Autowired FreteService frete;
    @Autowired UsuarioAtual usuario;
    @Autowired PagamentoService pagamentos;
    @Autowired PedidoService pedidos;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired NotificacaoEstoqueService notificacoesEstoque;
    @Autowired ReservaCarrinhoService reservas;

    @MockitoSpyBean
    PedidoRepository repository;

    private CarrinhoService carrinho;
    private CheckoutService checkout;
    private FormularioPagamento formulario;
    private Long cartaoId;

    @BeforeEach
    void preparar() {
        reset(repository);

        Long livroId = livros.consultar("O Hobbit", null)
            .get(0).getId();

        cartaoId = clientes.exigirCliente(1L)
            .getCartoes().get(0).getId();

        carrinho = new CarrinhoService(
            livros,
            notificacoesEstoque,
            reservas
        );
        carrinho.adicionar(livroId, 1);

        checkout = new CheckoutService(
            clientes, carrinho, frete, usuario
        );

        var endereco = new FormularioEnderecoCheckout();
        endereco.setEnderecoId(
            checkout.listarEnderecos().get(0).getId()
        );
        checkout.confirmarEndereco(endereco);

        formulario = new FormularioPagamento();
        formulario.setUsarNovoCartao(true);
        formulario.setNovoCartao(
            new CartaoCredito(
                null,
                "5555555555554444",
                "NOVO CARTAO",
                "Mastercard",
                "8642",
                false
            )
        );
        formulario.setValorNovoCartao("47.00");
        formulario.setSalvarCartaoPerfil(true);
    }

    private Long finalizar() {
        return pedidos.finalizar(
            carrinho.listar(),
            checkout.obterEntrega().getEndereco(),
            formulario,
            carrinho.getSessaoId()
        );
    }

    private int contar(String tabela) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + tabela,
            Integer.class
        );
    }

    private Long cupom(int valor) {
        jdbc.update(
            """
            INSERT INTO cupom (
                codigo, tipo, cliente_id,
                valor_centavos, validade, ativo
            )
            VALUES (?, 'TROCA', 1, ?, '2099-12-31', 1)
            """,
            UUID.randomUUID().toString(),
            valor
        );

        return jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );
    }

    @Test
    void incorporaCartaoEPersisteSomenteNumeroMascarado() {
        Long id = finalizar();

        assertEquals(
            2,
            clientes.exigirCliente(1L).getCartoes().size()
        );

        var parcelas = repository
            .buscarAdministrativo(id).pagamentos();

        assertEquals(1, parcelas.size());

        var pagamento = parcelas.get(0);

        assertEquals("CARTAO", pagamento.forma());
        assertEquals(
            new BigDecimal("47.00"),
            pagamento.valor()
        );
        assertEquals("Mastercard", pagamento.bandeira());
        assertEquals(
            "**** **** **** 4444",
            pagamento.numeroMascarado()
        );

        Long salvo = jdbc.queryForObject(
            "SELECT cartao_id FROM pagamento WHERE pedido_id = ?",
            Long.class,
            id
        );

        assertNotNull(salvo);
        assertNotEquals(cartaoId, salvo);

        assertEquals(
            1,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM cartao_credito
                WHERE id = ? AND cliente_id = 1
                """,
                Integer.class,
                salvo
            )
        );

        assertEquals(
            "5555555555554444",
            formulario.getNovoCartao().getNumero()
        );

        String dados = json.writeValueAsString(
            jdbc.queryForList(
                "SELECT dados FROM log_transacao"
            )
        );

        assertFalse(dados.contains("5555555555554444"));
        assertFalse(dados.contains("8642"));
        assertFalse(dados.contains("codigoSeguranca"));

        assertEquals(
            1,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM log_transacao
                WHERE tabela = 'cartao_credito'
                  AND operacao = 'INSERT'
                """,
                Integer.class
            )
        );
    }

    @Test
    void usaSemIncorporarMantendoCartaoIdNulo() {
        formulario.setSalvarCartaoPerfil(false);

        Long id = finalizar();

        assertEquals(1, contar("cartao_credito"));

        assertNull(
            jdbc.queryForObject(
                "SELECT cartao_id FROM pagamento WHERE pedido_id = ?",
                Long.class,
                id
            )
        );

        assertEquals(
            "**** **** **** 4444",
            repository.buscarAdministrativo(id)
                .pagamentos().get(0).numeroMascarado()
        );
    }

    @Test
    void prepararNaoGravaNemIncluiNumeroCompletoOuCvvNoPlano() {
        var plano = pagamentos.preparar(
            formulario,
            new BigDecimal("47.00")
        );

        assertNull(plano.parcelas().get(0).cartaoId());

        String dados = json.writeValueAsString(plano);

        assertFalse(dados.contains("5555555555554444"));
        assertFalse(dados.contains("8642"));

        assertEquals(1, contar("cartao_credito"));
        assertEquals(0, contar("pedido"));
    }

    @Test
    void rn0035AceitaCincoReaisNoNovoCartaoAposCupom() {
        formulario.setCuponsIds(
            List.of(cupom(4200))
        );
        formulario.setValorNovoCartao("5.00");
        formulario.setSalvarCartaoPerfil(false);

        Long id = finalizar();

        assertEquals(
            2,
            repository.listarPagamentosParaProcessar(id).size()
        );

        assertEquals(
            1,
            jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM pagamento
                WHERE pedido_id = ?
                  AND forma = 'CARTAO'
                  AND valor_centavos = 500
                  AND cartao_id IS NULL
                """,
                Integer.class,
                id
            )
        );
    }

    @Test
    void rn0034RecusaNovoCartaoAbaixoDoMinimo() {
        formulario.setValoresCartoes(
            Map.of(cartaoId, "37.01")
        );
        formulario.setValorNovoCartao("9.99");

        assertThrows(
            ValidacaoException.class,
            this::finalizar
        );

        assertEquals(1, contar("cartao_credito"));
        assertEquals(0, contar("pedido"));
    }

    @Test
    void rn0035NaoAplicaExcecaoAUmSalvoMaisUmNovo() {
        formulario.setCuponsIds(
            List.of(cupom(4200))
        );
        formulario.setValoresCartoes(
            Map.of(cartaoId, "2.00")
        );
        formulario.setValorNovoCartao("3.00");

        assertThrows(
            ValidacaoException.class,
            this::finalizar
        );

        assertEquals(0, contar("pedido"));
        assertEquals(1, contar("cartao_credito"));
    }

    @Test
    void falhaNoPedidoDesfazCartaoEPreservaFormulario() {
        int logsAntes = contar("log_transacao");

        doThrow(
            new DataAccessResourceFailureException(
                "Falha simulada"
            )
        ).when(repository).salvarPedido(
            anyLong(),
            anyString(),
            anyLong(),
            anyLong()
        );

        assertThrows(
            DataAccessResourceFailureException.class,
            this::finalizar
        );

        assertEquals(1, contar("cartao_credito"));
        assertEquals(0, contar("pedido"));
        assertEquals(logsAntes, contar("log_transacao"));

        assertEquals(
            "5555555555554444",
            formulario.getNovoCartao().getNumero()
        );
        assertNull(formulario.getNovoCartao().getId());

        // O mesmo formulário continua válido para uma nova tentativa.
        reset(repository);

        assertNotNull(finalizar());
        assertEquals(2, contar("cartao_credito"));
    }
}