package com.fatec.livraria.repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnaliseVendasRepository {

    public record TotalMensal(
        YearMonth mes,
        String categoria,
        long totalCentavos
    ) {}

    static final String CONSULTA = """
        SELECT
            strftime('%Y-%m', p.data_pedido) AS mes,
            c.nome AS categoria,
            SUM(ip.quantidade * ip.preco_unitario_centavos) AS total
        FROM pedido p
        JOIN item_pedido ip
            ON ip.pedido_id = p.id
        JOIN livro_categoria lc
            ON lc.livro_id = ip.livro_id
        JOIN categoria c
            ON c.id = lc.categoria_id
        WHERE p.status IN (
            'APROVADA',
            'EM TRANSPORTE',
            'ENTREGUE'
        )
          AND p.data_pedido >= ?
          AND p.data_pedido < ?
        GROUP BY mes, c.nome
        ORDER BY mes, c.nome
        """;

    private final JdbcTemplate jdbc;

    public AnaliseVendasRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TotalMensal> consultar(
            LocalDate dataInicial,
            LocalDate limiteFinalExclusivo) {

        return jdbc.query(
            CONSULTA,
            (rs, linha) -> new TotalMensal(
                YearMonth.parse(rs.getString("mes")),
                rs.getString("categoria"),
                rs.getLong("total")
            ),
            dataInicial.toString(),
            limiteFinalExclusivo.toString()
        );
    }
}