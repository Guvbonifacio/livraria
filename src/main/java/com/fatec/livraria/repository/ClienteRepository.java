package com.fatec.livraria.repository;

import com.fatec.livraria.model.CartaoCredito;
import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.Endereco;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ClienteRepository {

    private final JdbcTemplate jdbc;

    public ClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Long salvar(Cliente cliente) {
        jdbc.update("""
                INSERT INTO cliente
                    (nome, genero, data_nascimento, cpf, telefone, email, senha_hash, ativo)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """,
                cliente.getNome(),
                cliente.getGenero(),
                cliente.getDataNascimento(),
                cliente.getCpf(),
                cliente.getTelefone(),
                cliente.getEmail(),
                cliente.getSenha());

        return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    public boolean existePorCpf(String cpf) {
        Integer quantidade = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cliente WHERE cpf = ?",
            Integer.class,
            cpf
        );
        return quantidade != null && quantidade > 0;
    }

    public boolean existePorCpf(String cpf, Long idIgnorado) {
        Integer quantidade = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cliente WHERE cpf = ? AND id <> ?",
            Integer.class,
            cpf,
            idIgnorado
        );
        return quantidade != null && quantidade > 0;
    }

    public boolean existePorEmail(String email) {
        Integer quantidade = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cliente WHERE email = ?",
            Integer.class,
            email
        );
        return quantidade != null && quantidade > 0;
    }

    public boolean existePorEmail(String email, Long idIgnorado) {
        Integer quantidade = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cliente WHERE email = ? AND id <> ?",
            Integer.class,
            email,
            idIgnorado
        );
        return quantidade != null && quantidade > 0;
    }

    public Cliente buscarPorId(Long id) {
        Cliente cliente = jdbc.queryForObject(
            "SELECT * FROM cliente WHERE id = ?",
            this::mapearCliente,
            id
        );

        if (cliente != null) {
            cliente.setEnderecos(buscarEnderecos(id));
            cliente.setCartoes(buscarCartoes(id));
        }

        return cliente;
    }

    public List<Cliente> listarTodos() {
        return jdbc.query("SELECT * FROM cliente ORDER BY nome", this::mapearCliente);
    }

    @Transactional
    public Long salvarEndereco(Long clienteId, Endereco e) {
        jdbc.update("""
                INSERT INTO endereco
                    (cliente_id, nome_curto, tipo, tipo_residencia, logradouro, numero, bairro, cep, cidade, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                clienteId,
                e.getNomeCurto(),
                e.getTipo(),
                e.getTipoResidencia(),
                e.getLogradouro(),
                e.getNumero(),
                e.getBairro(),
                e.getCep(),
                e.getCidade(),
                e.getEstado());

        return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    public List<Endereco> buscarEnderecos(Long clienteId) {
        return jdbc.query(
            "SELECT * FROM endereco WHERE cliente_id = ? ORDER BY id",
            this::mapearEndereco,
            clienteId
        );
    }

    @Transactional
    public Long salvarCartao(Long clienteId, CartaoCredito c) {
        jdbc.update("""
                INSERT INTO cartao_credito
                    (cliente_id, numero_mascarado, nome_impresso, bandeira, preferencial)
                VALUES (?, ?, ?, ?, ?)
                """,
                clienteId,
                c.getNumero(),
                c.getNomeImpresso(),
                c.getBandeira(),
                c.isPreferencial() ? 1 : 0);

        return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    public List<CartaoCredito> buscarCartoes(Long clienteId) {
        return jdbc.query(
            "SELECT * FROM cartao_credito WHERE cliente_id = ? ORDER BY id",
            this::mapearCartao,
            clienteId
        );
    }

    public List<Cliente> consultar(String nome, String cpf, String email) {
        StringBuilder sql = new StringBuilder("SELECT * FROM cliente WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (nome != null && !nome.isBlank()) {
            sql.append(" AND nome LIKE ?");
            params.add("%" + nome + "%");
        }

        if (cpf != null && !cpf.isBlank()) {
            sql.append(" AND cpf = ?");
            params.add(cpf.replaceAll("\\D", ""));
        }

        if (email != null && !email.isBlank()) {
            sql.append(" AND email LIKE ?");
            params.add("%" + email.toLowerCase() + "%");
        }

        sql.append(" ORDER BY nome");

        return jdbc.query(sql.toString(), this::mapearCliente, params.toArray());
    }

    public void atualizar(Cliente c) {
        jdbc.update("""
                UPDATE cliente
                SET nome = ?, genero = ?, data_nascimento = ?, cpf = ?, telefone = ?, email = ?
                WHERE id = ?
                """,
                c.getNome(),
                c.getGenero(),
                c.getDataNascimento(),
                c.getCpf(),
                c.getTelefone(),
                c.getEmail(),
                c.getId());
    }

    public void inativar(Long id) {
        jdbc.update("UPDATE cliente SET ativo = 0 WHERE id = ?", id);
    }

    public void atualizarSenha(Long id, String senhaHash) {
        jdbc.update("UPDATE cliente SET senha_hash = ? WHERE id = ?", senhaHash, id);
    }

    public void atualizarEndereco(Long clienteId, Endereco e) {
        jdbc.update("""
                UPDATE endereco
                SET nome_curto = ?, tipo = ?, tipo_residencia = ?, logradouro = ?,
                    numero = ?, bairro = ?, cep = ?, cidade = ?, estado = ?
                WHERE id = ? AND cliente_id = ?
                """,
                e.getNomeCurto(),
                e.getTipo(),
                e.getTipoResidencia(),
                e.getLogradouro(),
                e.getNumero(),
                e.getBairro(),
                e.getCep(),
                e.getCidade(),
                e.getEstado(),
                e.getId(),
                clienteId);
    }

    public void excluirEndereco(Long clienteId, Long enderecoId) {
        jdbc.update("DELETE FROM endereco WHERE id = ? AND cliente_id = ?", enderecoId, clienteId);
    }

    public Endereco buscarEnderecoPorId(Long clienteId, Long enderecoId) {
        List<Endereco> encontrados = jdbc.query(
            "SELECT * FROM endereco WHERE id = ? AND cliente_id = ?",
            this::mapearEndereco,
            enderecoId,
            clienteId
        );
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    public void atualizarCartao(Long clienteId, CartaoCredito c) {
        jdbc.update("""
                UPDATE cartao_credito
                SET numero_mascarado = ?, nome_impresso = ?, bandeira = ?
                WHERE id = ? AND cliente_id = ?
                """,
                c.getNumero(),
                c.getNomeImpresso(),
                c.getBandeira(),
                c.getId(),
                clienteId);
    }

    public void excluirCartao(Long clienteId, Long cartaoId) {
        jdbc.update("DELETE FROM cartao_credito WHERE id = ? AND cliente_id = ?", cartaoId, clienteId);
    }

    @Transactional
    public void definirPreferencial(Long clienteId, Long cartaoId) {
        Integer quantidade = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cartao_credito WHERE id = ? AND cliente_id = ?",
            Integer.class,
            cartaoId,
            clienteId
        );

        if (quantidade == null || quantidade != 1) {
            throw new IllegalArgumentException("Cartão não pertence ao cliente.");
        }

        jdbc.update("UPDATE cartao_credito SET preferencial = 0 WHERE cliente_id = ?", clienteId);

        int alterados = jdbc.update("""
                UPDATE cartao_credito
                SET preferencial = 1
                WHERE id = ? AND cliente_id = ?
                """,
                cartaoId,
                clienteId);

        if (alterados != 1) {
            throw new IllegalStateException("Não foi possível definir o cartão preferencial.");
        }
    }

    private Cliente mapearCliente(ResultSet rs, int linha) throws SQLException {
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

    private Endereco mapearEndereco(ResultSet rs, int linha) throws SQLException {
        Endereco e = new Endereco();
        e.setId(rs.getLong("id"));
        e.setNomeCurto(rs.getString("nome_curto"));
        e.setTipo(rs.getString("tipo"));
        e.setTipoResidencia(rs.getString("tipo_residencia"));
        e.setLogradouro(rs.getString("logradouro"));
        e.setNumero(rs.getString("numero"));
        e.setBairro(rs.getString("bairro"));
        e.setCep(rs.getString("cep"));
        e.setCidade(rs.getString("cidade"));
        e.getEstado(); // mantendo compatibilidade
        e.setEstado(rs.getString("estado"));
        return e;
    }

    private CartaoCredito mapearCartao(ResultSet rs, int linha) throws SQLException {
        CartaoCredito c = new CartaoCredito();
        c.setId(rs.getLong("id"));
        c.setNumero(rs.getString("numero_mascarado"));
        c.setNomeImpresso(rs.getString("nome_impresso"));
        c.setBandeira(rs.getString("bandeira"));
        c.setPreferencial(rs.getInt("preferencial") == 1);
        return c;
    }
}