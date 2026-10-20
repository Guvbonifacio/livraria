DELETE FROM item_troca;
DELETE FROM troca;
DELETE FROM notificacao;
DELETE FROM reserva_estoque;
DELETE FROM log_transacao;
DELETE FROM pagamento;
DELETE FROM item_pedido;
DELETE FROM cupom;
DELETE FROM pedido;
DELETE FROM cartao_credito;
DELETE FROM endereco;
DELETE FROM cliente;

INSERT INTO cliente (
    id, nome, genero, data_nascimento, cpf,
    telefone, email, senha_hash, ativo
)
VALUES (
    1, 'Cliente Teste', 'Outro', '2000-01-01',
    '52998224725', '11999990000',
    'checkout@teste.local',
    'hash-nao-usado-neste-teste', 1
);

INSERT INTO endereco (
    cliente_id, nome_curto, tipo, tipo_residencia,
    tipo_logradouro, logradouro, numero, bairro,
    cep, cidade, estado, pais
)
VALUES (
    1, 'Casa', 'ENTREGA', 'Casa',
    'Rua', 'Teste', '10', 'Centro',
    '08700000', 'Mogi das Cruzes', 'SP', 'Brasil'
);

INSERT INTO cartao_credito (
    cliente_id, numero_mascarado,
    nome_impresso, bandeira, preferencial
)
VALUES (
    1, '**** **** **** 1111',
    'Cliente Teste', 'Visa', 1
);

UPDATE item_estoque SET quantidade = 20;