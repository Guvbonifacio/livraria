package com.fatec.livraria.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "livraria.carrinho.agendamento-ativo=false"
})
@Sql("/cenario-checkout.sql")
class AnaliseVendasExportacaoTest extends TesteComSessao {

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioAtual usuario;
    @Autowired JdbcTemplate jdbc;

    @MockitoSpyBean
    AnaliseVendasService analise;

    private MockMvc mvc;

    @Override
    protected String perfilInicial() {
        return "ADMIN";
    }

    @BeforeEach
    void prepararMvc() {
        mvc = webAppContextSetup(contexto)
            .defaultRequest(get("/").session(sessao))
            .build();
    }

    private void venda(
            String data,
            String isbn,
            int quantidade,
            long preco) {

        long total = quantidade * preco;

        jdbc.update("""
            INSERT INTO pedido (
                cliente_id, data_pedido, status,
                endereco_entrega_json,
                subtotal_centavos, frete_centavos,
                desconto_centavos, total_centavos
            )
            VALUES (1, ?, 'APROVADA', '{}', ?, 0, 0, ?)
            """,
            data,
            total,
            total
        );

        Long id = jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );

        jdbc.update("""
            INSERT INTO item_pedido (
                pedido_id, livro_id, titulo,
                quantidade, preco_unitario_centavos
            )
            SELECT ?, id, titulo, ?, ?
            FROM livro
            WHERE isbn = ?
            """,
            id,
            quantidade,
            preco,
            isbn
        );
    }

    @Test
    void exportaMesmoServiceComBomCabecalhosEValoresDoGrafico()
            throws Exception {

        venda(
            "2026-01-15T12:00:00",
            "9788595084742",
            2,
            12345
        );

        venda(
            "2026-03-31T23:59:59",
            "9788595084742",
            1,
            7856
        );

        venda(
            "2026-04-01T00:00:00",
            "9788595084742",
            1,
            99999
        );

        var resposta = mvc.perform(
                get("/admin/analise/exportar")
                    .param("dataInicio", "2026-01-01")
                    .param("dataFim", "2026-03-31")
                    .param("categorias", "Fantasia")
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(
                new MediaType(
                    "text",
                    "csv",
                    StandardCharsets.UTF_8
                )
            ))
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"analise-vendas.csv\""
            ))
            .andReturn()
            .getResponse();

        byte[] bytes = resposta.getContentAsByteArray();

        assertArrayEquals(
            new byte[] {
                (byte) 0xEF,
                (byte) 0xBB,
                (byte) 0xBF
            },
            java.util.Arrays.copyOf(bytes, 3)
        );

        assertEquals(
            "\uFEFFperíodo;categoria;valor de venda\r\n"
            + "jan/2026;Fantasia;246,90\r\n"
            + "fev/2026;Fantasia;0,00\r\n"
            + "mar/2026;Fantasia;78,56\r\n",
            new String(bytes, StandardCharsets.UTF_8)
        );

        verify(analise, times(1)).analisar(
            "2026-01-01",
            "2026-03-31",
            List.of("Fantasia")
        );
    }

    @Test
    void exportaZerosDePeriodoSemVendas() throws Exception {
        var resposta = mvc.perform(
                get("/admin/analise/exportar")
                    .param("dataInicio", "2026-02-01")
                    .param("dataFim", "2026-02-28")
                    .param("categorias", "Fantasia")
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        assertEquals(
            "\uFEFFperíodo;categoria;valor de venda\r\n"
            + "fev/2026;Fantasia;0,00\r\n",
            new String(
                resposta.getContentAsByteArray(),
                StandardCharsets.UTF_8
            )
        );
    }

    @Test
    void clienteNaoRecebeArquivo() throws Exception {
        usuario.simularPerfil("CLIENTE");

        mvc.perform(get("/admin/analise/exportar"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(
                HttpHeaders.CONTENT_DISPOSITION
            ));
    }

    @Test
    void periodoInvalidoNaoProduzArquivo() throws Exception {
        mvc.perform(
                get("/admin/analise/exportar")
                    .param("dataInicio", "2026-03-01")
                    .param("dataFim", "2026-02-28")
            )
            .andExpect(status().isBadRequest())
            .andExpect(header().doesNotExist(
                HttpHeaders.CONTENT_DISPOSITION
            ));
    }

    @Test
    void escapaSeparadorEAspasNoNomeDaCategoria()
            throws Exception {

        String nome = "Ficção; \"Especial\"";

        jdbc.update(
            "UPDATE categoria SET nome = ? "
            + "WHERE nome = 'Ficção Científica'",
            nome
        );

        try {
            venda(
                "2026-02-15T12:00:00",
                "9788576573135",
                1,
                1999
            );

            var resposta = mvc.perform(
                    get("/admin/analise/exportar")
                        .param("dataInicio", "2026-02-01")
                        .param("dataFim", "2026-02-28")
                        .param("categorias", nome)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

            assertEquals(
                "\uFEFFperíodo;categoria;valor de venda\r\n"
                + "fev/2026;\"Ficção; \"\"Especial\"\"\";19,99\r\n",
                new String(
                    resposta.getContentAsByteArray(),
                    StandardCharsets.UTF_8
                )
            );

        } finally {
            jdbc.update(
                "UPDATE categoria SET nome = 'Ficção Científica' "
                + "WHERE nome = ?",
                nome
            );
        }
    }
}