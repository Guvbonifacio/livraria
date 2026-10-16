package com.fatec.livraria.repository;

import com.fatec.livraria.model.PlanoPagamento.Parcela;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbc;

    public PedidoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
}