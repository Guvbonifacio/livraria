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

    // Insere um novo cliente no banco de dados e retorna o ID gerado
    @Transactional
    public Long salvar(Cliente cliente) {
        jdbc.update("""
                INSERT INTO cliente (nome, genero, data_nascimento, cpf, telefone, email, senha_hash, ativo)
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

    // Verifica se já existe um cadastro com o CPF informado
    public boolean existePorCpf(String cpf) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE cpf = ?";
        Integer quantidade = jdbc.queryForObject(sql, Integer.class, cpf);
        return quantidade != null && quantidade > 0;
    }

    // Verifica se já existe um cadastro com o CPF informado ignorando o ID atual
    public boolean existePorCpf(String cpf, Long idIgnorado) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE cpf = ? AND id <> ?";
        Integer quantidade = jdbc.queryForObject(sql, Integer.class, cpf, idIgnorado);
        return quantidade != null && quantidade > 0;
    }

    // Verifica se já existe um cadastro com o E-mail informado
    public boolean existePorEmail(String email) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE email = ?";
        Integer quantidade = jdbc.queryForObject(sql, Integer.class, email);
        return quantidade != null && quantidade > 0;
    }

    // Verifica se já existe um cadastro com o E-mail informado ignorando o ID atual
    public boolean existePorEmail(String email, Long idIgnorado) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE email = ? AND id <> ?";
        Integer quantidade = jdbc.queryForObject(sql, Integer.class, email, idIgnorado);
        return quantidade != null && quantidade > 0;
    }

    // Busca cliente por ID carregando também seus endereços e cartões
    public Cliente buscarPorId(Long id) {
        Cliente cliente = jdbc.queryForObject("SELECT * FROM cliente WHERE id = ?", this::mapearCliente, id);
        if (cliente != null) {
            cliente.setEnderecos(buscarEnderecos(id));
            cliente.setCartoes(buscarCartoes(id));
        }
        return cliente;
    }

    // Retorna todos os clientes ordenados pelo nome
    public List<Cliente> listarTodos() {
        return jdbc.query("SELECT * FROM cliente ORDER BY nome", this::mapearCliente);
    }

    // Persiste um novo endereço associado ao cliente
    public void salvarEndereco(Long clienteId, Endereco e) {
        jdbc.update("""
                INSERT INTO endereco (cliente_id, nome_curto, tipo, tipo_residencia, logradouro, numero, bairro, cep, cidade, estado)
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

    // Lista os endereços pertencentes a um cliente
    public List<Endereco> buscarEnderecos(Long clienteId) {
        return jdbc.query("SELECT * FROM endereco WHERE cliente_id = ? ORDER BY id", this::mapearEndereco, clienteId);
    }

    // Persiste um cartão de crédito associado a um cliente
    public void salvarCartao(Long clienteId, CartaoCredito c) {
        jdbc.update("""
                INSERT INTO cartao_credito (cliente_id, numero_mascarado, nome_impresso, bandeira, preferencial)
                VALUES (?, ?, ?, ?, ?)
                """,
                clienteId,
                c.getNumero(),
                c.getNomeImpresso(),
                c.getBandeira(),
                c.isPreferencial() ? 1 : 0);
    }

    // Lista os cartões de crédito pertencentes a um cliente
    public List<CartaoCredito> buscarCartoes(Long clienteId) {
        return jdbc.query("SELECT * FROM cartao_credito WHERE cliente_id = ? ORDER BY id", this::mapearCartao, clienteId);
    }

    // Realiza busca dinâmica de clientes por filtros opcionais (nome, CPF e e-mail)
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

    // Atualiza os dados cadastrais do cliente
    public void atualizar(Cliente c) {
        jdbc.update("""
                UPDATE cliente
                SET nome = ?,
                    genero = ?,
                    data_nascimento = ?,
                    cpf = ?,
                    telefone = ?,
                    email = ?
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

    // Altera o status do cliente para inativo
    public void inativar(Long id) {
        String sql = "UPDATE cliente SET ativo = 0 WHERE id = ?";
        jdbc.update(sql, id);
    }

    // Mapeia o resultado do banco para o objeto Cliente
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

    // Mapeia o resultado do banco para o objeto Endereco
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

    // Mapeia o resultado do banco para o objeto CartaoCredito
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