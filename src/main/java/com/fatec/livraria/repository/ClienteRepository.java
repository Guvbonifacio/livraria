package com.fatec.livraria.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.livraria.model.Cliente;

@Repository
public class ClienteRepository {

    private final JdbcTemplate jdbc;

    public ClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Long salvar(Cliente cliente) {

        String sql = "INSERT INTO cliente "
                + "(nome, genero, data_nascimento, cpf, telefone, email, senha_hash, ativo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 1)";

        jdbc.update(
                sql,
                cliente.getNome(),
                cliente.getGenero(),
                cliente.getDataNascimento(),
                cliente.getCpf(),
                cliente.getTelefone(),
                cliente.getEmail(),
                cliente.getSenha()
        );

        return jdbc.queryForObject(
                "SELECT last_insert_rowid()",
                Long.class
        );
    }

    public Cliente buscarPorId(Long id) {

        String sql = "SELECT * FROM cliente WHERE id = ?";

        return jdbc.queryForObject(  //devolve apenas um resultado
                sql,
                this::mapearCliente,
                id);
    }

    private Cliente mapearCliente(ResultSet rs, int linha) throws SQLException { // método que traduz o que o banco guarda para objeto em java

        Cliente c = new Cliente();

        c.setId(rs.getLong("id"));
        c.setNome(rs.getString("nome"));
        c.setGenero(rs.getString("genero"));
        c.setDataNascimento(rs.getString("data_nascimento"));
        c.setCpf(rs.getString("cpf"));
        c.setTelefone(rs.getString("telefone"));
        c.setEmail(rs.getString("email"));
        c.setSenha(rs.getString("senha_hash"));
        c.setAtivo(rs.getInt("ativo") == 1);

        return c;
    }

    public List<Cliente> listarTodos() {
    return jdbc.query(                      //devolve uma lista
        "SELECT * FROM cliente ORDER BY nome",  //retorna em ordem alfavetica
        this::mapearCliente);
    }
}