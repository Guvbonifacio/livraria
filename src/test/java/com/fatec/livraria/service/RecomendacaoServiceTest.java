package com.fatec.livraria.service;

import com.fatec.livraria.model.ModeloRecomendacao;
import com.fatec.livraria.model.PeriodoRecomendacao;
import com.fatec.livraria.repository.RecomendacaoRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "livraria.carrinho.agendamento-ativo=false"
})
@Import(RecomendacaoServiceTest.RelogioTeste.class)
@Sql(scripts = {
    "/cenario-recomendacao.sql",
    "classpath:data.sql",
    "classpath:data-treino.sql"
})
class RecomendacaoServiceTest extends TesteComSessao {

    @TestConfiguration
    static class RelogioTeste {
        @Bean
        @Primary
        Clock relogioRecomendacaoTeste() {
            return Clock.fixed(
                Instant.parse("2026-11-18T12:00:00Z"),
                ZoneOffset.UTC
            );
        }
    }

    @Autowired RecomendacaoService service;
    @Autowired RecomendacaoRepository repository;
    @Autowired UsuarioAtual usuario;
    @Autowired JdbcTemplate jdbc;

    @Override
    protected String perfilInicial() {
        return "ADMIN";
    }

    @Test
    void perdaCaiENormalizacaoEModeloSaoPersistidos() {
        var amostras = service.extrairAmostras();

        assertEquals(80, amostras.size());

        for (int j = 0;
                j < ModeloRecomendacao.FEATURES.size();
                j++) {

            final int indice = j;

            assertEquals(
                0,
                amostras.stream()
                    .mapToDouble(a -> a.features().get(indice))
                    .average()
                    .orElseThrow(),
                1e-10
            );
        }

        var modelo = service.treinar();

        assertEquals(80, modelo.amostras());
        assertTrue(modelo.perdaFinal() < modelo.perdaInicial());
        assertEquals(6, modelo.coeficientes().size());
        assertEquals(4, modelo.vieses().size());
        assertNotNull(modelo.metricaValidacao());

        assertEquals(modelo, repository.buscar(modelo.id()));

        assertEquals(
            modelo.metricaValidacao(),
            service.avaliar(),
            1e-12
        );

        assertEquals(
            modelo,
            repository.buscar(modelo.id()),
            "Avaliação não altera os parâmetros."
        );

        var periodo = PeriodoRecomendacao.PADRAO;

        var antes = repository.extrair(
            periodo.corte(),
            periodo.observacaoExclusiva()
        );

        jdbc.update("""
            INSERT INTO preferencia_cliente (
                cliente_id, categoria_id,
                peso_declarado, vigente_de
            )
            SELECT 1, id, 999, '2026-11-16T10:00:00'
            FROM categoria
            WHERE nome = 'Fantasia'
            """);

        assertEquals(
            antes,
            repository.extrair(
                periodo.corte(),
                periodo.observacaoExclusiva()
            ),
            "Preferência posterior ao corte não muda o treino."
        );

        jdbc.update("""
            INSERT INTO recomendacao_mensagem (
                mensagem_chat_id, livro_id
            )
            SELECT m.id, l.id
            FROM mensagem_chat m
            JOIN conversa cv ON cv.id = m.conversa_id
            CROSS JOIN livro l
            WHERE cv.cliente_id = 1
              AND m.papel = 'ASSISTENTE'
              AND m.criada_em = '2026-10-15T10:00:01'
              AND l.isbn = '9788533613379'
            """);

        assertEquals(
            antes,
            repository.extrair(
                periodo.corte(),
                periodo.observacaoExclusiva()
            ),
            "Dois livros da mesma categoria contam um voto."
        );

        var nova = service.treinar();

        assertNotEquals(modelo.id(), nova.id());

        assertEquals(
            modelo,
            repository.buscar(modelo.id()),
            "A versão anterior permanece intacta."
        );

        assertThrows(
            ValidacaoException.class,
            () -> service.treinar(null)
        );

        usuario.simularPerfil("CLIENTE");

        assertThrows(
            ValidacaoException.class,
            service::treinar
        );
    }

    @Test
    void clientesUmETresRecebemListasDiferentes() {
        service.treinar();

        var a = service.recomendar(1L)
            .stream()
            .map(l -> l.getId())
            .toList();

        var b = service.recomendar(3L)
            .stream()
            .map(l -> l.getId())
            .toList();

        assertEquals(3, a.size());
        assertEquals(3, b.size());
        assertNotEquals(a, b);
    }

    @Test
    void aprendeCoeficienteNegativoEmMassaControlada() {
        massaControlada();

        var modelo = service.treinar();

        double coeficiente = modelo.coeficientes()
            .stream()
            .filter(c ->
                c.feature().equals("votos_nao_uteis")
            )
            .findFirst()
            .orElseThrow()
            .coeficiente();

        assertTrue(
            coeficiente < 0,
            "Coeficiente observado: " + coeficiente
        );
    }

    @Test
    void votoNegativoReduzScoreSemRetreinar() {
        massaControlada();

        var modeloFixo = service.treinar();

        for (long id : List.of(2001L, 2002L)) {
            jdbc.update("""
                INSERT INTO cliente (
                    id, nome, genero, data_nascimento,
                    cpf, telefone, email,
                    senha_hash, ativo, perfil
                )
                VALUES (
                    ?, ?, 'Outro', '1990-01-01',
                    ?, '11999990000', ?,
                    'hash-teste', 1, 'CLIENTE'
                )
                """,
                id,
                "Clone " + id,
                String.format("%011d", id),
                "clone" + id + "@teste.local"
            );
        }

        long categoria = jdbc.queryForObject("""
            SELECT id
            FROM categoria
            WHERE nome = 'Fantasia'
            """,
            Long.class
        );

        assertEquals(
            service.pontuarCategorias(2001L),
            service.pontuarCategorias(2002L)
        );

        feedback(
            2001L,
            categoria,
            0,
            "2026-11-16T10:00:00"
        );

        assertTrue(
            service.pontuarCategorias(2001L).get(categoria)
                < service.pontuarCategorias(2002L).get(categoria)
        );

        assertEquals(modeloFixo, repository.buscarAtual());
    }

    @Test
    void dunaNaoAparecePorqueEstaSemEstoque() {
        service.treinar();

        assertTrue(
            service.recomendar(1L)
                .stream()
                .noneMatch(l -> "Duna".equals(l.getTitulo()))
        );

        assertTrue(
            service.recomendar(3L)
                .stream()
                .noneMatch(l -> "Duna".equals(l.getTitulo()))
        );

        assertEquals(
            0,
            jdbc.queryForObject("""
                SELECT COALESCE(SUM(e.quantidade), 0)
                FROM livro l
                LEFT JOIN item_estoque e ON e.livro_id = l.id
                WHERE l.isbn = '9788576573135'
                """,
                Integer.class
            )
        );
    }

    private void massaControlada() {
        for (String tabela : List.of(
                "feedback_resposta",
                "recomendacao_mensagem",
                "mensagem_chat",
                "conversa",
                "preferencia_cliente",
                "pagamento",
                "item_pedido",
                "pedido",
                "log_transacao")) {

            jdbc.update("DELETE FROM " + tabela);
        }

        var clientes = jdbc.queryForList("""
            SELECT id
            FROM cliente
            WHERE perfil = 'CLIENTE'
            ORDER BY id
            """,
            Long.class
        );

        var categorias = jdbc.queryForList("""
            SELECT id
            FROM categoria
            ORDER BY id
            """,
            Long.class
        );

        for (int i = 0; i < clientes.size(); i++) {
            for (long categoria : categorias) {
                if (i % 2 == 0) {
                    feedback(
                        clientes.get(i),
                        categoria,
                        0,
                        "2026-10-15T10:00:00"
                    );
                } else {
                    feedback(
                        clientes.get(i),
                        categoria,
                        1,
                        "2026-11-05T10:00:00"
                    );
                }
            }
        }
    }

    private void feedback(
            long cliente,
            long categoria,
            int util,
            String data) {

        Long conversa = jdbc.queryForObject("""
            INSERT INTO conversa (cliente_id, iniciada_em)
            VALUES (?, ?)
            RETURNING id
            """,
            Long.class,
            cliente,
            data
        );

        Long mensagem = jdbc.queryForObject("""
            INSERT INTO mensagem_chat (
                conversa_id, papel, texto, criada_em
            )
            VALUES (
                ?, 'ASSISTENTE',
                'Recomendação de teste', ?
            )
            RETURNING id
            """,
            Long.class,
            conversa,
            data
        );

        Long livro = jdbc.queryForObject("""
            SELECT lc.livro_id
            FROM livro_categoria lc
            WHERE lc.categoria_id = ?
              AND (
                  SELECT COUNT(*)
                  FROM livro_categoria x
                  WHERE x.livro_id = lc.livro_id
              ) = 1
            ORDER BY lc.livro_id
            LIMIT 1
            """,
            Long.class,
            categoria
        );

        jdbc.update("""
            INSERT INTO recomendacao_mensagem (
                mensagem_chat_id, livro_id
            )
            VALUES (?, ?)
            """,
            mensagem,
            livro
        );

        jdbc.update("""
            INSERT INTO feedback_resposta (
                mensagem_chat_id, util, criada_em
            )
            VALUES (?, ?, ?)
            """,
            mensagem,
            util,
            data
        );
    }
}