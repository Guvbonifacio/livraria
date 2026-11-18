package com.fatec.livraria.repository;

import com.fatec.livraria.model.ModeloRecomendacao;
import com.fatec.livraria.model.ModeloRecomendacao.Coeficiente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecomendacaoRepository {

    public record Linha(
        long clienteId,
        long categoriaId,
        String categoria,
        long quantidade,
        long valor,
        Double dias,
        double pesoDeclarado,
        long votosUteis,
        long votosNaoUteis,
        int rotulo
    ) {}

    private final JdbcTemplate jdbc;

    public RecomendacaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Features anteriores ao corte.
     * Rótulos entre corte inclusivo e fim exclusivo.
     *
     * Limitação declarada: status atual dos pedidos;
     * a massa não realiza transições posteriores ao corte.
     */
    public List<Linha> extrair(
            LocalDateTime corte,
            LocalDateTime fimExclusivo) {

        return jdbc.query("""
            WITH limites AS (
                SELECT ? AS corte, ? AS fim
            ),
            compras AS (
                SELECT
                    p.cliente_id,
                    lc.categoria_id,
                    SUM(ip.quantidade) AS quantidade,
                    SUM(ip.quantidade * ip.preco_unitario_centavos) AS valor,
                    MAX(p.data_pedido) AS ultima

                FROM pedido p
                JOIN item_pedido ip ON ip.pedido_id = p.id
                JOIN livro_categoria lc ON lc.livro_id = ip.livro_id

                WHERE julianday(p.data_pedido)
                    < julianday((SELECT corte FROM limites))
                  AND p.status IN (
                      'APROVADA', 'EM TRANSPORTE', 'ENTREGUE'
                  )

                GROUP BY p.cliente_id, lc.categoria_id
            ),
            votos AS (
                SELECT DISTINCT
                    cv.cliente_id,
                    lc.categoria_id,
                    m.id AS mensagem_id,
                    f.util,
                    f.criada_em

                FROM feedback_resposta f
                JOIN mensagem_chat m ON m.id = f.mensagem_chat_id
                JOIN conversa cv ON cv.id = m.conversa_id
                JOIN recomendacao_mensagem rm
                    ON rm.mensagem_chat_id = m.id
                JOIN livro_categoria lc ON lc.livro_id = rm.livro_id

                WHERE m.papel = 'ASSISTENTE'
                  AND m.tipo = 'TEXTO'
            ),
            votos_antes AS (
                SELECT
                    cliente_id,
                    categoria_id,
                    COUNT(DISTINCT CASE
                        WHEN util = 1 THEN mensagem_id
                    END) AS uteis,
                    COUNT(DISTINCT CASE
                        WHEN util = 0 THEN mensagem_id
                    END) AS nao_uteis

                FROM votos
                WHERE julianday(criada_em)
                    < julianday((SELECT corte FROM limites))

                GROUP BY cliente_id, categoria_id
            ),
            preferencias AS (
                SELECT *,
                    ROW_NUMBER() OVER (
                        PARTITION BY cliente_id, categoria_id
                        ORDER BY vigente_de DESC, id DESC
                    ) AS posicao

                FROM preferencia_cliente
                WHERE julianday(vigente_de)
                    < julianday((SELECT corte FROM limites))
            ),
            positivos AS (
                SELECT p.cliente_id, lc.categoria_id

                FROM pedido p
                JOIN item_pedido ip ON ip.pedido_id = p.id
                JOIN livro_categoria lc ON lc.livro_id = ip.livro_id

                WHERE julianday(p.data_pedido)
                    >= julianday((SELECT corte FROM limites))
                  AND julianday(p.data_pedido)
                    < julianday((SELECT fim FROM limites))
                  AND p.status IN (
                      'APROVADA', 'EM TRANSPORTE', 'ENTREGUE'
                  )

                UNION

                SELECT cliente_id, categoria_id
                FROM votos
                WHERE util = 1
                  AND julianday(criada_em)
                    >= julianday((SELECT corte FROM limites))
                  AND julianday(criada_em)
                    < julianday((SELECT fim FROM limites))
            )
            SELECT
                cl.id AS cliente_id,
                ca.id AS categoria_id,
                ca.nome,
                COALESCE(c.quantidade, 0) AS quantidade,
                COALESCE(c.valor, 0) AS valor,

                julianday((SELECT corte FROM limites))
                    - julianday(c.ultima) AS dias,

                COALESCE(pr.peso_declarado, 0) AS peso,
                COALESCE(v.uteis, 0) AS uteis,
                COALESCE(v.nao_uteis, 0) AS nao_uteis,

                CASE
                    WHEN po.cliente_id IS NULL THEN 0
                    ELSE 1
                END AS rotulo

            FROM cliente cl
            CROSS JOIN categoria ca

            LEFT JOIN compras c
                ON c.cliente_id = cl.id
               AND c.categoria_id = ca.id

            LEFT JOIN votos_antes v
                ON v.cliente_id = cl.id
               AND v.categoria_id = ca.id

            LEFT JOIN preferencias pr
                ON pr.cliente_id = cl.id
               AND pr.categoria_id = ca.id
               AND pr.posicao = 1

            LEFT JOIN positivos po
                ON po.cliente_id = cl.id
               AND po.categoria_id = ca.id

            WHERE cl.perfil = 'CLIENTE'
            ORDER BY cl.id, ca.id
            """,
            (rs, n) -> new Linha(
                rs.getLong("cliente_id"),
                rs.getLong("categoria_id"),
                rs.getString("nome"),
                rs.getLong("quantidade"),
                rs.getLong("valor"),
                rs.getObject("dias") == null
                    ? null
                    : rs.getDouble("dias"),
                rs.getDouble("peso"),
                rs.getLong("uteis"),
                rs.getLong("nao_uteis"),
                rs.getInt("rotulo")
            ),
            corte.toString(),
            fimExclusivo.toString()
        );
    }

    public Long salvar(ModeloRecomendacao m) {
        return jdbc.queryForObject("""
            INSERT INTO modelo_recomendacao (
                versao, treinado_em,
                data_corte, fim_observacao, fim_validacao,
                amostras, iteracoes, taxa_aprendizado,
                perda_inicial, perda_final,
                metrica_validacao, intercepto
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """,
            Long.class,
            m.versao(),
            m.treinadoEm().toString(),
            m.dataCorte().toString(),
            m.fimObservacao().toString(),
            m.fimValidacao().toString(),
            m.amostras(),
            m.iteracoes(),
            m.taxaAprendizado(),
            m.perdaInicial(),
            m.perdaFinal(),
            m.metricaValidacao(),
            m.intercepto()
        );
    }

    public void salvarCoeficiente(Long id, Coeficiente c) {
        jdbc.update("""
            INSERT INTO coeficiente_modelo (
                modelo_id, feature, coeficiente, media, desvio
            )
            VALUES (?, ?, ?, ?, ?)
            """,
            id, c.feature(), c.coeficiente(), c.media(), c.desvio()
        );
    }

    public void salvarVies(
            Long id,
            Long categoriaId,
            double vies) {

        jdbc.update("""
            INSERT INTO vies_categoria (
                modelo_id, categoria_id, vies
            )
            VALUES (?, ?, ?)
            """,
            id, categoriaId, vies
        );
    }

    public ModeloRecomendacao buscarAtual() {
        var ids = jdbc.query("""
            SELECT id
            FROM modelo_recomendacao
            ORDER BY id DESC
            LIMIT 1
            """,
            (rs, n) -> rs.getLong("id")
        );

        return ids.isEmpty() ? null : buscar(ids.get(0));
    }

    public ModeloRecomendacao buscar(Long id) {
        var coeficientes = jdbc.query("""
            SELECT feature, coeficiente, media, desvio
            FROM coeficiente_modelo
            WHERE modelo_id = ?
            ORDER BY feature
            """,
            (rs, n) -> new Coeficiente(
                rs.getString("feature"),
                rs.getDouble("coeficiente"),
                rs.getDouble("media"),
                rs.getDouble("desvio")
            ),
            id
        );

        Map<Long, Double> vieses = new LinkedHashMap<>();

        jdbc.query("""
            SELECT categoria_id, vies
            FROM vies_categoria
            WHERE modelo_id = ?
            """,
            rs -> {
                vieses.put(
                    rs.getLong("categoria_id"),
                    rs.getDouble("vies")
                );
            },
            id
        );

        var encontrados = jdbc.query("""
            SELECT *
            FROM modelo_recomendacao
            WHERE id = ?
            """,
            (rs, n) -> new ModeloRecomendacao(
                rs.getLong("id"),
                rs.getString("versao"),
                LocalDateTime.parse(rs.getString("treinado_em")),
                LocalDate.parse(rs.getString("data_corte")),
                LocalDate.parse(rs.getString("fim_observacao")),
                LocalDate.parse(rs.getString("fim_validacao")),
                rs.getInt("amostras"),
                rs.getInt("iteracoes"),
                rs.getDouble("taxa_aprendizado"),
                rs.getDouble("perda_inicial"),
                rs.getDouble("perda_final"),
                rs.getObject("metrica_validacao") == null
                    ? null
                    : rs.getDouble("metrica_validacao"),
                rs.getDouble("intercepto"),
                coeficientes,
                vieses
            ),
            id
        );

        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    public Map<Long, List<Long>> categoriasDosLivros() {
        Map<Long, List<Long>> resultado = new LinkedHashMap<>();

        jdbc.query("""
            SELECT livro_id, categoria_id
            FROM livro_categoria
            """,
            rs -> {
                while (rs.next()) {
                    resultado.computeIfAbsent(
                        rs.getLong(1),
                        k -> new ArrayList<>()
                    ).add(rs.getLong(2));
                }
                return resultado;
            }
        );

        return resultado;
    }
}