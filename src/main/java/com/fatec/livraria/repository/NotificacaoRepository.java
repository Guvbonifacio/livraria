package com.fatec.livraria.repository;

import com.fatec.livraria.model.Notificacao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacaoRepository {

    private final JdbcTemplate jdbc;

    public NotificacaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long salvar(
            Long clienteId,
            String mensagem,
            LocalDateTime dataHora) {

        jdbc.update(
            """
            INSERT INTO notificacao (
                cliente_id,
                mensagem,
                data_hora,
                lida
            )
            VALUES (?, ?, ?, 0)
            """,
            clienteId,
            mensagem,
            dataHora.toString()
        );

        return jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );
    }

    public List<Notificacao> listarDoCliente(Long clienteId) {
        return jdbc.query(
            """
            SELECT id, cliente_id, mensagem, data_hora, lida
            FROM notificacao
            WHERE cliente_id = ?
            ORDER BY data_hora DESC, id DESC
            """,
            (rs, linha) -> new Notificacao(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getString("mensagem"),
                LocalDateTime.parse(rs.getString("data_hora")),
                rs.getInt("lida") == 1
            ),
            clienteId
        );
    }
}