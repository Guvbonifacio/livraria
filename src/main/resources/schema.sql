CREATE TABLE IF NOT EXISTS cliente (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nome            TEXT NOT NULL,
    genero          TEXT NOT NULL,
    data_nascimento TEXT NOT NULL,
    cpf             TEXT NOT NULL UNIQUE,
    telefone        TEXT NOT NULL,
    email           TEXT NOT NULL UNIQUE,
    senha_hash      TEXT NOT NULL,
    ativo           INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
    perfil          TEXT NOT NULL DEFAULT 'CLIENTE' CHECK (perfil IN ('CLIENTE', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS endereco (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id      INTEGER NOT NULL REFERENCES cliente(id),
    nome_curto      TEXT NOT NULL,
    tipo            TEXT NOT NULL CHECK (tipo IN ('ENTREGA', 'COBRANCA')),
    tipo_residencia TEXT NOT NULL,
    tipo_logradouro TEXT NOT NULL CHECK (length(trim(tipo_logradouro)) > 0),
    logradouro      TEXT NOT NULL,
    numero          TEXT NOT NULL,
    bairro          TEXT NOT NULL,
    cep             TEXT NOT NULL,
    cidade          TEXT NOT NULL,
    estado          TEXT NOT NULL,
    pais            TEXT NOT NULL CHECK (length(trim(pais)) > 0),
    observacoes     TEXT
);

-- RN0025: domínio das bandeiras registradas no sistema.
CREATE TABLE IF NOT EXISTS bandeira (
    nome TEXT PRIMARY KEY NOT NULL CHECK (length(trim(nome)) > 0)
);

-- RNF0013: carga inicial deste domínio sem duplicar registros.
INSERT INTO bandeira (nome)
VALUES ('Visa'), ('Mastercard'), ('Elo')
ON CONFLICT (nome) DO NOTHING;

CREATE TABLE IF NOT EXISTS cartao_credito (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id       INTEGER NOT NULL REFERENCES cliente(id),
    numero_mascarado TEXT NOT NULL,
    nome_impresso    TEXT NOT NULL,
    bandeira         TEXT NOT NULL REFERENCES bandeira(nome),
    preferencial     INTEGER NOT NULL DEFAULT 0 CHECK (preferencial IN (0, 1))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_cartao_preferencial_cliente
ON cartao_credito(cliente_id) WHERE preferencial = 1;

CREATE TABLE IF NOT EXISTS log_transacao (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    data_hora   TEXT NOT NULL,
    usuario     TEXT NOT NULL,
    operacao    TEXT NOT NULL CHECK (operacao IN ('INSERT', 'UPDATE', 'DELETE')),
    tabela      TEXT NOT NULL,
    registro_id INTEGER NOT NULL,
    dados       TEXT NOT NULL
);

-- GRUPO DE PRECIFICAÇÃO
CREATE TABLE IF NOT EXISTS grupo_precificacao (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL UNIQUE,
    margem_percentual INTEGER NOT NULL
        CHECK (margem_percentual >= 0)
);

-- Grupo usado pela carga de demonstração de livros.
INSERT INTO grupo_precificacao (nome, margem_percentual)
VALUES ('Padrão', 0)
ON CONFLICT (nome) DO NOTHING;

-- CATEGORIAS
CREATE TABLE IF NOT EXISTS categoria (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL UNIQUE
);

-- LIVROS: campos da RN0011
CREATE TABLE IF NOT EXISTS livro (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    autor TEXT NOT NULL CHECK (length(trim(autor)) > 0),
    ano INTEGER NOT NULL CHECK (ano > 0),
    titulo TEXT NOT NULL CHECK (length(trim(titulo)) > 0),
    editora TEXT NOT NULL,
    edicao TEXT NOT NULL,
    isbn TEXT NOT NULL UNIQUE,
    paginas INTEGER NOT NULL CHECK (paginas > 0),
    sinopse TEXT NOT NULL,

    altura_mm INTEGER NOT NULL CHECK (altura_mm > 0),
    largura_mm INTEGER NOT NULL CHECK (largura_mm > 0),
    profundidade_mm INTEGER NOT NULL CHECK (profundidade_mm > 0),
    peso_g INTEGER NOT NULL CHECK (peso_g > 0),

    grupo_precificacao_id INTEGER NOT NULL
        REFERENCES grupo_precificacao(id),

    codigo_barras TEXT NOT NULL UNIQUE,
    preco_centavos INTEGER NOT NULL CHECK (preco_centavos > 0),
    imagem_url TEXT,

    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))
);

-- RN0012: um livro pode pertencer a várias categorias.
CREATE TABLE IF NOT EXISTS livro_categoria (
    livro_id INTEGER NOT NULL REFERENCES livro(id),
    categoria_id INTEGER NOT NULL REFERENCES categoria(id),

    PRIMARY KEY (livro_id, categoria_id)
);

-- Cada registro representa um lote.
-- quantidade = saldo atual desse lote.
-- disponibilidade do livro = SUM(quantidade).
CREATE TABLE IF NOT EXISTS item_estoque (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    livro_id INTEGER NOT NULL REFERENCES livro(id),

    quantidade INTEGER NOT NULL CHECK (quantidade >= 0),

    valor_custo_centavos INTEGER NOT NULL
        CHECK (valor_custo_centavos > 0),

    fornecedor TEXT NOT NULL,
    data_entrada TEXT NOT NULL,

    -- Identifica a entrada e evita duplicar a carga de demonstração.
    referencia_entrada TEXT NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS ix_item_estoque_livro
ON item_estoque(livro_id);

-- PEDIDOS
CREATE TABLE IF NOT EXISTS pedido (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id INTEGER NOT NULL REFERENCES cliente(id),
    data_pedido TEXT NOT NULL,

    status TEXT NOT NULL DEFAULT 'EM PROCESSAMENTO'
        CHECK (
            status IN (
                'EM PROCESSAMENTO',
                'APROVADA',
                'REPROVADA',
                'EM TRANSPORTE',
                'ENTREGUE',
                'EM TROCA',
                'TROCA AUTORIZADA',
                'TROCADO'
            )
        ),

    -- Cópia do endereço no momento da compra.
    endereco_entrega_json TEXT NOT NULL,

    subtotal_centavos INTEGER NOT NULL
        CHECK (subtotal_centavos >= 0),

    frete_centavos INTEGER NOT NULL
        CHECK (frete_centavos >= 0),

    desconto_centavos INTEGER NOT NULL DEFAULT 0
        CHECK (desconto_centavos >= 0),

    total_centavos INTEGER NOT NULL
        CHECK (total_centavos >= 0),

    CHECK (desconto_centavos <= subtotal_centavos + frete_centavos),
    CHECK (total_centavos = subtotal_centavos + frete_centavos - desconto_centavos)
);

CREATE INDEX IF NOT EXISTS ix_pedido_cliente
ON pedido(cliente_id);

CREATE INDEX IF NOT EXISTS ix_pedido_status_data
ON pedido(status, data_pedido);

CREATE TABLE IF NOT EXISTS item_pedido (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id INTEGER NOT NULL REFERENCES pedido(id),
    livro_id INTEGER NOT NULL REFERENCES livro(id),

    -- Cópias dos dados comerciais no momento da compra.
    titulo TEXT NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),

    preco_unitario_centavos INTEGER NOT NULL
        CHECK (preco_unitario_centavos > 0),

    UNIQUE (pedido_id, livro_id)
);

-- CUPONS
CREATE TABLE IF NOT EXISTS cupom (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,

    tipo TEXT NOT NULL
        CHECK (tipo IN ('PROMOCIONAL', 'TROCA')),

    cliente_id INTEGER REFERENCES cliente(id),
    pedido_origem_id INTEGER REFERENCES pedido(id),

    valor_centavos INTEGER NOT NULL CHECK (valor_centavos > 0),
    validade TEXT NOT NULL,

    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),

    -- Cupom de troca pertence a um cliente.
    CHECK (tipo <> 'TROCA' OR cliente_id IS NOT NULL)
);

-- Um pedido pode ter vários pagamentos.
CREATE TABLE IF NOT EXISTS pagamento (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id INTEGER NOT NULL REFERENCES pedido(id),

    forma TEXT NOT NULL
        CHECK (forma IN ('CARTAO', 'CUPOM_PROMOCIONAL', 'CUPOM_TROCA')),

    valor_centavos INTEGER NOT NULL CHECK (valor_centavos > 0),

    status TEXT NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'ESTORNADO')),

    cartao_id INTEGER REFERENCES cartao_credito(id)
        ON DELETE SET NULL,

    -- Dados históricos, mesmo se o cartão for removido do cadastro.
    numero_mascarado TEXT,
    bandeira TEXT,

    cupom_id INTEGER REFERENCES cupom(id),

    CHECK (
        (
            forma = 'CARTAO'
            AND cupom_id IS NULL
            AND numero_mascarado IS NOT NULL
            AND bandeira IS NOT NULL
        )
        OR
        (
            forma IN ('CUPOM_PROMOCIONAL', 'CUPOM_TROCA')
            AND cupom_id IS NOT NULL
            AND cartao_id IS NULL
            AND numero_mascarado IS NULL
            AND bandeira IS NULL
        )
    ),

    UNIQUE (pedido_id, cupom_id)
);

-- RN0033: apenas um cupom promocional por pedido.
CREATE UNIQUE INDEX IF NOT EXISTS ux_pagamento_promocional_pedido
ON pagamento(pedido_id)
WHERE forma = 'CUPOM_PROMOCIONAL';

CREATE INDEX IF NOT EXISTS ix_item_pedido_pedido
ON item_pedido(pedido_id);

CREATE INDEX IF NOT EXISTS ix_pagamento_pedido
ON pagamento(pedido_id);

CREATE TABLE IF NOT EXISTS troca (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id INTEGER NOT NULL REFERENCES pedido(id),

    status TEXT NOT NULL DEFAULT 'EM TROCA'
        CHECK (status IN ('EM TROCA', 'TROCA AUTORIZADA', 'TROCADO')),

    motivo TEXT NOT NULL,
    data_solicitacao TEXT NOT NULL,
    data_autorizacao TEXT,
    data_recebimento TEXT,

    -- NULL: decisão ainda não tomada; 0: não; 1: sim.
    retorna_estoque INTEGER
        CHECK (retorna_estoque IN (0, 1)),

    cupom_id INTEGER REFERENCES cupom(id)
);

CREATE INDEX IF NOT EXISTS ix_troca_pedido
ON troca(pedido_id);

CREATE TABLE IF NOT EXISTS item_troca (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    troca_id INTEGER NOT NULL REFERENCES troca(id),
    item_pedido_id INTEGER NOT NULL REFERENCES item_pedido(id),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),

    UNIQUE (troca_id, item_pedido_id)
);

CREATE INDEX IF NOT EXISTS ix_item_troca_item_pedido
ON item_troca(item_pedido_id);

CREATE TABLE IF NOT EXISTS reserva_estoque (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    livro_id INTEGER NOT NULL REFERENCES livro(id),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),

    sessao_id TEXT NOT NULL
        CHECK (length(trim(sessao_id)) > 0),

    -- Instante ISO UTC, exemplo: 2026-10-26T15:30:00Z.
    expira_em TEXT NOT NULL,

    cliente_id INTEGER REFERENCES cliente(id),

    aviso_enviado INTEGER NOT NULL DEFAULT 0
        CHECK (aviso_enviado IN (0, 1)),

    expirado INTEGER NOT NULL DEFAULT 0
        CHECK (expirado IN (0, 1)),

    UNIQUE (sessao_id, livro_id)
);

CREATE INDEX IF NOT EXISTS ix_reserva_livro_expira
ON reserva_estoque(livro_id, expira_em);

CREATE TABLE IF NOT EXISTS notificacao (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id INTEGER NOT NULL REFERENCES cliente(id),
    mensagem TEXT NOT NULL,
    data_hora TEXT NOT NULL,
    lida INTEGER NOT NULL DEFAULT 0 CHECK (lida IN (0, 1))
);

CREATE INDEX IF NOT EXISTS ix_notificacao_cliente
ON notificacao(cliente_id, lida, data_hora);

-- =========================================================
-- CHAT E RECOMENDAÇÃO
-- =========================================================

-- Estado atual das preferências.
CREATE TABLE IF NOT EXISTS preferencia_cliente (
    cliente_id INTEGER NOT NULL REFERENCES cliente(id),
    categoria_id INTEGER NOT NULL REFERENCES categoria(id),
    peso_declarado REAL NOT NULL,
    atualizada_em TEXT NOT NULL,

    UNIQUE (cliente_id, categoria_id)
);

-- Histórico usado para reconstruir preferências antes do corte.
-- Peso zero representa a remoção de uma preferência.
CREATE TABLE IF NOT EXISTS historico_preferencia_cliente (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id INTEGER NOT NULL REFERENCES cliente(id),
    categoria_id INTEGER NOT NULL REFERENCES categoria(id),
    peso_declarado REAL NOT NULL,
    registrada_em TEXT NOT NULL,

    UNIQUE (cliente_id, categoria_id, registrada_em)
);

CREATE INDEX IF NOT EXISTS ix_historico_preferencia_corte
ON historico_preferencia_cliente(
    cliente_id,
    categoria_id,
    registrada_em
);

CREATE TABLE IF NOT EXISTS conversa (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id INTEGER NOT NULL REFERENCES cliente(id),
    iniciada_em TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_conversa_cliente
ON conversa(cliente_id, iniciada_em);

CREATE TABLE IF NOT EXISTS mensagem_chat (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conversa_id INTEGER NOT NULL REFERENCES conversa(id),

    papel TEXT NOT NULL
        CHECK (papel IN ('CLIENTE', 'ASSISTENTE')),

    texto TEXT NOT NULL,

    tipo TEXT NOT NULL DEFAULT 'TEXTO'
        CHECK (tipo IN ('TEXTO', 'PROTOCOLO')),

    conteudo_json TEXT,
    provedor TEXT,
    criada_em TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_mensagem_chat_conversa
ON mensagem_chat(conversa_id, criada_em, id);

-- Identifica os livros efetivamente recomendados na mensagem.
CREATE TABLE IF NOT EXISTS recomendacao_mensagem (
    mensagem_chat_id INTEGER NOT NULL
        REFERENCES mensagem_chat(id),

    livro_id INTEGER NOT NULL REFERENCES livro(id),

    PRIMARY KEY (mensagem_chat_id, livro_id)
);

CREATE TABLE IF NOT EXISTS feedback_resposta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    mensagem_chat_id INTEGER NOT NULL
        REFERENCES mensagem_chat(id),

    util INTEGER NOT NULL CHECK (util IN (0, 1)),
    criada_em TEXT NOT NULL,

    UNIQUE (mensagem_chat_id)
);

CREATE TABLE IF NOT EXISTS modelo_recomendacao (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    versao TEXT NOT NULL UNIQUE,
    treinado_em TEXT NOT NULL,

    data_corte TEXT NOT NULL,
    fim_observacao TEXT NOT NULL,

    amostras INTEGER NOT NULL,
    iteracoes INTEGER NOT NULL,
    taxa_aprendizado REAL NOT NULL,

    perda_inicial REAL NOT NULL,
    perda_final REAL NOT NULL,
    intercepto REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS coeficiente_modelo (
    modelo_id INTEGER NOT NULL
        REFERENCES modelo_recomendacao(id),

    feature TEXT NOT NULL,
    coeficiente REAL NOT NULL,
    media REAL NOT NULL,
    desvio REAL NOT NULL,

    PRIMARY KEY (modelo_id, feature)
);

CREATE TABLE IF NOT EXISTS vies_categoria (
    modelo_id INTEGER NOT NULL
        REFERENCES modelo_recomendacao(id),

    categoria_id INTEGER NOT NULL REFERENCES categoria(id),
    vies REAL NOT NULL,

    PRIMARY KEY (modelo_id, categoria_id)
);

-- =========================================================
-- MASSA DE DEMONSTRAÇÃO: CHAT E RECOMENDAÇÃO
-- Cliente A: 1
-- Administrador existente: 2
-- Cliente B: 3
-- =========================================================

-- Senha pública de demonstração: Demo@123.
-- Armazenada como hash BCrypt.
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

-- Garante um endereço para cada cliente de demonstração.
INSERT INTO endereco (
    cliente_id,
    nome_curto,
    tipo,
    tipo_residencia,
    tipo_logradouro,
    logradouro,
    numero,
    bairro,
    cep,
    cidade,
    estado,
    pais,
    observacoes
)
SELECT
    c.id,
    'Casa',
    'ENTREGA',
    'Casa',
    'Rua',
    'das Flores',
    '100',
    'Centro',
    '08700000',
    'Mogi das Cruzes',
    'SP',
    'Brasil',
    NULL
FROM cliente c
WHERE c.id IN (1, 3)
  AND NOT EXISTS (
      SELECT 1
      FROM endereco e
      WHERE e.cliente_id = c.id
  );

-- Somente dados mascarados; nenhum CVV é persistido.
INSERT INTO cartao_credito (
    cliente_id,
    numero_mascarado,
    nome_impresso,
    bandeira,
    preferencial
)
SELECT
    c.id,
    '**** **** **** 1111',
    c.nome,
    'Visa',
    1
FROM cliente c
WHERE c.id IN (1, 3)
  AND NOT EXISTS (
      SELECT 1
      FROM cartao_credito cc
      WHERE cc.cliente_id = c.id
  );

-- Completa o catálogo de Autoajuda.
-- Se o ISBN já existir, preserva o livro cadastrado.
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
VALUES (
    'Charles Duhigg',
    2012,
    'O Poder do Hábito',
    'Editora Demonstração',
    '1ª',
    '9788539004119',
    408,
    'Uma investigação sobre a formação dos hábitos.',
    210,
    140,
    25,
    500,
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

-- Tabela temporária usada para montar a carga.
CREATE TEMP TABLE demo_chat_itens (
    cliente_id INTEGER NOT NULL,
    isbn TEXT NOT NULL
);

INSERT INTO demo_chat_itens (cliente_id, isbn)
VALUES
    (1, '9788595084742'), -- O Hobbit: Fantasia.
    (1, '9788576573135'), -- Duna: Ficção Científica e Fantasia.
    (3, '9788539004119'), -- O Poder do Hábito: Autoajuda.
    (3, '9788535914849'); -- Dom Casmurro: Literatura Clássica.

-- Um pedido histórico ENTREGUE para cada cliente.
-- Quantidade: uma unidade de cada livro.
-- Frete e desconto zerados nesta massa.
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
SELECT
    d.cliente_id,
    '2026-11-01T10:00:00',
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
FROM demo_chat_itens d
JOIN livro l ON l.isbn = d.isbn
WHERE NOT EXISTS (
    SELECT 1
    FROM pedido p
    WHERE p.cliente_id = d.cliente_id
      AND p.data_pedido = '2026-11-01T10:00:00'
)
GROUP BY d.cliente_id;

INSERT INTO item_pedido (
    pedido_id,
    livro_id,
    titulo,
    quantidade,
    preco_unitario_centavos
)
SELECT
    p.id,
    l.id,
    l.titulo,
    1,
    l.preco_centavos
FROM demo_chat_itens d
JOIN livro l ON l.isbn = d.isbn
JOIN pedido p ON p.cliente_id = d.cliente_id
             AND p.data_pedido = '2026-11-01T10:00:00'
ON CONFLICT (pedido_id, livro_id) DO NOTHING;

-- Pagamento aprovado acompanha os pedidos históricos entregues.
INSERT INTO pagamento (
    pedido_id,
    forma,
    valor_centavos,
    status,
    cartao_id,
    numero_mascarado,
    bandeira
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
  AND p.data_pedido = '2026-11-01T10:00:00'
  AND NOT EXISTS (
      SELECT 1
      FROM pagamento pg
      WHERE pg.pedido_id = p.id
  );

DROP TABLE demo_chat_itens;

-- Somente A possui preferências declaradas.
-- Autoajuda não aparece nas compras de A.
-- Peso 1.0 é um dado ilustrativo da carga.
INSERT INTO preferencia_cliente (
    cliente_id,
    categoria_id,
    peso_declarado,
    atualizada_em
)
SELECT
    1,
    id,
    1.0,
    '2026-11-01T10:00:00'
FROM categoria
WHERE nome IN ('Fantasia', 'Autoajuda')
ON CONFLICT (cliente_id, categoria_id) DO NOTHING;

-- Uma conversa de demonstração para cada cliente.
INSERT INTO conversa (cliente_id, iniciada_em)
SELECT
    c.id,
    '2026-11-02T10:00:00'
FROM cliente c
WHERE c.id IN (1, 3)
  AND NOT EXISTS (
      SELECT 1
      FROM conversa cv
      WHERE cv.cliente_id = c.id
        AND cv.iniciada_em = '2026-11-02T10:00:00'
  );

CREATE TEMP TABLE demo_chat_mensagens (
    cliente_id INTEGER NOT NULL,
    papel TEXT NOT NULL,
    texto TEXT NOT NULL,
    criada_em TEXT NOT NULL,
    util INTEGER
);

-- Textos sintéticos para demonstração.
INSERT INTO demo_chat_mensagens (
    cliente_id,
    papel,
    texto,
    criada_em,
    util
)
VALUES
(
    1,
    'CLIENTE',
    'Quero uma indicação de fantasia.',
    '2026-11-02T10:00:00',
    NULL
),
(
    1,
    'ASSISTENTE',
    'O Hobbit é uma opção de Fantasia do catálogo.',
    '2026-11-02T10:00:01',
    1
),
(
    1,
    'CLIENTE',
    'E uma indicação diferente?',
    '2026-11-02T10:01:00',
    NULL
),
(
    1,
    'ASSISTENTE',
    'Dom Casmurro é uma opção de Literatura Clássica.',
    '2026-11-02T10:01:01',
    0
),
(
    3,
    'CLIENTE',
    'Quero uma indicação de autoajuda.',
    '2026-11-02T10:00:00',
    NULL
),
(
    3,
    'ASSISTENTE',
    'O Poder do Hábito é uma opção de Autoajuda.',
    '2026-11-02T10:00:01',
    1
),
(
    3,
    'CLIENTE',
    'E uma indicação diferente?',
    '2026-11-02T10:01:00',
    NULL
),
(
    3,
    'ASSISTENTE',
    'Duna é uma opção de Ficção Científica e Fantasia.',
    '2026-11-02T10:01:01',
    0
);

INSERT INTO mensagem_chat (
    conversa_id,
    papel,
    texto,
    criada_em
)
SELECT
    cv.id,
    d.papel,
    d.texto,
    d.criada_em
FROM demo_chat_mensagens d
JOIN conversa cv ON cv.cliente_id = d.cliente_id
                AND cv.iniciada_em = '2026-11-02T10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM mensagem_chat m
    WHERE m.conversa_id = cv.id
      AND m.papel = d.papel
      AND m.criada_em = d.criada_em
);

-- Avaliações somente nas respostas do assistente.
INSERT INTO feedback_resposta (
    mensagem_chat_id,
    util,
    criada_em
)
SELECT
    m.id,
    d.util,
    strftime('%Y-%m-%dT%H:%M:%S', d.criada_em, '+10 seconds')
FROM demo_chat_mensagens d
JOIN conversa cv ON cv.cliente_id = d.cliente_id
                AND cv.iniciada_em = '2026-11-02T10:00:00'
JOIN mensagem_chat m ON m.conversa_id = cv.id
                   AND m.papel = d.papel
                   AND m.criada_em = d.criada_em
WHERE d.papel = 'ASSISTENTE'
  AND d.util IS NOT NULL
ON CONFLICT (mensagem_chat_id) DO NOTHING;

DROP TABLE demo_chat_mensagens;

CREATE TABLE IF NOT EXISTS mensagem_categoria (
    mensagem_chat_id INTEGER NOT NULL
        REFERENCES mensagem_chat(id),

    categoria_id INTEGER NOT NULL REFERENCES categoria(id),

    PRIMARY KEY (mensagem_chat_id, categoria_id)
);