package com.fatec.livraria.repository;

import com.fatec.livraria.model.Livro;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LivroRepository {

    private final JdbcTemplate jdbc;

    private static final String CONSULTA = """
        SELECT
            l.*,
            COALESCE(e.disponibilidade, 0) AS disponibilidade,
            (
                SELECT group_concat(c.nome, ', ')
                FROM livro_categoria lc
                JOIN categoria c ON c.id = lc.categoria_id
                WHERE lc.livro_id = l.id
            ) AS categorias
        FROM livro l
        LEFT JOIN (
            SELECT livro_id, SUM(quantidade) AS disponibilidade
            FROM item_estoque
            GROUP BY livro_id
        ) e ON e.livro_id = l.id
        WHERE l.ativo = 1
        """;

    public LivroRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Livro> consultar(String busca, String categoria) {
        StringBuilder sql = new StringBuilder(CONSULTA);
        List<Object> parametros = new ArrayList<>();

        if (busca != null && !busca.isBlank()) {
            sql.append("""
                 AND (
                    l.titulo LIKE ?
                    OR l.autor LIKE ?
                    OR l.isbn LIKE ?
                 )
                """);

            String termo = "%" + busca.trim() + "%";

            parametros.add(termo);
            parametros.add(termo);
            parametros.add(termo);
        }

        if (categoria != null && !categoria.isBlank()) {
            sql.append("""
                 AND EXISTS (
                    SELECT 1
                    FROM livro_categoria lc
                    JOIN categoria c ON c.id = lc.categoria_id
                    WHERE lc.livro_id = l.id
                      AND c.nome = ?
                 )
                """);

            parametros.add(categoria);
        }

        sql.append(" ORDER BY l.titulo COLLATE NOCASE, l.id");

        return jdbc.query(
            sql.toString(),
            this::mapear,
            parametros.toArray()
        );
    }

    public Livro buscarPorId(Long id) {
        List<Livro> encontrados = jdbc.query(
            CONSULTA + " AND l.id = ?",
            this::mapear,
            id
        );

        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    public long buscarDisponibilidade(Long livroId) {
        Long quantidade = jdbc.queryForObject(
            """
            SELECT COALESCE(SUM(e.quantidade), 0)
            FROM item_estoque e
            JOIN livro l ON l.id = e.livro_id
            WHERE l.id = ? AND l.ativo = 1
            """,
            Long.class,
            livroId
        );

        return quantidade == null ? 0 : quantidade;
    }

        public List<String> listarCategorias() {
         return jdbc.queryForList(
        "SELECT nome FROM categoria ORDER BY nome",
        String.class
    );
    }

    private Livro mapear(ResultSet rs, int linha)
            throws SQLException {
        String dimensoes =
            rs.getInt("altura_mm") + " x "
            + rs.getInt("largura_mm") + " x "
            + rs.getInt("profundidade_mm") + " mm - "
            + rs.getInt("peso_g") + " g";

        return new Livro(
            rs.getLong("id"),
            rs.getString("titulo"),
            rs.getString("autor"),
            rs.getString("categorias"),
            rs.getInt("ano"),
            rs.getString("editora"),
            rs.getString("edicao"),
            rs.getString("isbn"),
            rs.getInt("paginas"),
            rs.getString("sinopse"),
            dimensoes,
            rs.getLong("preco_centavos") / 100.0,
            Math.toIntExact(rs.getLong("disponibilidade")),
            rs.getString("imagem_url")
        );
    }
}