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
-- Catálogo de demonstração ampliado para 15 livros.
-- Os três livros acima são preservados: dez testes Cypress os localizam
-- pelo título, e Duna segue sem lote de propósito, para demonstrar
-- indisponibilidade.
-- ISBN e código de barras são valores de demonstração, não verificados.
-- =====================================================================

INSERT INTO categoria (nome)
VALUES
    ('Romance'),
    ('Suspense'),
    ('História'),
    ('Tecnologia')
ON CONFLICT (nome) DO NOTHING;

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
    'Dan Brown',
    2017,
    'O Código Da Vinci',
    'Editora Demonstração',
    '1ª',
    '9788580419971',
    464,
    'Um simbologista investiga um assassinato no Louvre e uma sociedade secreta.',
    230,
    160,
    30,
    560,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788580419971',
    4690,
    '/img/logo.png'
),
(
    'Thomas Harris',
    2016,
    'O Silêncio dos Inocentes',
    'Editora Demonstração',
    '1ª',
    '9788501401229',
    384,
    'Uma agente do FBI recorre a um psiquiatra preso para encontrar um assassino.',
    210,
    140,
    25,
    440,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788501401229',
    4290,
    '/img/logo.png'
),
(
    'Yuval Noah Harari',
    2020,
    'Sapiens: Uma Breve História da Humanidade',
    'Editora Demonstração',
    '1ª',
    '9788525432186',
    464,
    'Da revolução cognitiva à revolução científica, a trajetória do Homo sapiens.',
    230,
    160,
    30,
    620,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788525432186',
    6490,
    '/img/logo.png'
),
(
    'Laurentino Gomes',
    2019,
    '1808',
    'Editora Demonstração',
    '1ª',
    '9788525061232',
    416,
    'A fuga da corte portuguesa para o Brasil e o que ela mudou no país.',
    230,
    160,
    28,
    580,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788525061232',
    5990,
    '/img/logo.png'
),
(
    'Robert C. Martin',
    2018,
    'Código Limpo: Habilidades Práticas do Agile Software',
    'Editora Demonstração',
    '1ª',
    '9788576082675',
    432,
    'Princípios e práticas para escrever código legível e sustentável.',
    240,
    170,
    30,
    700,
    (SELECT id FROM grupo_precificacao WHERE nome = 'Padrão'),
    '9788576082675',
    13490,
    '/img/logo.png'
)
ON CONFLICT (isbn) DO NOTHING;

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
        l.isbn = '9788535910666'
        AND c.nome IN ('Literatura Clássica')
    )
    OR
    (
        l.isbn = '9788544001820'
        AND c.nome IN ('Romance', 'Literatura Clássica')
    )
    OR
    (
        l.isbn = '9788580419971'
        AND c.nome IN ('Suspense')
    )
    OR
    (
        l.isbn = '9788501401229'
        AND c.nome IN ('Suspense')
    )
    OR
    (
        l.isbn = '9788525432186'
        AND c.nome IN ('História')
    )
    OR
    (
        l.isbn = '9788525061232'
        AND c.nome IN ('História')
    )
    OR
    (
        l.isbn = '9788576082675'
        AND c.nome IN ('Tecnologia')
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
    2700,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788580419971'
FROM livro
WHERE isbn = '9788580419971'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    2500,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788501401229'
FROM livro
WHERE isbn = '9788501401229'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    3800,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788525432186'
FROM livro
WHERE isbn = '9788525432186'
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
    'DEMO-9788525061232'
FROM livro
WHERE isbn = '9788525061232'
ON CONFLICT (referencia_entrada) DO NOTHING;

INSERT INTO item_estoque (
    livro_id, quantidade, valor_custo_centavos,
    fornecedor, data_entrada, referencia_entrada
)
SELECT
    id,
    10,
    7900,
    'Distribuidora Demonstração',
    '2026-10-01',
    'DEMO-9788576082675'
FROM livro
WHERE isbn = '9788576082675'
ON CONFLICT (referencia_entrada) DO NOTHING;
