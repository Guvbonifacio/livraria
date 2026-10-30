package com.fatec.livraria.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReservaCarrinhoRepository {

    public record Reserva(
        Long id,
        Long livroId,
        int quantidade,
        String titulo,
        String sessaoId,
        Long clienteId,
        Instant expiraEm,
        boolean avisado,
        boolean expirado
    ) {}

    private final JdbcTemplate jdbc;

    public ReservaCarrinhoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Reserva> listar(String sessao) {
        return jdbc.query("""
            SELECT r.*, l.titulo
            FROM reserva_estoque r
            JOIN livro l ON l.id = r.livro_id
            WHERE r.sessao_id = ?
            ORDER BY r.id
            """,
            (rs, linha) -> new Reserva(
                rs.getLong("id"),
                rs.getLong("livro_id"),
                rs.getInt("quantidade"),
                rs.getString("titulo"),
                rs.getString("sessao_id"),
                rs.getObject("cliente_id") == null
                    ? null
                    : rs.getLong("cliente_id"),
                Instant.parse(rs.getString("expira_em")),
                rs.getInt("aviso_enviado") == 1,
                rs.getInt("expirado") == 1
            ),
            sessao
        );
    }

    public List<String> listarSessoes() {
        return jdbc.queryForList("""
            SELECT DISTINCT sessao_id
            FROM reserva_estoque
            WHERE expirado = 0
              AND cliente_id IS NOT NULL
              AND sessao_id NOT LIKE 'pedido:%'
            """,
            String.class
        );
    }

    public Long salvar(
            String sessao,
            Long cliente,
            Long livro,
            int quantidade,
            Instant prazo) {

        jdbc.update("""
            INSERT INTO reserva_estoque (
                sessao_id, cliente_id, livro_id,
                quantidade, expira_em,
                aviso_enviado, expirado
            )
            VALUES (?, ?, ?, ?, ?, 0, 0)
            """,
            sessao,
            cliente,
            livro,
            quantidade,
            prazo.toString()
        );

        return jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );
    }

    public int atualizar(
            Long id,
            int quantidade,
            Instant prazo,
            boolean avisado,
            boolean expirado) {

        return jdbc.update("""
            UPDATE reserva_estoque
            SET quantidade = ?,
                expira_em = ?,
                aviso_enviado = ?,
                expirado = ?
            WHERE id = ?
            """,
            quantidade,
            prazo.toString(),
            avisado ? 1 : 0,
            expirado ? 1 : 0,
            id
        );
    }

    public int excluir(Long id) {
        return jdbc.update(
            "DELETE FROM reserva_estoque WHERE id = ?",
            id
        );
    }

    public int vincular(
            Long id,
            String sessao,
            String destino,
            Instant agora) {

        return jdbc.update("""
            UPDATE reserva_estoque
            SET sessao_id = ?
            WHERE id = ?
              AND sessao_id = ?
              AND expirado = 0
              AND julianday(expira_em) > julianday(?)
            """,
            destino,
            id,
            sessao,
            agora.toString()
        );
    }
}