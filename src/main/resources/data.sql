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