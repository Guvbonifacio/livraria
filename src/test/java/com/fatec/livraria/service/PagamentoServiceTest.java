package com.fatec.livraria.service;

import com.fatec.livraria.model.FormularioEnderecoCheckout;
import com.fatec.livraria.repository.LivroRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.fatec.livraria.repository.PedidoRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1"
    
})

@org.springframework.test.context.jdbc.Sql(
    "/cenario-checkout.sql"
)


@Transactional
class PagamentoServiceTest {

    
    @Autowired ClienteService clientes;
    @Autowired LivroRepository livros;
    @Autowired FreteService frete;
    @Autowired UsuarioAtual usuario;
    @Autowired JdbcTemplate jdbc;
    @Autowired PedidoRepository pedidos;

    CarrinhoService carrinho;
    CheckoutService checkout;
    PagamentoService pagamentos;

    Long primeiro;
    Long segundo;

    @BeforeEach
    void preparar() {
        pagamentos = new PagamentoService(
    clientes,
    checkout,
    usuario,
    pedidos
);
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

pagamentos = new PagamentoService(
    clientes, checkout, usuario, pedidos
);

        primeiro = pagamentos.listarCartoes().get(0).getId();

        jdbc.update("""
            INSERT INTO cartao_credito
                (cliente_id, numero_mascarado, nome_impresso,
                 bandeira, preferencial)
            VALUES (?, '**** **** **** 2222', 'Teste', 'Visa', 0)
            """, usuario.getClienteId());

        segundo = jdbc.queryForObject(
            "SELECT last_insert_rowid()", Long.class
        );
    }

    BigDecimal total() {
        return checkout.obterEntrega().getTotal();
    }

    @Test
    void aceitaUmCartaoComTotalIncluindoFrete() {
        assertDoesNotThrow(() ->
            pagamentos.validarCartoes(
                Map.of(primeiro, total().toPlainString())
            )
        );
    }

    @Test
    void aceitaDivisaoComMinimoExatoDeDezReais() {
        assertDoesNotThrow(() ->
            pagamentos.validarCartoes(Map.of(
                primeiro, "10.00",
                segundo,
                total()
                    .subtract(new BigDecimal("10.00"))
                    .toPlainString()
            ))
        );
    }

@Test
void recusaParcelaDeNoveENoventaENoveMesmoComSomaCorreta() {
    var erro = assertThrows(
        ValidacaoException.class,
        () -> pagamentos.validarCartoes(Map.of(
            primeiro, "9.99",
            segundo,
            total()
                .subtract(new BigDecimal("9.99"))
                .toPlainString()
        ))
    );

    assertTrue(
        erro.getErros().contains(
            "O valor m\u00ednimo por cart\u00e3o \u00e9 R$ 10,00."
        ),
        () -> "Mensagens recebidas do Service: " + erro.getErros()
    );
}

    @Test
    void recusaSomaMenorOuMaiorQueTotal() {
        for (String diferenca : new String[]{"-0.01", "0.01"}) {
            assertThrows(
                ValidacaoException.class,
                () -> pagamentos.validarCartoes(Map.of(
                    primeiro,
                    total()
                        .add(new BigDecimal(diferenca))
                        .toPlainString()
                ))
            );
        }
    }

    @Test
    void recusaCartaoPertencenteAOutroCliente() {
        jdbc.update("""
            INSERT INTO cliente
                (nome, genero, data_nascimento, cpf,
                 telefone, email, senha_hash)
            VALUES
                ('Outro', 'Outro', '2000-01-01', '12345678901',
                 '11999990000', 'outro-pagamento@teste.local',
                 'hash-teste')
            """);

        Long outro = jdbc.queryForObject(
            "SELECT last_insert_rowid()", Long.class
        );

        jdbc.update("""
            INSERT INTO cartao_credito
                (cliente_id, numero_mascarado, nome_impresso,
                 bandeira, preferencial)
            VALUES (?, '**** **** **** 3333', 'Outro', 'Visa', 0)
            """, outro);

        Long cartaoOutro = jdbc.queryForObject(
            "SELECT last_insert_rowid()", Long.class
        );

        assertFalse(
            pagamentos.listarCartoes().stream()
                .anyMatch(c -> c.getId().equals(cartaoOutro))
        );

        assertThrows(
            ValidacaoException.class,
            () -> pagamentos.validarCartoes(
                Map.of(cartaoOutro, total().toPlainString())
            )
        );
    }

    @Test
    void recusaValoresInvalidosSemArredondar() {
        for (String invalido :
                new String[]{"10.001", "-10", "0", "abc", "1e2"}) {

            assertThrows(
                ValidacaoException.class,
                () -> pagamentos.validarCartoes(
                    Map.of(primeiro, invalido)
                ),
                invalido
            );
        }
    }

    @Test
    void exigePeloMenosUmCartao() {
        assertThrows(
            ValidacaoException.class,
            () -> pagamentos.validarCartoes(Map.of())
        );

        assertThrows(
            ValidacaoException.class,
            () -> pagamentos.validarCartoes(
                Map.of(primeiro, "  ")
            )
        );
    }

    @Test
    void exigeEntregaConfirmada() {
        checkout.limparEntrega();

        assertThrows(
            ValidacaoException.class,
            () -> pagamentos.validarCartoes(
                Map.of(primeiro, "50.00")
            )
        );
    }

    @Test
    void recalculaTotalSeCarrinhoMudar() {
        String anterior = total().toPlainString();

        carrinho.alterarQuantidade(
            carrinho.listar().get(0).getLivro().getId(),
            2
        );

        assertThrows(
            ValidacaoException.class,
            () -> pagamentos.validarCartoes(
                Map.of(primeiro, anterior)
            )
        );
    }

    @Test
    void validarNaoCriaPedidoNemPagamento() {
        Integer pedidos = jdbc.queryForObject(
            "SELECT COUNT(*) FROM pedido", Integer.class
        );

        Integer registros = jdbc.queryForObject(
            "SELECT COUNT(*) FROM pagamento", Integer.class
        );

        pagamentos.validarCartoes(
            Map.of(primeiro, total().toPlainString())
        );

        assertEquals(
            pedidos,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM pedido", Integer.class
            )
        );

        assertEquals(
            registros,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM pagamento", Integer.class
            )
        );
    }
}