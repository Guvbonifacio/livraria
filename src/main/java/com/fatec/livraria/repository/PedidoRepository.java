package com.fatec.livraria.repository;

import com.fatec.livraria.model.PlanoPagamento.Parcela;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.fatec.livraria.model.DadosAdministrativos.*;
import com.fatec.livraria.model.Endereco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PedidoRepository {

private final JdbcTemplate jdbc;
private final ObjectMapper json;

public PedidoRepository(
        JdbcTemplate jdbc,
        ObjectMapper json) {

    this.jdbc = jdbc;
    this.json = json;
}

    public record CupomDisponivel(
        Long id,
        String codigo,
        String tipo,
        Long clienteId,
        BigDecimal valor,
        LocalDate validade,
        boolean ativo
    ) {}

    public record Confirmacao(
        Long id,
        String status,
        BigDecimal total
    ) {}

    public CupomDisponivel buscarCupom(Long id) {
        var encontrados = jdbc.query(
            "SELECT * FROM cupom WHERE id = ?",
            (rs, numeroLinha) -> new CupomDisponivel(
                rs.getLong("id"),
                rs.getString("codigo"),
                rs.getString("tipo"),
                rs.getObject("cliente_id") == null
                    ? null
                    : rs.getLong("cliente_id"),
                BigDecimal.valueOf(
                    rs.getLong("valor_centavos"), 2
                ),
                LocalDate.parse(rs.getString("validade")),
                rs.getInt("ativo") == 1
            ),
            id
        );

        return encontrados.isEmpty()
            ? null
            : encontrados.get(0);
    }

    public List<Long> listarCupons(Long clienteId) {
        return jdbc.query("""
            SELECT id
            FROM cupom
            WHERE ativo = 1
              AND (
                cliente_id = ?
                OR (
                    cliente_id IS NULL
                    AND tipo = 'PROMOCIONAL'
                )
              )
            ORDER BY tipo, id
            """,
            (rs, numeroLinha) -> rs.getLong("id"),
            clienteId
        );
    }

    public Long salvarPedido(
            Long clienteId,
            String enderecoJson,
            long subtotal,
            long frete) {

        jdbc.update("""
            INSERT INTO pedido (
                cliente_id,
                data_pedido,
                status,
                endereco_entrega_json,
                subtotal_centavos,
                frete_centavos,
                desconto_centavos,
                total_centavos
            )
            VALUES (?, ?, 'EM PROCESSAMENTO', ?, ?, ?, 0, ?)
            """,
            clienteId,
            LocalDateTime.now().toString(),
            enderecoJson,
            subtotal,
            frete,
            Math.addExact(subtotal, frete)
        );

        return ultimoId();
    }

    public Long salvarItem(
            Long pedidoId,
            Long livroId,
            String titulo,
            int quantidade,
            long preco) {

        jdbc.update("""
            INSERT INTO item_pedido (
                pedido_id,
                livro_id,
                titulo,
                quantidade,
                preco_unitario_centavos
            )
            VALUES (?, ?, ?, ?, ?)
            """,
            pedidoId,
            livroId,
            titulo,
            quantidade,
            preco
        );

        return ultimoId();
    }

    public Long salvarPagamento(
            Long pedidoId,
            Parcela parcela) {

        jdbc.update("""
            INSERT INTO pagamento (
                pedido_id,
                forma,
                valor_centavos,
                status,
                cartao_id,
                numero_mascarado,
                bandeira,
                cupom_id
            )
            VALUES (?, ?, ?, 'PENDENTE', ?, ?, ?, ?)
            """,
            pedidoId,
            parcela.forma(),
            centavos(parcela.valor()),
            parcela.cartaoId(),
            parcela.numeroMascarado(),
            parcela.bandeira(),
            parcela.cupomId()
        );

        return ultimoId();
    }

    public int consumirTroca(
            Long id,
            Long clienteId,
            LocalDate hoje) {

        return jdbc.update("""
            UPDATE cupom
            SET ativo = 0
            WHERE id = ?
              AND cliente_id = ?
              AND tipo = 'TROCA'
              AND ativo = 1
              AND validade >= ?
            """,
            id,
            clienteId,
            hoje.toString()
        );
    }

    public Long salvarExcedente(
            String codigo,
            Long clienteId,
            Long pedidoId,
            BigDecimal valor,
            LocalDate validade) {

        jdbc.update("""
            INSERT INTO cupom (
                codigo,
                tipo,
                cliente_id,
                pedido_origem_id,
                valor_centavos,
                validade,
                ativo
            )
            VALUES (?, 'TROCA', ?, ?, ?, ?, 1)
            """,
            codigo,
            clienteId,
            pedidoId,
            centavos(valor),
            validade.toString()
        );

        return ultimoId();
    }

    public Confirmacao buscarConfirmacao(
            Long id,
            Long clienteId) {

        var encontrados = jdbc.query("""
            SELECT id, status, total_centavos
            FROM pedido
            WHERE id = ?
              AND cliente_id = ?
            """,
            (rs, numeroLinha) -> new Confirmacao(
                rs.getLong("id"),
                rs.getString("status"),
                BigDecimal.valueOf(
                    rs.getLong("total_centavos"), 2
                )
            ),
            id,
            clienteId
        );

        return encontrados.isEmpty()
            ? null
            : encontrados.get(0);
    }

    public record PagamentoPendente(
    Long id,
    String forma,
    BigDecimal valor,
    String status,
    Long cupomId
) {}

public record PedidoPendente(
    Long id,
    Long clienteId,
    String status,
    BigDecimal total
) {}

public PedidoPendente buscarParaProcessar(Long id) {
    var encontrados = jdbc.query("""
        SELECT
            id,
            cliente_id,
            status,
            total_centavos
        FROM pedido
        WHERE id = ?
        """,
        (rs, linha) -> new PedidoPendente(
            rs.getLong("id"),
            rs.getLong("cliente_id"),
            rs.getString("status"),
            BigDecimal.valueOf(
                rs.getLong("total_centavos"), 2
            )
        ),
        id
    );

    return encontrados.isEmpty()
        ? null
        : encontrados.get(0);
}

public List<PagamentoPendente> listarPagamentosParaProcessar(
        Long pedidoId) {

    return jdbc.query("""
        SELECT *
        FROM pagamento
        WHERE pedido_id = ?
        ORDER BY id
        """,
        (rs, linha) -> new PagamentoPendente(
            rs.getLong("id"),
            rs.getString("forma"),
            BigDecimal.valueOf(
                rs.getLong("valor_centavos"), 2
            ),
            rs.getString("status"),
            rs.getObject("cupom_id") == null
                ? null
                : rs.getLong("cupom_id")
        ),
        pedidoId
    );
}

public boolean origemCupomAprovada(Long cupomId) {
    Integer quantidade = jdbc.queryForObject("""
        SELECT COUNT(*)
        FROM cupom c
        LEFT JOIN pedido p ON p.id = c.pedido_origem_id
        WHERE c.id = ?
          AND (
              c.pedido_origem_id IS NULL
              OR p.status IN (
                  'APROVADA',
                  'EM TRANSPORTE',
                  'ENTREGUE',
                  'EM TROCA',
                  'TROCA AUTORIZADA',
                  'TROCADO'
              )
          )
        """,
        Integer.class,
        cupomId
    );

    return quantidade != null && quantidade == 1;
}

public boolean trocaComprometidaEmOutroPedido(
        Long cupomId,
        Long pedidoId) {

    Integer quantidade = jdbc.queryForObject("""
        SELECT COUNT(*)
        FROM pagamento
        WHERE cupom_id = ?
          AND pedido_id <> ?
          AND status IN ('PENDENTE', 'APROVADO')
        """,
        Integer.class,
        cupomId,
        pedidoId
    );

    return quantidade != null && quantidade > 0;
}

public BigDecimal totalExcedente(Long pedidoId) {
    Long valor = jdbc.queryForObject("""
        SELECT COALESCE(SUM(valor_centavos), 0)
        FROM cupom
        WHERE pedido_origem_id = ?
          AND tipo = 'TROCA'
        """,
        Long.class,
        pedidoId
    );

    return BigDecimal.valueOf(
        valor == null ? 0 : valor,
        2
    );
}
    public int atualizarStatusPedido(
        Long id,
        String status) {

    return jdbc.update("""
        UPDATE pedido
        SET status = ?
        WHERE id = ?
          AND status = 'EM PROCESSAMENTO'
        """,
        status,
        id
    );
}

public int atualizarStatusPagamento(
        Long id,
        String status) {

    return jdbc.update("""
        UPDATE pagamento
        SET status = ?
        WHERE id = ?
          AND status = 'PENDENTE'
        """,
        status,
        id
    );
}

public int restaurarTroca(
        Long cupomId,
        Long clienteId,
        BigDecimal valor,
        Long pedidoId) {

    return jdbc.update("""
        UPDATE cupom
        SET ativo = 1
        WHERE id = ?
          AND tipo = 'TROCA'
          AND cliente_id = ?
          AND valor_centavos = ?
          AND ativo = 0
          AND NOT EXISTS (
              SELECT 1
              FROM pagamento
              WHERE cupom_id = ?
                AND pedido_id <> ?
                AND status IN ('PENDENTE', 'APROVADO')
          )
        """,
        cupomId,
        clienteId,
        centavos(valor),
        cupomId,
        pedidoId
    );
}

public List<Long> listarExcedentes(Long pedidoId) {
    return jdbc.query("""
        SELECT id
        FROM cupom
        WHERE pedido_origem_id = ?
          AND tipo = 'TROCA'
        ORDER BY id
        """,
        (rs, linha) -> rs.getLong("id"),
        pedidoId
    );
}

public int ativarExcedente(
        Long id,
        LocalDate validade) {

    return jdbc.update("""
        UPDATE cupom
        SET ativo = 1,
            validade = ?
        WHERE id = ?
          AND ativo = 0
        """,
        validade.toString(),
        id
    );
}

public int desativarExcedente(Long id) {
    return jdbc.update("""
        UPDATE cupom
        SET ativo = 0
        WHERE id = ?
          AND ativo = 1
        """,
        id
    );
}

    private Long ultimoId() {
        // Usa a conexão vinculada à transação do Service.
        return jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );
    }

    private long centavos(BigDecimal valor) {
        return valor.movePointRight(2).longValueExact();
    }

    private static final String CONSULTA_ADMIN = """
    SELECT
        p.*,
        c.nome AS cliente_nome
    FROM pedido p
    JOIN cliente c ON c.id = p.cliente_id
    WHERE 1 = 1
    """;

public List<Pedido> listarAdministrativo(
        String status,
        LocalDate inicio,
        LocalDate fim) {

    var sql = new StringBuilder(CONSULTA_ADMIN);
    var parametros = new ArrayList<Object>();

    if (status != null && !status.isBlank()) {
        sql.append(" AND p.status = ?");
        parametros.add(status);
    }

    if (inicio != null) {
        sql.append(" AND p.data_pedido >= ?");
        parametros.add(inicio.atStartOfDay().toString());
    }

    if (fim != null) {
        // Inclui todo o último dia solicitado.
        sql.append(" AND p.data_pedido < ?");
        parametros.add(
            fim.plusDays(1).atStartOfDay().toString()
        );
    }

    sql.append(" ORDER BY p.data_pedido DESC, p.id DESC");

    return jdbc.query(
        sql.toString(),
        this::mapearAdministrativo,
        parametros.toArray()
    );
}

public Pedido buscarAdministrativo(Long id) {
    var encontrados = jdbc.query(
        CONSULTA_ADMIN + " AND p.id = ?",
        this::mapearAdministrativo,
        id
    );

    if (encontrados.isEmpty()) {
        return null;
    }

    Pedido pedido = encontrados.get(0);

    var itens = jdbc.query("""
        SELECT
            id,
            livro_id,
            titulo,
            quantidade,
            preco_unitario_centavos
        FROM item_pedido
        WHERE pedido_id = ?
        ORDER BY id
        """,
        (rs, linha) -> new Item(
            rs.getLong("id"),
            new Livro(
                rs.getLong("livro_id"),
                rs.getString("titulo"),
                BigDecimal.valueOf(
                    rs.getLong("preco_unitario_centavos"), 2
                )
            ),
            rs.getInt("quantidade")
        ),
        id
    );

    var pagamentos = jdbc.query("""
        SELECT
            pg.*,
            c.codigo AS codigo_cupom
        FROM pagamento pg
        LEFT JOIN cupom c ON c.id = pg.cupom_id
        WHERE pg.pedido_id = ?
        ORDER BY pg.id
        """,
        (rs, linha) -> new Pagamento(
            rs.getLong("id"),
            rs.getString("forma"),
            BigDecimal.valueOf(
                rs.getLong("valor_centavos"), 2
            ),
            rs.getString("status"),
            rs.getString("numero_mascarado"),
            rs.getString("bandeira"),
            rs.getString("codigo_cupom")
        ),
        id
    );

    return new Pedido(
        pedido.id(),
        pedido.dataPedido(),
        pedido.status(),
        pedido.cliente(),
        pedido.enderecoEntrega(),
        pedido.valorFrete(),
        pedido.valorTotal(),
        itens,
        pagamentos
    );
}

private Pedido mapearAdministrativo(
        ResultSet rs,
        int linha) throws SQLException {

    return new Pedido(
        rs.getLong("id"),
        LocalDateTime.parse(rs.getString("data_pedido")),
        rs.getString("status"),

        new Cliente(
            rs.getLong("cliente_id"),
            rs.getString("cliente_nome")
        ),

        json.readValue(
            rs.getString("endereco_entrega_json"),
            Endereco.class
        ),

        BigDecimal.valueOf(
            rs.getLong("frete_centavos"), 2
        ),

        BigDecimal.valueOf(
            rs.getLong("total_centavos"), 2
        ),

        List.of(),
        List.of()
    );
}

public int transicionarStatus(
        Long id,
        String origem,
        String destino) {

    return jdbc.update(
        """
        UPDATE pedido
        SET status = ?
        WHERE id = ?
          AND status = ?
        """,
        destino,
        id,
        origem
    );
}
}