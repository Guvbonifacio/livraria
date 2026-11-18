-- Dados ilustrativos para desenvolvimento.
-- Valores monetários em centavos.

INSERT INTO grupo_precificacao (nome, margem_percentual)
VALUES ('Padrão', 50)
ON CONFLICT (nome) DO NOTHING;

INSERT INTO categoria (nome)
VALUES
    ('Autoajuda'),
    ('Fantasia'),
    ('Literatura Clássica'),
    ('Ficção Científica')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO livro (
    autor,
    ano,
    titulo,
    editora,
    edicao,
    isbn,
    paginas,
    sinopse,
    altura_mm,
    largura_mm,
    profundidade_mm,
    peso_g,
    grupo_precificacao_id,
    codigo_barras,
    preco_centavos,
    imagem_url
)
VALUES
(
    'J. R. R. Tolkien',
    2019,
    'O Hobbit',
    'Editora Demonstração',
    '1ª',
    '9788595084742',
    336,
    'Uma aventura de Bilbo Bolseiro pela Terra Média.',
    210,
    140,
    20,
    400,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788595084742',
    3000,
    '/img/logo.png'
),
(
    'Machado de Assis',
    2020,
    'Dom Casmurro',
    'Editora Demonstração',
    '1ª',
    '9788535914849',
    256,
    'Memórias de Bentinho e sua relação com Capitu.',
    210,
    140,
    18,
    350,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788535914849',
    4500,
    '/img/logo.png'
),
(
    'Frank Herbert',
    2021,
    'Duna',
    'Editora Demonstração',
    '1ª',
    '9788576573135',
    680,
    'Disputas políticas e a sobrevivência no planeta Arrakis.',
    230,
    160,
    40,
    800,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788576573135',
    6000,
    '/img/logo.png'
)
ON CONFLICT (isbn) DO NOTHING;

-- Associa os livros às categorias.
INSERT INTO livro_categoria (livro_id, categoria_id)
SELECT l.id, c.id
FROM livro l
CROSS JOIN categoria c
WHERE
    (
        l.isbn = '9788595084742'
        AND c.nome = 'Fantasia'
    )
    OR
    (
        l.isbn = '9788535914849'
        AND c.nome = 'Literatura Clássica'
    )
    OR
    (
        l.isbn = '9788576573135'
        AND c.nome IN ('Ficção Científica', 'Fantasia')
    )
ON CONFLICT (livro_id, categoria_id) DO NOTHING;

-- O Hobbit: dois lotes, disponibilidade inicial = 8 + 4 = 12.
INSERT INTO item_estoque (
    livro_id,
    quantidade,
    valor_custo_centavos,
    fornecedor,
    data_entrada,
    referencia_entrada
)
SELECT
    id,
    8,
    2000,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-HOBBIT-LOTE-1'
FROM livro
WHERE isbn = '9788595084742'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id,
    quantidade,
    valor_custo_centavos,
    fornecedor,
    data_entrada,
    referencia_entrada
)
SELECT
    id,
    4,
    2000,
    'Distribuidora Demonstração',
    '2026-10-02',
    'DEMO-HOBBIT-LOTE-2'
FROM livro
WHERE isbn = '9788595084742'
ON CONFLICT (referencia_entrada) DO NOTHING;

-- Dom Casmurro: disponibilidade inicial = 5.
INSERT INTO item_estoque (
    livro_id,
    quantidade,
    valor_custo_centavos,
    fornecedor,
    data_entrada,
    referencia_entrada
)
SELECT
    id,
    5,
    3000,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-CASMURRO-LOTE-1'
FROM livro
WHERE isbn = '9788535914849'
ON CONFLICT (referencia_entrada) DO NOTHING;

-- Duna permanece sem lote para demonstrar disponibilidade zero.

INSERT INTO cupom (
    codigo,
    tipo,
    valor_centavos,
    validade
)
VALUES
    ('BEMVINDO10', 'PROMOCIONAL', 1000, '2027-12-31'),
    ('LEITURA15', 'PROMOCIONAL', 1500, '2027-12-31')
ON CONFLICT (codigo) DO NOTHING;

-- Administrador de demonstração.
-- Senha pública de demonstração: Demo@123, armazenada como BCrypt.
INSERT INTO cliente (
    id,
    nome,
    genero,
    data_nascimento,
    cpf,
    telefone,
    email,
    senha_hash,
    ativo,
    perfil
)
VALUES (
    2,
    'Administrador Demonstração',
    'Outro',
    '1990-01-01',
    '11144477735',
    '11999990002',
    'admin@estantedosaber.local',
    '$2a$10$q7QLFBZUiaHQySPZvuQjQu.jOR6tDgl4.rusggAkJcYDP1WCRUa6q',
    1,
    'ADMIN'
)
ON CONFLICT DO NOTHING;

-- =====================================================================
-- Catálogo de demonstração ampliado para 15 livros, mantendo as quatro
-- categorias do DRS: Autoajuda, Fantasia, Literatura Clássica e
-- Ficção Científica.
--
-- Os três livros acima são preservados: dez specs Cypress os localizam
-- pelo título, o vendas:preparar reexecuta este arquivo, e Duna segue
-- sem lote de propósito, para demonstrar indisponibilidade.
--
-- ISBN e código de barras são valores de demonstração, não verificados.
-- =====================================================================

INSERT INTO livro (
    autor, ano, titulo, editora, edicao, isbn, paginas, sinopse,
    altura_mm, largura_mm, profundidade_mm, peso_g,
    grupo_precificacao_id, codigo_barras, preco_centavos, imagem_url
)
VALUES
(
    'J. R. R. Tolkien',
    2019,
    'O Senhor dos Anéis: A Sociedade do Anel',
    'Editora Demonstração',
    '1ª',
    '9788533613379',
    576,
    'Frodo recebe o Um Anel e parte de Valfenda com a Sociedade para destruí-lo.',
    230,
    160,
    35,
    700,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788533613379',
    6990,
    '/img/logo.png'
),
(
    'J. K. Rowling',
    2017,
    'Harry Potter e a Pedra Filosofal',
    'Editora Demonstração',
    '1ª',
    '9788532530783',
    264,
    'Um órfão descobre no aniversário de onze anos que é bruxo e vai para Hogwarts.',
    210,
    140,
    20,
    380,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788532530783',
    4490,
    '/img/logo.png'
),
(
    'William Gibson',
    2016,
    'Neuromancer',
    'Editora Demonstração',
    '1ª',
    '9788576572000',
    320,
    'Um hacker decadente aceita um último trabalho que o leva ao ciberespaço.',
    210,
    140,
    22,
    420,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788576572000',
    5490,
    '/img/logo.png'
),
(
    'Isaac Asimov',
    2020,
    'Fundação',
    'Editora Demonstração',
    '1ª',
    '9788576573111',
    288,
    'Hari Seldon prevê a queda do Império e cria a Fundação para encurtar a escuridão.',
    210,
    140,
    20,
    380,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788576573111',
    4990,
    '/img/logo.png'
),
(
    'George Orwell',
    2018,
    '1984',
    'Editora Demonstração',
    '1ª',
    '9788535902233',
    416,
    'Winston Smith vive sob vigilância total e começa a duvidar do Partido.',
    210,
    140,
    26,
    450,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788535902233',
    3990,
    '/img/logo.png'
),
(
    'Machado de Assis',
    2019,
    'Memórias Póstumas de Brás Cubas',
    'Editora Demonstração',
    '1ª',
    '9788535910666',
    288,
    'Um defunto autor narra a própria vida com ironia e liberdade.',
    210,
    140,
    20,
    360,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788535910666',
    3490,
    '/img/logo.png'
),
(
    'Jane Austen',
    2018,
    'Orgulho e Preconceito',
    'Editora Demonstração',
    '1ª',
    '9788544001820',
    424,
    'Elizabeth Bennet e o senhor Darcy entre mal-entendidos, orgulho e reputação.',
    210,
    140,
    26,
    460,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788544001820',
    3790,
    '/img/logo.png'
),
(
    'Aluísio Azevedo',
    2017,
    'O Cortiço',
    'Editora Demonstração',
    '1ª',
    '9788508133031',
    240,
    'A vida de um cortiço no Rio de Janeiro do fim do século XIX.',
    210,
    140,
    18,
    320,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788508133031',
    2990,
    '/img/logo.png'
),
(
    'Charles Duhigg',
    2018,
    'O Poder do Hábito',
    'Editora Demonstração',
    '1ª',
    '9788539004119',
    408,
    'Como os hábitos se formam no cérebro e como podem ser reescritos.',
    230,
    160,
    28,
    520,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788539004119',
    4490,
    '/img/logo.png'
),
(
    'Carol S. Dweck',
    2017,
    'Mindset: A Nova Psicologia do Sucesso',
    'Editora Demonstração',
    '1ª',
    '9788547000240',
    312,
    'A diferença entre mentalidade fixa e de crescimento e seu efeito no aprendizado.',
    210,
    140,
    22,
    400,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788547000240',
    3990,
    '/img/logo.png'
),
(
    'Stephen R. Covey',
    2019,
    'Os 7 Hábitos das Pessoas Altamente Eficazes',
    'Editora Demonstração',
    '1ª',
    '9788568905067',
    432,
    'Sete princípios de eficácia pessoal e interpessoal, do caráter aos resultados.',
    230,
    160,
    30,
    560,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788568905067',
    5990,
    '/img/logo.png'
),
(
    'Hal Elrod',
    2016,
    'O Milagre da Manhã',
    'Editora Demonstração',
    '1ª',
    '9788581637461',
    208,
    'Seis práticas para a primeira hora do dia e seu efeito na rotina.',
    210,
    140,
    16,
    300,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788581637461',
    3690,
    '/img/logo.png'
)
ON CONFLICT (isbn) DO NOTHING;

-- Associação aos livros novos. 1984 entra em duas categorias, como Duna.
INSERT INTO livro_categoria (livro_id, categoria_id)
SELECT l.id, c.id
FROM livro l
CROSS JOIN categoria c
WHERE
    (
        l.isbn = '9788533613379'
        AND c.nome IN ('Fantasia')
    )
    OR
    (
        l.isbn = '9788532530783'
        AND c.nome IN ('Fantasia')
    )
    OR
    (
        l.isbn = '9788576572000'
        AND c.nome IN ('Ficção Científica')
    )
    OR
    (
        l.isbn = '9788576573111'
        AND c.nome IN ('Ficção Científica')
    )
    OR
    (
        l.isbn = '9788535902233'
        AND c.nome IN ('Ficção Científica', 'Literatura Clássica')
    )
    OR
    (
        l.isbn = '9788535910666'
        AND c.nome IN ('Literatura Clássica')
    )
    OR
    (
        l.isbn = '9788544001820'
        AND c.nome IN ('Literatura Clássica')
    )
    OR
    (
        l.isbn = '9788508133031'
        AND c.nome IN ('Literatura Clássica')
    )
    OR
    (
        l.isbn = '9788539004119'
        AND c.nome IN ('Autoajuda')
    )
    OR
    (
        l.isbn = '9788547000240'
        AND c.nome IN ('Autoajuda')
    )
    OR
    (
        l.isbn = '9788568905067'
        AND c.nome IN ('Autoajuda')
    )
    OR
    (
        l.isbn = '9788581637461'
        AND c.nome IN ('Autoajuda')
    )
ON CONFLICT (livro_id, categoria_id) DO NOTHING;

-- Um lote de 10 unidades para cada livro novo.
INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    4000,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788533613379'
FROM livro
WHERE isbn = '9788533613379'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2600,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788532530783'
FROM livro
WHERE isbn = '9788532530783'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    3200,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788576572000'
FROM livro
WHERE isbn = '9788576572000'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2900,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788576573111'
FROM livro
WHERE isbn = '9788576573111'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2300,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788535902233'
FROM livro
WHERE isbn = '9788535902233'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2000,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788535910666'
FROM livro
WHERE isbn = '9788535910666'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2200,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788544001820'
FROM livro
WHERE isbn = '9788544001820'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    1700,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788508133031'
FROM livro
WHERE isbn = '9788508133031'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2600,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788539004119'
FROM livro
WHERE isbn = '9788539004119'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2300,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788547000240'
FROM livro
WHERE isbn = '9788547000240'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    3500,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788568905067'
FROM livro
WHERE isbn = '9788568905067'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2100,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788581637461'
FROM livro
WHERE isbn = '9788581637461'
ON CONFLICT (referencia_entrada) DO NOTHING;

-- =========================================================
-- MASSA DO CHATBOT REVISADA
--
-- Cliente A: ID 1
-- Administrador existente: ID 2
-- Cliente B: ID 3
--
-- Corte: 2026-11-01T00:00:00
-- Fim exclusivo: 2026-11-10T00:00:00
-- =========================================================

-- Senha pública de demonstração: Demo@123, em BCrypt.
INSERT INTO cliente (
    id, nome, genero, data_nascimento, cpf, telefone,
    email, senha_hash, ativo, perfil
)
VALUES
(
    1,
    'Cliente A Demonstração',
    'Outro',
    '1995-05-20',
    '52998224725',
    '11999990001',
    'cliente.a@estantedosaber.local',
    '$2a$10$q7QLFBZUiaHQySPZvuQjQu.jOR6tDgl4.rusggAkJcYDP1WCRUa6q',
    1,
    'CLIENTE'
),
(
    3,
    'Cliente B Demonstração',
    'Outro',
    '1994-08-15',
    '12345678909',
    '11999990003',
    'cliente.b@estantedosaber.local',
    '$2a$10$q7QLFBZUiaHQySPZvuQjQu.jOR6tDgl4.rusggAkJcYDP1WCRUa6q',
    1,
    'CLIENTE'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO endereco (
    cliente_id, nome_curto, tipo, tipo_residencia,
    tipo_logradouro, logradouro, numero, bairro, cep,
    cidade, estado, pais, observacoes
)
SELECT
    c.id, 'Casa', 'ENTREGA', 'Casa', 'Rua',
    'das Flores', '100', 'Centro', '08700000',
    'Mogi das Cruzes', 'SP', 'Brasil', NULL
FROM cliente c
WHERE c.id IN (1, 3)
  AND NOT EXISTS (
      SELECT 1
      FROM endereco e
      WHERE e.cliente_id = c.id
  );

INSERT INTO cartao_credito (
    cliente_id, numero_mascarado,
    nome_impresso, bandeira, preferencial
)
SELECT
    c.id, '**** **** **** 1111', c.nome, 'Visa', 1
FROM cliente c
WHERE c.id IN (1, 3)
  AND NOT EXISTS (
      SELECT 1
      FROM cartao_credito cc
      WHERE cc.cliente_id = c.id
  );

-- Completa Autoajuda sem duplicar um ISBN existente.
INSERT INTO livro (
    autor, ano, titulo, editora, edicao, isbn,
    paginas, sinopse,
    altura_mm, largura_mm, profundidade_mm, peso_g,
    grupo_precificacao_id,
    codigo_barras, preco_centavos, imagem_url
)
VALUES (
    'Charles Duhigg',
    2012,
    'O Poder do Hábito',
    'Editora Demonstração',
    '1ª',
    '9788539004119',
    408,
    'Uma investigação sobre os hábitos e suas transformações.',
    230, 160, 25, 500,
    (
        SELECT id
        FROM grupo_precificacao
        WHERE nome = 'Padrão'
    ),
    '9788539004119',
    4000,
    '/img/logo.png'
)
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO livro_categoria (livro_id, categoria_id)
SELECT l.id, c.id
FROM livro l
CROSS JOIN categoria c
WHERE l.isbn = '9788539004119'
  AND c.nome = 'Autoajuda'
ON CONFLICT (livro_id, categoria_id) DO NOTHING;

-- Saldo atual. A carga de pedidos históricos não faz nova baixa.
INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    8,
    2500,
    'Distribuidora Demonstração',
    '2026-11-01',
    'DEMO-AUTOAJUDA-LOTE-1'
FROM livro
WHERE isbn = '9788539004119'
ON CONFLICT (referencia_entrada) DO NOTHING;

-- Tabela temporária usada somente para organizar a carga.
CREATE TEMP TABLE demo_chat_compras (
    cliente_id INTEGER NOT NULL,
    isbn TEXT NOT NULL,
    data_compra TEXT NOT NULL
);

-- Duas compras por cliente: uma antes e outra depois do corte.
INSERT INTO demo_chat_compras VALUES
    (1, '9788595084742', '2026-10-10T10:00:00'),
    (1, '9788576573135', '2026-10-10T10:00:00'),
    (3, '9788539004119', '2026-10-10T11:00:00'),
    (3, '9788535914849', '2026-10-10T11:00:00'),

    (1, '9788595084742', '2026-11-05T10:00:00'),
    (1, '9788576573135', '2026-11-05T10:00:00'),
    (3, '9788539004119', '2026-11-05T11:00:00'),
    (3, '9788535914849', '2026-11-05T11:00:00');

INSERT INTO pedido (
    cliente_id, data_pedido, status, endereco_entrega_json,
    subtotal_centavos, frete_centavos,
    desconto_centavos, total_centavos
)
SELECT
    d.cliente_id,
    d.data_compra,
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

    SUM(l.preco_centavos),
    0,
    0,
    SUM(l.preco_centavos)

FROM demo_chat_compras d
JOIN livro l ON l.isbn = d.isbn

WHERE NOT EXISTS (
    SELECT 1
    FROM pedido p
    WHERE p.cliente_id = d.cliente_id
      AND p.data_pedido = d.data_compra
)

GROUP BY d.cliente_id, d.data_compra;

INSERT INTO item_pedido (
    pedido_id, livro_id, titulo,
    quantidade, preco_unitario_centavos
)
SELECT
    p.id, l.id, l.titulo, 1, l.preco_centavos
FROM demo_chat_compras d
JOIN livro l ON l.isbn = d.isbn
JOIN pedido p ON p.cliente_id = d.cliente_id
             AND p.data_pedido = d.data_compra
WHERE 1 = 1
ON CONFLICT (pedido_id, livro_id) DO NOTHING;

INSERT INTO pagamento (
    pedido_id, forma, valor_centavos, status,
    cartao_id, numero_mascarado, bandeira
)
SELECT
    p.id,
    'CARTAO',
    p.total_centavos,
    'APROVADO',
    cc.id,
    cc.numero_mascarado,
    cc.bandeira

FROM pedido p

JOIN cartao_credito cc ON cc.id = (
    SELECT id
    FROM cartao_credito
    WHERE cliente_id = p.cliente_id
    ORDER BY preferencial DESC, id
    LIMIT 1
)

WHERE p.cliente_id IN (1, 3)
  AND EXISTS (
      SELECT 1
      FROM demo_chat_compras d
      WHERE d.cliente_id = p.cliente_id
        AND d.data_compra = p.data_pedido
  )
  AND NOT EXISTS (
      SELECT 1
      FROM pagamento pg
      WHERE pg.pedido_id = p.id
  );

-- Eventos históricos da carga, identificados como demonstração.
-- Não representam operações realizadas agora pelo administrador.
INSERT INTO log_transacao (
    data_hora, usuario, operacao,
    tabela, registro_id, dados
)
SELECT
    p.data_pedido,
    'carga-demonstracao-chat',
    'INSERT',
    'pedido',
    p.id,
    json_object(
        'status', 'ENTREGUE',
        'origem', 'massa-chat'
    )

FROM pedido p

WHERE EXISTS (
    SELECT 1
    FROM demo_chat_compras d
    WHERE d.cliente_id = p.cliente_id
      AND d.data_compra = p.data_pedido
)
AND NOT EXISTS (
    SELECT 1
    FROM log_transacao lt
    WHERE lt.tabela = 'pedido'
      AND lt.registro_id = p.id
      AND lt.usuario = 'carga-demonstracao-chat'
);

DROP TABLE demo_chat_compras;

-- Preferências append-only: somente cliente A, antes do corte.
INSERT INTO preferencia_cliente (
    cliente_id,
    categoria_id,
    peso_declarado,
    vigente_de
)
SELECT
    1,
    id,
    1.0,
    '2026-10-05T09:00:00'
FROM categoria
WHERE nome IN ('Fantasia', 'Autoajuda')
ON CONFLICT (cliente_id, categoria_id, vigente_de) DO NOTHING;

INSERT INTO conversa (cliente_id, iniciada_em)
SELECT
    c.id, '2026-10-15T10:00:00'
FROM cliente c
WHERE c.id IN (1, 3)
  AND NOT EXISTS (
      SELECT 1
      FROM conversa cv
      WHERE cv.cliente_id = c.id
        AND cv.iniciada_em = '2026-10-15T10:00:00'
  );

CREATE TEMP TABLE demo_chat_mensagens (
    cliente_id INTEGER NOT NULL,
    papel TEXT NOT NULL,
    texto TEXT NOT NULL,
    criada_em TEXT NOT NULL,
    util INTEGER,
    isbns TEXT NOT NULL
);

-- Textos sintéticos para a massa.
-- A lista de ISBNs define quais livros foram recomendados.
-- Lista vazia significa que não houve recomendação.
INSERT INTO demo_chat_mensagens VALUES
(
    1, 'CLIENTE',
    'Quero uma indicação de fantasia.',
    '2026-10-15T10:00:00', NULL, '[]'
),
(
    1, 'ASSISTENTE',
    'O Hobbit é uma opção de Fantasia.',
    '2026-10-15T10:00:01', 1,
    '["9788595084742"]'
),
(
    1, 'CLIENTE',
    'E uma indicação diferente?',
    '2026-10-15T10:01:00', NULL, '[]'
),
(
    1, 'ASSISTENTE',
    'Dom Casmurro é uma opção de Literatura Clássica.',
    '2026-10-15T10:01:01', 0,
    '["9788535914849"]'
),
(
    3, 'CLIENTE',
    'Quero uma indicação de autoajuda.',
    '2026-10-15T10:00:00', NULL, '[]'
),
(
    3, 'ASSISTENTE',
    'O Poder do Hábito é uma opção de Autoajuda.',
    '2026-10-15T10:00:01', 1,
    '["9788539004119"]'
),
(
    3, 'CLIENTE',
    'E ficção científica?',
    '2026-10-15T10:01:00', NULL, '[]'
),
(
    3, 'ASSISTENTE',
    'Duna é uma obra de Ficção Científica e Fantasia.',
    '2026-10-15T10:01:01', 0,
    '["9788576573135"]'
),
(
    3, 'CLIENTE',
    'Como vejo meus pedidos?',
    '2026-10-20T10:00:00', NULL, '[]'
),
(
    3, 'ASSISTENTE',
    'Use o menu Meus Pedidos.',
    '2026-10-20T10:00:01', 1, '[]'
),
(
    1, 'CLIENTE',
    'Quero outra indicação de fantasia.',
    '2026-11-06T10:00:00', NULL, '[]'
),
(
    1, 'ASSISTENTE',
    'O Hobbit é uma opção de Fantasia.',
    '2026-11-06T10:00:01', 1,
    '["9788595084742"]'
),
(
    1, 'CLIENTE',
    'E um clássico?',
    '2026-11-06T10:01:00', NULL, '[]'
),
(
    1, 'ASSISTENTE',
    'Dom Casmurro é uma opção de Literatura Clássica.',
    '2026-11-06T10:01:01', 0,
    '["9788535914849"]'
),
(
    3, 'CLIENTE',
    'Quero uma indicação de um clássico.',
    '2026-11-06T10:00:00', NULL, '[]'
),
(
    3, 'ASSISTENTE',
    'Dom Casmurro é uma opção de Literatura Clássica.',
    '2026-11-06T10:00:01', 1,
    '["9788535914849"]'
),
(
    3, 'CLIENTE',
    'E uma aventura?',
    '2026-11-06T10:01:00', NULL, '[]'
),
(
    3, 'ASSISTENTE',
    'O Hobbit é uma opção de Fantasia.',
    '2026-11-06T10:01:01', 0,
    '["9788595084742"]'
);

INSERT INTO mensagem_chat (
    conversa_id, papel, texto, criada_em, provedor
)
SELECT
    cv.id,
    d.papel,
    d.texto,
    d.criada_em,

    CASE
        WHEN d.papel = 'ASSISTENTE'
        THEN 'MASSA_DEMONSTRACAO'
    END

FROM demo_chat_mensagens d

JOIN conversa cv ON cv.cliente_id = d.cliente_id
                AND cv.iniciada_em = '2026-10-15T10:00:00'

WHERE NOT EXISTS (
    SELECT 1
    FROM mensagem_chat m
    WHERE m.conversa_id = cv.id
      AND m.papel = d.papel
      AND m.criada_em = d.criada_em
);

INSERT INTO recomendacao_mensagem (
    mensagem_chat_id, livro_id
)
SELECT
    m.id, l.id

FROM demo_chat_mensagens d

JOIN conversa cv ON cv.cliente_id = d.cliente_id
                AND cv.iniciada_em = '2026-10-15T10:00:00'

JOIN mensagem_chat m ON m.conversa_id = cv.id
                   AND m.papel = d.papel
                   AND m.criada_em = d.criada_em

JOIN json_each(d.isbns) j ON 1 = 1
JOIN livro l ON l.isbn = j.value

WHERE d.papel = 'ASSISTENTE'

ON CONFLICT (mensagem_chat_id, livro_id) DO NOTHING;

INSERT INTO feedback_resposta (
    mensagem_chat_id, util, criada_em
)
SELECT
    m.id,
    d.util,
    strftime(
        '%Y-%m-%dT%H:%M:%S',
        d.criada_em,
        '+10 seconds'
    )

FROM demo_chat_mensagens d

JOIN conversa cv ON cv.cliente_id = d.cliente_id
                AND cv.iniciada_em = '2026-10-15T10:00:00'

JOIN mensagem_chat m ON m.conversa_id = cv.id
                   AND m.papel = d.papel
                   AND m.criada_em = d.criada_em

WHERE d.papel = 'ASSISTENTE'
  AND d.util IS NOT NULL

ON CONFLICT (mensagem_chat_id) DO NOTHING;

DROP TABLE demo_chat_mensagens;

-- Modelo, coeficientes e vieses ficam vazios
-- até a execução de um treinamento real.