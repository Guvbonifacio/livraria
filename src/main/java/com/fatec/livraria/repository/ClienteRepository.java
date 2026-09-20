package com.fatec.livraria.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.livraria.model.CartaoCredito;
import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.Endereco;

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
                cliente.getSenha());   // o Service passará o hash aqui 

        return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    public Cliente buscarPorId(Long id) {
        Cliente cliente = jdbc.queryForObject(
                "SELECT * FROM cliente WHERE id = ?",
                this::mapearCliente,
                id);

        cliente.setEnderecos(buscarEnderecos(id));
        cliente.setCartoes(buscarCartoes(id));
        return cliente;
    }

    public List<Cliente> listarTodos() {
        return jdbc.query("SELECT * FROM cliente ORDER BY nome", this::mapearCliente);
    }

    // ----------------------------------------------------------------- ENDERECO

    public void salvarEndereco(Long clienteId, Endereco e) {
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
    }

    public List<Endereco> buscarEnderecos(Long clienteId) {
        return jdbc.query(
                "SELECT * FROM endereco WHERE cliente_id = ? ORDER BY id",
                this::mapearEndereco,
                clienteId);
    }

    public void salvarCartao(Long clienteId, CartaoCredito c) {
        // O código de segurança (CVV) NUNCA é gravado é proibido 
        jdbc.update("""
                INSERT INTO cartao_credito
                (cliente_id, numero_mascarado, nome_impresso, bandeira, preferencial)
                VALUES (?, ?, ?, ?, ?)
                """,
                clienteId,
                c.getNumero(),
                c.getNomeImpresso(),
                c.getBandeira(),
                c.isPreferencial() ? 1 : 0);   // SQLite não tem booleano: 1 = true, 0 = false
    }

    public List<CartaoCredito> buscarCartoes(Long clienteId) {
        return jdbc.query(
                "SELECT * FROM cartao_credito WHERE cliente_id = ? ORDER BY id",
                this::mapearCartao,
                clienteId);
    }

    // ----------------------------------------------------------------- MAPPERS
    // Traduzem uma linha do banco (ResultSet) em um objeto Java.
    // Um por tabela; a assinatura (ResultSet, int) é a que o JdbcTemplate espera.

    private Cliente mapearCliente(ResultSet rs, int linha) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getLong("id"));
        c.setNome(rs.getString("nome"));
        c.setGenero(rs.getString("genero"));
        c.setDataNascimento(rs.getString("data_nascimento"));
        c.setCpf(rs.getString("cpf"));
        c.setTelefone(rs.getString("telefone"));
        c.setEmail(rs.getString("email"));
        c.setSenha(rs.getString("senha_hash"));      // carrega o HASH; o UPDATE de alteração não pode regravar isso
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