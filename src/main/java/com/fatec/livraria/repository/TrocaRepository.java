package com.fatec.livraria.repository;

import com.fatec.livraria.model.DadosAdministrativos.*;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TrocaRepository {

    private final JdbcTemplate jdbc;
    private final PedidoRepository pedidos;

    public TrocaRepository(
            JdbcTemplate jdbc,
            PedidoRepository pedidos) {

        this.jdbc = jdbc;
        this.pedidos = pedidos;
    }

    public List<Troca> listar() {
        return jdbc.query("""
            SELECT *
            FROM troca
            ORDER BY data_solicitacao DESC, id DESC
            """,
            this::mapear
        );
    }

    public Troca buscarPorId(Long id) {
        var encontrados = jdbc.query(
            "SELECT * FROM troca WHERE id = ?",
            this::mapear,
            id
        );

        return encontrados.isEmpty()
            ? null
            : encontrados.get(0);
    }

    private Troca mapear(
            ResultSet rs,
            int linha) throws SQLException {

        Long id = rs.getLong("id");

        var itens = jdbc.query("""
            SELECT
                it.id,
                ip.livro_id,
                ip.titulo,
                ip.preco_unitario_centavos,
                it.quantidade
            FROM item_troca it
            JOIN item_pedido ip ON ip.id = it.item_pedido_id
            WHERE it.troca_id = ?
            ORDER BY it.id
            """,
            (r, n) -> new Item(
                r.getLong("id"),
                new Livro(
                    r.getLong("livro_id"),
                    r.getString("titulo"),
                    BigDecimal.valueOf(
                        r.getLong("preco_unitario_centavos"), 2
                    )
                ),
                r.getInt("quantidade")
            ),
            id
        );

        return new Troca(
            id,
            pedidos.buscarAdministrativo(
                rs.getLong("pedido_id")
            ),
            rs.getString("status"),
            rs.getString("motivo"),
            data(rs.getString("data_solicitacao")),
            data(rs.getString("data_autorizacao")),
            data(rs.getString("data_recebimento")),

            rs.getObject("retorna_estoque") == null
                ? null
                : rs.getInt("retorna_estoque") == 1,

            rs.getObject("cupom_id") == null
                ? null
                : rs.getLong("cupom_id"),

            itens
        );
    }

    private LocalDateTime data(String valor) {
        return valor == null
            ? null
            : LocalDateTime.parse(valor);
    }

    public List<Troca> listarDoPedido(Long pedidoId) {
    return jdbc.query(
        """
        SELECT *
        FROM troca
        WHERE pedido_id = ?
        ORDER BY id DESC
        """,
        this::mapear,
        pedidoId
    );
}

public List<Long> listarItensSolicitados(Long pedidoId) {
    return jdbc.query(
        """
        SELECT it.item_pedido_id
        FROM item_troca it
        JOIN troca t ON t.id = it.troca_id
        WHERE t.pedido_id = ?
        """,
        (rs, linha) -> rs.getLong("item_pedido_id"),
        pedidoId
    );
}

public Long salvar(Long pedidoId, String motivo) {
    jdbc.update(
        """
        INSERT INTO troca (
            pedido_id,
            status,
            motivo,
            data_solicitacao
        )
        VALUES (?, 'EM TROCA', ?, ?)
        """,
        pedidoId,
        motivo,
        LocalDateTime.now().toString()
    );

    return jdbc.queryForObject(
        "SELECT last_insert_rowid()",
        Long.class
    );
}

public Long salvarItem(
        Long trocaId,
        Long itemPedidoId,
        int quantidade) {

    jdbc.update(
        """
        INSERT INTO item_troca (
            troca_id,
            item_pedido_id,
            quantidade
        )
        VALUES (?, ?, ?)
        """,
        trocaId,
        itemPedidoId,
        quantidade
    );

    return jdbc.queryForObject(
        "SELECT last_insert_rowid()",
        Long.class
    );
}

public record ItemSolicitado(
    Long itemPedidoId,
    int quantidade
) {}

public List<ItemSolicitado> listarItensDaTroca(Long trocaId) {
    return jdbc.query(
        """
        SELECT item_pedido_id, quantidade
        FROM item_troca
        WHERE troca_id = ?
        ORDER BY id
        """,
        (rs, linha) -> new ItemSolicitado(
            rs.getLong("item_pedido_id"),
            rs.getInt("quantidade")
        ),
        trocaId
    );
}

public int autorizar(
        Long id,
        LocalDateTime dataAutorizacao) {

    return jdbc.update(
        """
        UPDATE troca
        SET status = 'TROCA AUTORIZADA',
            data_autorizacao = ?
        WHERE id = ?
          AND status = 'EM TROCA'
        """,
        dataAutorizacao.toString(),
        id
    );
}
}