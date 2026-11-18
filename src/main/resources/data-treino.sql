-- Executado após data.sql.
-- Pedidos sintéticos já nascem ENTREGUE e não mudam de status.

WITH RECURSIVE numeros(n) AS (
    VALUES (1)
    UNION ALL
    SELECT n + 1 FROM numeros WHERE n < 18
)
INSERT INTO cliente (
    id, nome, genero, data_nascimento, cpf, telefone,
    email, senha_hash, ativo, perfil
)
SELECT
    1000 + n,
    'Sintético ' || n,
    'Outro',
    '1990-01-01',
    printf('%011d', 90000000000 + n),
    '11999990000',
    'sintetico' || n || '@estantedosaber.local',
    '$2a$10$q7QLFBZUiaHQySPZvuQjQu.jOR6tDgl4.rusggAkJcYDP1WCRUa6q',
    1,
'CLIENTE'
FROM numeros
WHERE 1 = 1
ON CONFLICT (id) DO NOTHING;

INSERT INTO endereco (
    cliente_id, nome_curto, tipo, tipo_residencia,
    tipo_logradouro, logradouro, numero, bairro,
    cep, cidade, estado, pais
)
SELECT
    id, 'Casa', 'ENTREGA', 'Casa',
    'Rua', 'das Flores', '100', 'Centro',
    '08700000', 'Mogi das Cruzes', 'SP', 'Brasil'
FROM cliente c
WHERE id BETWEEN 1001 AND 1018
  AND NOT EXISTS (
      SELECT 1 FROM endereco e WHERE e.cliente_id = c.id
  );

INSERT INTO cartao_credito (
    cliente_id, numero_mascarado,
    nome_impresso, bandeira, preferencial
)
SELECT
    id, '**** **** **** 1111', nome, 'Visa', 1
FROM cliente c
WHERE id BETWEEN 1001 AND 1018
  AND NOT EXISTS (
      SELECT 1 FROM cartao_credito cc WHERE cc.cliente_id = c.id
  );

-- Uma linha por cliente/categoria.
CREATE TEMP TABLE demo_perfil AS
WITH categorias AS (
    SELECT
        id,
        CASE nome
            WHEN 'Fantasia' THEN 0
            WHEN 'Ficção Científica' THEN 1
            WHEN 'Autoajuda' THEN 2
            WHEN 'Literatura Clássica' THEN 3
        END AS pos
    FROM categoria
),
base AS (
    SELECT
        cl.id AS cliente_id,
        ca.id AS categoria_id,
        ca.pos,
        cl.id - 1000 AS n
    FROM cliente cl
    CROSS JOIN categorias ca
    WHERE cl.id IN (1, 3)
       OR cl.id BETWEEN 1001 AND 1018
)
SELECT
    *,
    CASE
        WHEN cliente_id = 1
            THEN CASE WHEN pos IN (0, 1) THEN 1 ELSE 0 END
        WHEN cliente_id = 3
            THEN CASE WHEN pos IN (2, 3) THEN 1 ELSE 0 END
        WHEN pos = (n - 1) % 4 THEN 1 + n % 3
        WHEN n % 3 = 0 AND pos = n % 4 THEN 1
        ELSE 0
    END AS historico,

    CASE
        WHEN cliente_id = 1
            THEN CASE WHEN pos IN (0, 1) THEN 1 ELSE 0 END
        WHEN cliente_id = 3
            THEN CASE WHEN pos IN (2, 3) THEN 1 ELSE 0 END
        WHEN n % 5 = 0 THEN 0
        WHEN pos = (n - 1) % 4 THEN 1
        WHEN n % 3 = 0 AND pos = n % 4 THEN 1
        ELSE 0
    END AS futuro,

    CASE
        WHEN cliente_id = 1
            THEN CASE WHEN pos IN (0, 1) THEN 1 ELSE 0 END
        WHEN cliente_id = 3
            THEN CASE WHEN pos IN (2, 3) THEN 1 ELSE 0 END
        WHEN n % 7 = 0 THEN 0
        WHEN pos = (n - 1) % 4 THEN 1
        WHEN n % 3 = 0 AND pos = n % 4 THEN 1
        ELSE 0
    END AS validacao,

    CASE
        WHEN cliente_id = 1
            THEN CASE WHEN pos = 3 THEN 1 ELSE 0 END
        WHEN cliente_id = 3
            THEN CASE WHEN pos = 1 THEN 1 ELSE 0 END
        WHEN n % 5 = 0 AND pos = (n - 1) % 4 THEN 1
        WHEN n % 2 = 0 AND pos = (n + 1) % 4 THEN 1
        ELSE 0
    END AS negativo
FROM base;

INSERT INTO preferencia_cliente (
    cliente_id, categoria_id, peso_declarado, vigente_de
)
SELECT
    cliente_id, categoria_id, 1.0, '2026-10-05T09:00:00'
FROM demo_perfil
WHERE cliente_id >= 1001
  AND n % 2 = 1
  AND pos = (n - 1) % 4
ON CONFLICT (cliente_id, categoria_id, vigente_de) DO NOTHING;

CREATE TEMP TABLE demo_ondas AS
SELECT
    0 AS onda,
    '2026-10-10T10:00:00' AS compra,
    '2026-10-15T10:10:00' AS mensagem
UNION ALL
SELECT 1, '2026-11-05T10:00:00', '2026-11-06T10:10:00'
UNION ALL
SELECT 2, '2026-11-14T10:00:00', '2026-11-15T10:10:00';

CREATE TEMP TABLE demo_eventos AS
SELECT
    p.*,
    o.*,
    CASE o.onda
        WHEN 0 THEN p.historico
        WHEN 1 THEN p.futuro
        ELSE p.validacao
    END AS quantidade,
    CASE p.pos
        WHEN 0 THEN '9788595084742'
        WHEN 1 THEN '9788576573135'
        WHEN 2 THEN '9788539004119'
        ELSE '9788535914849'
    END AS isbn
FROM demo_perfil p
CROSS JOIN demo_ondas o
WHERE p.cliente_id >= 1001 OR o.onda = 2;

INSERT INTO pedido (
    cliente_id, data_pedido, status, endereco_entrega_json,
    subtotal_centavos, frete_centavos, desconto_centavos, total_centavos
)
SELECT
    d.cliente_id,
    d.compra,
    'ENTREGUE',
    (
        SELECT json_object(
            'id', e.id,
            'nomeCurto', e.nome_curto,
            'tipo', e.tipo,
            'tipoResidencia', e.tipo_residencia,
            'tipoLogradouro', e.tipo_logradouro,
            'logradouro', e.logradouro,
            'numero', e.numero,
            'bairro', e.bairro,
            'cep', e.cep,
            'cidade', e.cidade,
            'estado', e.estado,
            'pais', e.pais,
            'observacoes', e.observacoes
        )
        FROM endereco e
        WHERE e.cliente_id = d.cliente_id
        ORDER BY e.id
        LIMIT 1
    ),
    SUM(d.quantidade * l.preco_centavos),
    0,
    0,
    SUM(d.quantidade * l.preco_centavos)
FROM demo_eventos d
JOIN livro l ON l.isbn = d.isbn
WHERE d.quantidade > 0
  AND NOT EXISTS (
      SELECT 1 FROM pedido p
      WHERE p.cliente_id = d.cliente_id
        AND p.data_pedido = d.compra
  )
GROUP BY d.cliente_id, d.compra;

INSERT INTO item_pedido (
    pedido_id, livro_id, titulo, quantidade, preco_unitario_centavos
)
SELECT
    p.id, l.id, l.titulo, d.quantidade, l.preco_centavos
FROM demo_eventos d
JOIN livro l ON l.isbn = d.isbn
JOIN pedido p ON p.cliente_id = d.cliente_id
             AND p.data_pedido = d.compra
WHERE d.quantidade > 0
ON CONFLICT (pedido_id, livro_id) DO NOTHING;

INSERT INTO pagamento (
    pedido_id, forma, valor_centavos, status,
    cartao_id, numero_mascarado, bandeira
)
SELECT
    p.id, 'CARTAO', p.total_centavos, 'APROVADO',
    cc.id, cc.numero_mascarado, cc.bandeira
FROM pedido p
JOIN cartao_credito cc ON cc.cliente_id = p.cliente_id
WHERE (
    p.cliente_id BETWEEN 1001 AND 1018
    OR (
        p.cliente_id IN (1, 3)
        AND p.data_pedido = '2026-11-14T10:00:00'
    )
)
AND NOT EXISTS (
    SELECT 1 FROM pagamento pg WHERE pg.pedido_id = p.id
);

INSERT INTO log_transacao (
    data_hora, usuario, operacao, tabela, registro_id, dados
)
SELECT
    p.data_pedido,
    'carga-treino',
    'INSERT',
    'pedido',
    p.id,
    json_object('status', 'ENTREGUE', 'origem', 'massa-treino')
FROM pedido p
WHERE (
    p.cliente_id BETWEEN 1001 AND 1018
    OR (
        p.cliente_id IN (1, 3)
        AND p.data_pedido = '2026-11-14T10:00:00'
    )
)
AND NOT EXISTS (
    SELECT 1 FROM log_transacao lt
    WHERE lt.tabela = 'pedido' AND lt.registro_id = p.id
);

INSERT INTO conversa (cliente_id, iniciada_em)
SELECT id, '2026-10-15T10:00:00'
FROM cliente c
WHERE id BETWEEN 1001 AND 1018
  AND NOT EXISTS (
      SELECT 1 FROM conversa cv WHERE cv.cliente_id = c.id
  );

-- Fundação tem somente Ficção Científica nesta massa.
CREATE TEMP TABLE demo_respostas AS
SELECT
    d.*,
    l.id AS livro_id,
    strftime(
        '%Y-%m-%dT%H:%M:%S',
        d.mensagem,
        '+' || d.pos || ' minutes'
    ) AS instante,
    CASE WHEN d.negativo = 1 THEN 0 ELSE 1 END AS util
FROM demo_eventos d
JOIN livro l ON l.isbn =
    CASE
        WHEN d.pos = 1 THEN '9788576572000'
        ELSE d.isbn
    END;

INSERT INTO mensagem_chat (
    conversa_id, papel, texto, criada_em, provedor
)
SELECT
    cv.id,
    'ASSISTENTE',
    'Recomendação sintética para treinamento.',
    d.instante,
    'MASSA_DEMONSTRACAO'
FROM demo_respostas d
JOIN conversa cv ON cv.cliente_id = d.cliente_id
                AND cv.iniciada_em = '2026-10-15T10:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM mensagem_chat m
    WHERE m.conversa_id = cv.id AND m.criada_em = d.instante
);

INSERT INTO recomendacao_mensagem (mensagem_chat_id, livro_id)
SELECT m.id, d.livro_id
FROM demo_respostas d
JOIN conversa cv ON cv.cliente_id = d.cliente_id
                AND cv.iniciada_em = '2026-10-15T10:00:00'
JOIN mensagem_chat m ON m.conversa_id = cv.id
                   AND m.criada_em = d.instante
WHERE 1 = 1
ON CONFLICT (mensagem_chat_id, livro_id) DO NOTHING;

INSERT INTO feedback_resposta (mensagem_chat_id, util, criada_em)
SELECT
    m.id,
    d.util,
    strftime('%Y-%m-%dT%H:%M:%S', d.instante, '+10 seconds')
FROM demo_respostas d
JOIN conversa cv ON cv.cliente_id = d.cliente_id
                AND cv.iniciada_em = '2026-10-15T10:00:00'
JOIN mensagem_chat m ON m.conversa_id = cv.id
                   AND m.criada_em = d.instante
WHERE 1 = 1
ON CONFLICT (mensagem_chat_id) DO NOTHING;

DROP TABLE demo_respostas;
DROP TABLE demo_eventos;
DROP TABLE demo_ondas;
DROP TABLE demo_perfil;