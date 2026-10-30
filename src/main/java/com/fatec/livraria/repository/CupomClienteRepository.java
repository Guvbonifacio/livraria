package com.fatec.livraria.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CupomClienteRepository {

    public record Registro(
        String codigo,
        String tipo,
        BigDecimal valor,
        LocalDate validade,
        boolean ativo
    ) {}

    private final JdbcTemplate jdbc;

    public CupomClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Registro> listar(Long clienteId) {
        return jdbc.query("""
            SELECT
                codigo,
                tipo,
                valor_centavos,
                validade,
                ativo
            FROM cupom
            WHERE cliente_id = ?
               OR (
                   tipo = 'PROMOCIONAL'
                   AND cliente_id IS NULL
               )
            ORDER BY validade DESC, id DESC
            """,
            (rs, linha) -> new Registro(
                rs.getString("codigo"),
                rs.getString("tipo"),
                BigDecimal.valueOf(
                    rs.getLong("valor_centavos"),
                    2
                ),
                LocalDate.parse(rs.getString("validade")),
                rs.getInt("ativo") == 1
            ),
            clienteId
        );
    }
}