package com.fatec.livraria.service;

import com.fatec.livraria.model.PeriodoRecomendacao;
import com.fatec.livraria.repository.RecomendacaoRepository;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "livraria.carrinho.agendamento-ativo=false"
})
@Sql(scripts = {
    "/cenario-recomendacao.sql",
    "classpath:data.sql",
    "classpath:data-treino.sql"
})
class MassaTreinavelTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired RecomendacaoRepository repository;

    @Test
    void possuiVinteClientesComCpfValidoEOitentaAmostras() {
        var cpfs = jdbc.queryForList("""
            SELECT cpf
            FROM cliente
            WHERE perfil = 'CLIENTE'
            """,
            String.class
        );

        assertEquals(20, cpfs.size());
        assertEquals(20, cpfs.stream().distinct().count());

        for (String cpf : cpfs) {
            assertTrue(cpf.matches("[0-9]{11}"), cpf);
            assertFalse(cpf.matches("([0-9])\\1{10}"), cpf);

            for (int tamanho : List.of(9, 10)) {
                int soma = 0;

                for (int i = 0; i < tamanho; i++) {
                    soma += (cpf.charAt(i) - '0')
                        * (tamanho + 1 - i);
                }

                int resto = soma % 11;

                assertEquals(
                    resto < 2 ? 0 : 11 - resto,
                    cpf.charAt(tamanho) - '0',
                    cpf
                );
            }
        }

        var periodo = PeriodoRecomendacao.PADRAO;

        var treino = repository.extrair(
            periodo.corte(),
            periodo.observacaoExclusiva()
        );

        var validacao = repository.extrair(
            periodo.observacaoExclusiva(),
            periodo.validacaoExclusiva()
        );

        assertEquals(80, treino.size());
        assertEquals(80, validacao.size());

        assertTrue(treino.stream().anyMatch(l -> l.rotulo() == 1));
        assertTrue(treino.stream().anyMatch(l -> l.rotulo() == 0));

        assertTrue(validacao.stream().anyMatch(l -> l.rotulo() == 1));
        assertTrue(validacao.stream().anyMatch(l -> l.rotulo() == 0));
    }

    @Test
    void demonstracaoTemTresJanelasEHaDoisClientesSemCompras() {
        var periodo = PeriodoRecomendacao.PADRAO;

        String[] limites = {
            "1900-01-01",
            periodo.corte().toString(),
            periodo.observacaoExclusiva().toString(),
            periodo.validacaoExclusiva().toString()
        };

        for (long cliente : List.of(1L, 3L)) {
            for (int janela = 0; janela < 3; janela++) {
                assertTrue(jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM pedido
                    WHERE cliente_id = ?
                      AND julianday(data_pedido) >= julianday(?)
                      AND julianday(data_pedido) < julianday(?)
                    """,
                    Integer.class,
                    cliente,
                    limites[janela],
                    limites[janela + 1]
                ) > 0);

                assertTrue(jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM feedback_resposta f
                    JOIN mensagem_chat m
                        ON m.id = f.mensagem_chat_id
                    JOIN conversa c
                        ON c.id = m.conversa_id

                    WHERE c.cliente_id = ?
                      AND julianday(f.criada_em) >= julianday(?)
                      AND julianday(f.criada_em) < julianday(?)
                      AND EXISTS (
                          SELECT 1
                          FROM recomendacao_mensagem r
                          WHERE r.mensagem_chat_id = m.id
                      )
                    """,
                    Integer.class,
                    cliente,
                    limites[janela],
                    limites[janela + 1]
                ) > 0);
            }
        }

        var semCompras = jdbc.queryForList("""
            SELECT c.id
            FROM cliente c
            WHERE perfil = 'CLIENTE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM pedido p
                  WHERE p.cliente_id = c.id
              )
            ORDER BY c.id
            """,
            Long.class
        );

        assertEquals(List.of(1017L, 1018L), semCompras);

        assertEquals(
            0,
            jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM feedback_resposta f
                JOIN mensagem_chat m
                    ON m.id = f.mensagem_chat_id
                JOIN conversa c
                    ON c.id = m.conversa_id

                WHERE c.cliente_id IN (1017, 1018)
                  AND f.util = 1
                """,
                Integer.class
            )
        );
    }
}