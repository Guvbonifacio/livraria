-- Não sobrescreve um cliente que já ocupe o ID 1.
INSERT INTO cliente (
    id,
    nome,
    genero,
    data_nascimento,
    cpf,
    telefone,
    email,
    senha_hash,
    ativo
)
VALUES (
    1,
    'Cliente Demonstração',
    'Outro',
    '1995-05-20',
    '52998224725',
    '11999990000',
    'demo@livraria.local',
    '$2a$12$AHnOA4yS617WMgxFbyVEcO1cOwn6uGXPbUOzVXXRZ.x.dRww5uOXa',
    1
)
ON CONFLICT (id) DO NOTHING;

-- Só associa o endereço à conta de demonstração.
-- Não duplica nem substitui endereços já cadastrados.
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
WHERE c.id = 1
  AND c.email = 'demo@livraria.local'
  AND NOT EXISTS (
      SELECT 1
      FROM endereco e
      WHERE e.cliente_id = c.id
  );

-- Apenas número mascarado; nenhum CVV é armazenado.
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
    'CLIENTE DEMONSTRACAO',
    'Visa',
    1
FROM cliente c
WHERE c.id = 1
  AND c.email = 'demo@livraria.local'
  AND NOT EXISTS (
      SELECT 1
      FROM cartao_credito cc
      WHERE cc.cliente_id = c.id
  );