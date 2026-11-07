package com.fatec.livraria.repository;

import java.time.LocalDate;
import java.time.YearMonth;
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
class AnaliseVendasRepositoryTest {

    @Autowired
    AnaliseVendasRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    private Long livro(String isbn) {
        return jdbc.queryForObject(
            "SELECT id FROM livro WHERE isbn = ?",
            Long.class,
            isbn
        );
    }

    private Long pedido(String data, String status) {
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
            VALUES (1, ?, ?, '{}', 0, 0, 0, 0)
            """,
            data,
            status
        );

        return jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );
    }

    private void item(
            Long pedido,
            Long livro,
            int quantidade,
            long precoCentavos) {

        jdbc.update("""
            INSERT INTO item_pedido (
                pedido_id,
                livro_id,
                titulo,
                quantidade,
                preco_unitario_centavos
            )
            SELECT ?, id, titulo, ?, ?
            FROM livro
            WHERE id = ?
            """,
            pedido,
            quantidade,
            precoCentavos,
            livro
        );

        long valor = quantidade * precoCentavos;

        jdbc.update("""
            UPDATE pedido
            SET subtotal_centavos = subtotal_centavos + ?,
                total_centavos = total_centavos + ?
            WHERE id = ?
            """,
            valor,
            valor,
            pedido
        );
    }

    @Test
    void incluiUltimoInstanteDoDiaFinalEExcluiDiaSeguinte() {
        Long hobbit = livro("9788595084742");

        item(
            pedido("2026-02-28T23:59:59", "APROVADA"),
            hobbit, 1, 90000
        );

        item(
            pedido("2026-03-01T00:00:00", "APROVADA"),
            hobbit, 1, 3000
        );

        item(
            pedido("2026-03-31T23:59:59.999999999", "ENTREGUE"),
            hobbit, 2, 2000
        );

        item(
            pedido("2026-04-01T00:00:00", "APROVADA"),
            hobbit, 1, 80000
        );

        var resultado = repository.consultar(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 4, 1)
        );

        assertEquals(
            List.of(
                new AnaliseVendasRepository.TotalMensal(
                    YearMonth.of(2026, 3),
                    "Fantasia",
                    7000L
                )
            ),
            resultado
        );
    }

    @Test
    void consideraSomenteOsTresStatusPermitidos() {
        Long hobbit = livro("9788595084742");

        for (String status : List.of(
                "APROVADA",
                "EM TRANSPORTE",
                "ENTREGUE")) {

            item(
                pedido("2026-03-15T12:00:00", status),
                hobbit,
                2,
                1001
            );
        }

        for (String status : List.of(
                "EM PROCESSAMENTO",
                "REPROVADA")) {

            item(
                pedido("2026-03-15T12:00:00", status),
                hobbit,
                10,
                99999
            );
        }

        var resultado = repository.consultar(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 4, 1)
        );

        assertEquals(1, resultado.size());
        assertEquals(6006L, resultado.get(0).totalCentavos());
    }

    @Test
    void dunaEntraIntegralNasDuasCategoriasComPrecoHistorico() {
        item(
            pedido("2026-03-15T12:00:00", "APROVADA"),
            livro("9788576573135"),
            3,
            1999
        );

        var resultado = repository.consultar(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 4, 1)
        );

        assertEquals(2, resultado.size());

        assertEquals(
            List.of("Fantasia", "Ficção Científica"),
            resultado.stream()
                .map(r -> r.categoria())
                .toList()
        );

        assertTrue(
            resultado.stream()
                .allMatch(r -> r.totalCentavos() == 5997L)
        );
    }

    @Test
    void pedidoComDuasCategoriasDistribuiValoresDosItens() {
        Long pedido = pedido(
            "2026-03-15T12:00:00",
            "APROVADA"
        );

        item(
            pedido,
            livro("9788595084742"),
            1,
            3210
        );

        item(
            pedido,
            livro("9788535914849"),
            2,
            4789
        );

        var resultado = repository.consultar(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 4, 1)
        );

        assertEquals(
            List.of(
                new AnaliseVendasRepository.TotalMensal(
                    YearMonth.of(2026, 3),
                    "Fantasia",
                    3210L
                ),
                new AnaliseVendasRepository.TotalMensal(
                    YearMonth.of(2026, 3),
                    "Literatura Clássica",
                    9578L
                )
            ),
            resultado
        );
    }

    @Test
    void agrupaPorAnoEMesEMantemMesesVaziosParaOServicePreencher() {
        Long hobbit = livro("9788595084742");

        item(
            pedido("2025-03-15T12:00:00", "APROVADA"),
            hobbit, 1, 1000
        );

        item(
            pedido("2026-03-15T12:00:00", "APROVADA"),
            hobbit, 1, 2000
        );

        var resultado = repository.consultar(
            LocalDate.of(2025, 3, 1),
            LocalDate.of(2026, 4, 1)
        );

        assertEquals(
            List.of(
                YearMonth.of(2025, 3),
                YearMonth.of(2026, 3)
            ),
            resultado.stream()
                .map(r -> r.mes())
                .toList()
        );
    }

    @Test
    void intervaloSemVendasRetornaListaVazia() {
        assertTrue(
            repository.consultar(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1)
            ).isEmpty()
        );
    }

    @Test
    void indiceContemStatusEDataNessaOrdem() {
        var colunas = jdbc.query(
            "PRAGMA index_info(ix_pedido_status_data)",
            (rs, linha) -> rs.getString("name")
        );

        assertEquals(
            List.of("status", "data_pedido"),
            colunas
        );
    }
}