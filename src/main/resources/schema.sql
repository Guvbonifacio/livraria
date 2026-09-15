CREATE TABLE IF NOT EXISTS cliente (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,   -- RNF0035: código único
    nome            TEXT    NOT NULL,
    genero          TEXT    NOT NULL,
    data_nascimento TEXT    NOT NULL,
    cpf             TEXT    NOT NULL UNIQUE,
    telefone        TEXT    NOT NULL,
    email           TEXT    NOT NULL UNIQUE,
    senha_hash      TEXT    NOT NULL,                    -- RNF0033: nunca a senha em texto
    ativo           INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0,1))-- RF0023: 1 = ativo, 0 = inativo // impede que receba valores não binários
);

CREATE TABLE IF NOT EXISTS endereco (

    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id       INTEGER NOT NULL REFERENCES cliente(id), --references pega chave estrangeira
    nome_curto       TEXT    NOT NULL,                   -- RF0026
    tipo             TEXT    NOT NULL CHECK (tipo IN ('ENTREGA', 'COBRANCA')), -- RNF0034
    tipo_residencia  TEXT    NOT NULL,
    logradouro       TEXT    NOT NULL,
    numero           TEXT    NOT NULL,
    bairro           TEXT    NOT NULL,
    cep              TEXT    NOT NULL,
    cidade           TEXT    NOT NULL,
    estado           TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS cartao_credito (

    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id       INTEGER NOT NULL REFERENCES cliente(id),
    numero_mascarado TEXT    NOT NULL,
    nome_impresso    TEXT    NOT NULL,
    bandeira         TEXT    NOT NULL,
    preferencial     INTEGER NOT NULL DEFAULT 0 CHECK (preferencial IN (0,1))-- RF0027
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_cartao_preferencial_cliente
ON cartao_credito(cliente_id)
WHERE preferencial = 1; --Garante apenas um cartão preferencial.

CREATE TABLE IF NOT EXISTS log_transacao (

    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    data_hora     TEXT    NOT NULL,
    usuario       TEXT    NOT NULL,
    operacao      TEXT    NOT NULL CHECK (operacao IN ('INSERT', 'UPDATE', 'DELETE')),
    tabela        TEXT    NOT NULL,
    registro_id   INTEGER NOT NULL,
    dados         TEXT    NOT NULL                    -- RNF0012

);