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
