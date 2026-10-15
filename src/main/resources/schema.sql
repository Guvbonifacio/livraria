CREATE TABLE IF NOT EXISTS cliente (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nome            TEXT NOT NULL,
    genero          TEXT NOT NULL,
    data_nascimento TEXT NOT NULL,
    cpf             TEXT NOT NULL UNIQUE,
    telefone        TEXT NOT NULL,
    email           TEXT NOT NULL UNIQUE,
    senha_hash      TEXT NOT NULL,
    ativo           INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))
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