package com.fatec.livraria.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BandeiraRepository {

    private final JdbcTemplate jdbc;

    public BandeiraRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> listar() {
        return jdbc.queryForList(
            "SELECT nome FROM bandeira ORDER BY nome",
            String.class
        );
    }

    public boolean existePorNome(String nome) {
        Integer quantidade = jdbc.queryForObject(
            "SELECT COUNT(*) FROM bandeira WHERE nome = ?",
            Integer.class,
            nome
        );

        return quantidade != null && quantidade > 0;
    }
}