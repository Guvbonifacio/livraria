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
