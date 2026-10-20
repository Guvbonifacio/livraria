const { defineConfig } = require('cypress');
const { DatabaseSync } = require('node:sqlite');
const { readFileSync, existsSync } = require('node:fs');
const path = require('node:path');

module.exports = defineConfig({
  e2e: {
    // Padrao do projeto: ambiente de teste (porta 8081, banco e2e.db).
    // A porta 8080 com livraria.db e so para navegacao manual.
    baseUrl: 'http://localhost:8081',

    setupNodeEvents(on, config) {
      function banco() {
        if (config.baseUrl !== 'http://localhost:8081') {
          throw new Error(
            'Os testes de vendas exigem baseUrl=http://localhost:8081 ' +
            'e banco e2e.db. Suba a aplicacao com: npm run app:test'
          );
        }

        const arquivo = path.resolve(
          config.projectRoot,
          'e2e.db'
        );

        if (!existsSync(arquivo)) {
          throw new Error(
            'Banco e2e.db ausente. Suba a aplicacao com: npm run app:test'
          );
        }

        const db = new DatabaseSync(arquivo);

        db.exec(`
          PRAGMA foreign_keys = ON;
          PRAGMA busy_timeout = 5000;
        `);

        return db;
      }

      on('task', {
        'vendas:preparar'(tipo) {
          const tipos = [
            'padrao',
            'estoque',
            'multiplosCartoes',
            'cupomCartao',
            'excedente',
            'novos'
          ];

          if (!tipos.includes(tipo)) {
            throw new Error('Cenário desconhecido.');
          }

          const db = banco();

          try {
            db.exec('BEGIN IMMEDIATE');

            // Limpeza exclusiva do banco e2e.db.
            for (const tabela of [
              'item_troca',
              'troca',
              'notificacao',
              'reserva_estoque',
              'log_transacao',
              'pagamento',
              'item_pedido',
              'cupom',
              'pedido',
              'cartao_credito',
              'endereco',
              'cliente',
              'item_estoque',
              'livro_categoria',
              'livro',
              'categoria',
              'grupo_precificacao'
            ]) {
              db.exec(`DELETE FROM ${tabela}`);
            }

            db.exec(
              readFileSync(
                path.join(
                  config.projectRoot,
                  'src/main/resources/data.sql'
                ),
                'utf8'
              )
            );

            // Remove eventual cliente de demonstração do data.sql.
            for (const tabela of [
              'item_troca',
              'troca',
              'notificacao',
              'reserva_estoque',
              'log_transacao',
              'pagamento',
              'item_pedido',
              'cupom',
              'pedido',
              'cartao_credito',
              'endereco',
              'cliente'
            ]) {
              db.exec(`DELETE FROM ${tabela}`);
            }

            const n =
              `${Date.now()}${Math.floor(Math.random() * 10000)}`;

            const nome = `Cliente E2E ${n}`;

            db.prepare(`
              INSERT INTO cliente (
                id, nome, genero, data_nascimento, cpf,
                telefone, email, senha_hash, ativo
              )
              VALUES (
                1, ?, 'Outro', '2000-01-01', ?,
                '11999990000', ?, ?, 1
              )
            `).run(
              nome,
              n.slice(-11),
              `vendas${n}@teste.local`,
              '$2a$12$AHnOA4yS617WMgxFbyVEcO1cOwn6uGXPbUOzVXXRZ.x.dRww5uOXa'
            );

            const enderecoId = Number(
              db.prepare(`
                INSERT INTO endereco (
                  cliente_id, nome_curto, tipo, tipo_residencia,
                  tipo_logradouro, logradouro, numero, bairro,
                  cep, cidade, estado, pais
                )
                VALUES (
                  1, 'Casa E2E', 'ENTREGA', 'Casa',
                  'Rua', 'Existente E2E', '10', 'Centro',
                  '08700000', 'Mogi das Cruzes', 'SP', 'Brasil'
                )
              `).run().lastInsertRowid
            );

            const cartaoId = Number(
              db.prepare(`
                INSERT INTO cartao_credito (
                  cliente_id, numero_mascarado,
                  nome_impresso, bandeira, preferencial
                )
                VALUES (
                  1, '**** **** **** 1111',
                  'CLIENTE E2E', 'Visa', 1
                )
              `).run().lastInsertRowid
            );

            let segundoCartaoId = null;

            if (tipo === 'multiplosCartoes') {
              segundoCartaoId = Number(
                db.prepare(`
                  INSERT INTO cartao_credito (
                    cliente_id, numero_mascarado,
                    nome_impresso, bandeira, preferencial
                  )
                  VALUES (
                    1, '**** **** **** 4444',
                    'CLIENTE E2E', 'Mastercard', 0
                  )
                `).run().lastInsertRowid
              );
            }

            const livros = [
              '9788595084742',
              '9788535914849'
            ].map(isbn => {
              const livro = db.prepare(`
                SELECT
                  l.id,
                  l.titulo,
                  l.preco_centavos,
                  COALESCE(
                    (
                      SELECT SUM(quantidade)
                      FROM item_estoque
                      WHERE livro_id = l.id
                    ),
                    0
                  ) AS estoque
                FROM livro l
                WHERE isbn = ?
              `).get(isbn);

              if (!livro) {
                throw new Error(
                  `Livro necessário ausente no data.sql: ${isbn}`
                );
              }

              return { ...livro };
            });

            if (livros[0].preco_centavos !== 3000) {
              throw new Error(
                'O cenário pressupõe O Hobbit a R$ 30,00.'
              );
            }

            const cuponsIds = [];

            const valores =
              tipo === 'cupomCartao'
                ? [4200]
                : tipo === 'excedente'
                  ? [2000, 3500]
                  : [];

            for (let i = 0; i < valores.length; i++) {
              const resultado = db.prepare(`
                INSERT INTO cupom (
                  codigo, tipo, cliente_id,
                  valor_centavos, validade, ativo
                )
                VALUES (?, 'TROCA', 1, ?, '2099-12-31', 1)
              `).run(
                `E2E-${n}-${i}`,
                valores[i]
              );

              cuponsIds.push(Number(resultado.lastInsertRowid));
            }

            db.exec('COMMIT');

            return {
              nome,
              enderecoId,
              cartaoId,
              segundoCartaoId,
              livros,
              cuponsIds,
              totalCentavos: 4700,
              excedenteCentavos: 800
            };
          } catch (erro) {
            try {
              db.exec('ROLLBACK');
            } catch {}

            throw erro;
          } finally {
            db.close();
          }
        },

        'vendas:pedido'(id) {
          const db = banco();

          try {
            return {
              pedido: db.prepare(`
                SELECT *
                FROM pedido
                WHERE id = ? AND cliente_id = 1
              `).get(Number(id)) || null,

              itens: db.prepare(`
                SELECT *
                FROM item_pedido
                WHERE pedido_id = ?
                ORDER BY id
              `).all(Number(id)),

              pagamentos: db.prepare(`
                SELECT *
                FROM pagamento
                WHERE pedido_id = ?
                ORDER BY id
              `).all(Number(id)),

              emitidos: db.prepare(`
                SELECT *
                FROM cupom
                WHERE pedido_origem_id = ?
              `).all(Number(id)),

              cupons: db.prepare(`
                SELECT id, ativo
                FROM cupom
                WHERE cliente_id = 1
              `).all(),

              quantidadePedidos: db.prepare(`
                SELECT COUNT(*) AS n FROM pedido
              `).get().n
            };
          } finally {
            db.close();
          }
        },

        'vendas:perfil'() {
          const db = banco();

          try {
            return {
              nome: db.prepare(`
                SELECT nome FROM cliente WHERE id = 1
              `).get().nome,

              enderecos: db.prepare(`
                SELECT *
                FROM endereco
                WHERE cliente_id = 1
                ORDER BY id
              `).all(),

              cartoes: db.prepare(`
                SELECT *
                FROM cartao_credito
                WHERE cliente_id = 1
                ORDER BY id
              `).all()
            };
          } finally {
            db.close();
          }
        }
      });

      return config;
    }
  }
});