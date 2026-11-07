package com.fatec.livraria.repository;

import java.time.LocalDate;
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
@Sql("/cenario-checkout.sql")
class AnaliseVendasPlanoTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void rnf0011UsaIndiceDeStatusEDataNaConsultaReal() {
        Long livro = jdbc.queryForObject(
            "SELECT id FROM livro WHERE isbn = '9788595084742'",
            Long.class
        );

        List<String> status = List.of(
            "EM PROCESSAMENTO",
            "APROVADA",
            "EM TRANSPORTE",
            "ENTREGUE",
            "REPROVADA"
        );

        for (int i = 0; i < 278; i++) {
            String data = LocalDate.of(2025, 9, 1)
                .plusMonths(i % 14)
                .plusDays(i % 20)
                .atTime(12, 0)
                .toString();

            jdbc.update("""
                INSERT INTO pedido (
                    cliente_id,
                    data_pedido,
                    status,
                    endereco_entrega_json,
                    subtotal_centavos,
                    frete_centavos,
                    desconto_centavos,
                    total_centavos
                )
                VALUES (1, ?, ?, '{}', 3000, 0, 0, 3000)
                """,
                data,
                status.get(i % status.size())
            );

            Long pedido = jdbc.queryForObject(
                "SELECT last_insert_rowid()",
                Long.class
            );

            jdbc.update("""
                INSERT INTO item_pedido (
                    pedido_id,
                    livro_id,
                    titulo,
                    quantidade,
                    preco_unitario_centavos
                )
                VALUES (?, ?, 'O Hobbit', 1, 3000)
                """,
                pedido,
                livro
            );
        }

        List<String> plano = jdbc.query(
            "EXPLAIN QUERY PLAN "
                + AnaliseVendasRepository.CONSULTA,
            (rs, linha) -> rs.getString("detail"),
            "2025-09-01",
            "2026-11-01"
        );

        System.out.println(
            "Plano da consulta de análise:\n"
            + String.join("\n", plano)
        );

        assertTrue(
            plano.stream().anyMatch(linha ->
                linha.contains("SEARCH p")
                && linha.contains("ix_pedido_status_data")
            ),
            () -> "O plano não usou a busca pelo índice esperado:\n"
                + String.join("\n", plano)
        );
    }
}