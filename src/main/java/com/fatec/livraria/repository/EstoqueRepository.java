package com.fatec.livraria.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EstoqueRepository {

    private final JdbcTemplate jdbc;

    public EstoqueRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Lote(
        Long id,
        int quantidade
    ) {}

    public record Reserva(
        Long id,
        Long livroId,
        int quantidade
    ) {}

    // Consulta primeiro os lotes mais antigos.
    public List<Lote> listarLotes(Long livroId) {
        return jdbc.query("""
            SELECT
                id,
                quantidade
            FROM item_estoque
            WHERE livro_id = ?
              AND quantidade > 0
            ORDER BY data_entrada, id
            """,
            (rs, linha) -> new Lote(
                rs.getLong("id"),
                rs.getInt("quantidade")
            ),
            livroId
        );
    }

    // A condição protege contra saldo negativo caso o estoque mude.
    // O Service verificará se exatamente uma linha foi alterada.
    public int baixar(
            Long loteId,
            int quantidade) {

        return jdbc.update("""
            UPDATE item_estoque
            SET quantidade = quantidade - ?
            WHERE id = ?
              AND quantidade >= ?
            """,
            quantidade,
            loteId,
            quantidade
        );
    }

    public List<Reserva> listarReservas(String sessaoId) {
        return jdbc.query("""
            SELECT
                id,
                livro_id,
                quantidade
            FROM reserva_estoque
            WHERE sessao_id = ?
            ORDER BY id
            """,
            (rs, linha) -> new Reserva(
                rs.getLong("id"),
                rs.getLong("livro_id"),
                rs.getInt("quantidade")
            ),
            sessaoId
        );
    }

    // Transfere uma reserva existente para a identificação do pedido.
    // Preserva o prazo de expiração.
    public int vincularReserva(
            Long id,
            String origem,
            String destino) {

        return jdbc.update("""
            UPDATE reserva_estoque
            SET sessao_id = ?
            WHERE id = ?
              AND sessao_id = ?
            """,
            destino,
            id,
            origem
        );
    }

    public int excluirReserva(
            Long id,
            String sessaoId) {

        return jdbc.update("""
            DELETE FROM reserva_estoque
            WHERE id = ?
              AND sessao_id = ?
            """,
            id,
            sessaoId
        );
    }
}