package com.fatec.livraria.repository;

import com.fatec.livraria.model.LogTransacao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class LogRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<LogTransacao> rowMapper = (rs, rowNum) ->
        new LogTransacao(
            rs.getLong("id"),
            LocalDateTime.parse(rs.getString("data_hora")),
            rs.getString("usuario"),
            rs.getString("operacao"),
            rs.getString("tabela"),
            rs.getLong("registro_id"),
            rs.getString("dados")
        );

    public LogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void registrar(LogTransacao log) {
        String sql = """
            INSERT INTO log_transacao
                (data_hora, usuario, operacao, tabela, registro_id, dados)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(
            sql,
            log.getDataHora().toString(),
            log.getUsuario(),
            log.getOperacao(),
            log.getTabela(),
            log.getRegistroId(),
            log.getDados()
        );
    }

    public List<LogTransacao> buscarPorRegistro(String tabela, Long registroId) {
        String sql = """
            SELECT id, data_hora, usuario, operacao, tabela, registro_id, dados
            FROM log_transacao
            WHERE tabela = ? AND registro_id = ?
            ORDER BY data_hora DESC, id DESC
            """;

        return jdbcTemplate.query(sql, rowMapper, tabela, registroId);
    }
}