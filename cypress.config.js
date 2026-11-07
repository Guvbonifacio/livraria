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

      let massaAnalise = null;
    
      on('task', {
        'reserva:estado'() {
          const db = banco();

          try {
            return {
              reservas: db.prepare(`
                SELECT *
                FROM reserva_estoque
                ORDER BY id
              `).all(),

              notificacoes: db.prepare(`
                SELECT *
                FROM notificacao
                ORDER BY id
              `).all(),

              pedidos: db.prepare(`
                SELECT COUNT(*) AS n
                FROM pedido
              `).get().n
            };

          } finally {
            db.close();
          }
        },

        'reserva:tempo'(segundos) {
          if (![300, -1].includes(segundos)) {
            throw new Error('Tempo de teste inválido.');
          }

          const db = banco();

          try {
            db.prepare(`
              UPDATE reserva_estoque
              SET expira_em = ?
              WHERE cliente_id = 1
                AND expirado = 0
                AND sessao_id NOT LIKE 'pedido:%'
            `).run(
              new Date(Date.now() + segundos * 1000).toISOString()
            );

            return null;

          } finally {
            db.close();
          }
        },

        async 'reserva:aguardar'(estado) {
          if (!['aviso', 'expiracao'].includes(estado)) {
            throw new Error('Estado inválido.');
          }

          const limite = Date.now() + 10000;

          while (Date.now() < limite) {
            const db = banco();

            try {
              const linhas = db.prepare(`
                SELECT *
                FROM reserva_estoque
                WHERE cliente_id = 1
                  AND sessao_id NOT LIKE 'pedido:%'
              `).all();

              const concluido = linhas.length > 0 &&
                linhas.every(r =>
                  estado === 'aviso'
                    ? r.aviso_enviado === 1
                    : r.expirado === 1
                );

              if (concluido) return null;

            } finally {
              db.close();
            }

            await new Promise(resolve => setTimeout(resolve, 100));
          }

          throw new Error(
            'O agendador não confirmou ' + estado + ' em dez segundos.'
          );
        },

        'vendas:alterarEstoqueRN0032'({ livroId, quantidade }) {
          if (!Number.isSafeInteger(livroId) || livroId <= 0
              || !Number.isSafeInteger(quantidade) || quantidade < 0) {
            throw new Error('Livro ou quantidade inválidos.');
          }
          const db = banco();
          try {
            db.exec('BEGIN IMMEDIATE');
            const entrada = db.prepare(
              'SELECT MIN(id) AS id FROM item_estoque WHERE livro_id = ?'
            ).get(livroId);
            if (!entrada?.id) throw new Error('Livro sem entrada de estoque.');
            db.prepare('UPDATE item_estoque SET quantidade = 0 WHERE livro_id = ?')
              .run(livroId);
            db.prepare('UPDATE item_estoque SET quantidade = ? WHERE id = ?')
              .run(quantidade, entrada.id);
            db.exec('COMMIT');
            return null;
          } catch (erro) {
            try { db.exec('ROLLBACK'); } catch {}
            throw erro;
          } finally {
            db.close();
          }
        },

        'vendas:estadoRN0032'() {
          const db = banco();
          try {
            return {
              pedidos: db.prepare('SELECT COUNT(*) AS total FROM pedido').get().total,
              pagamentos: db.prepare('SELECT COUNT(*) AS total FROM pagamento').get().total,
              notificacoes: db.prepare(
                'SELECT * FROM notificacao WHERE cliente_id = 1 ORDER BY id'
              ).all(),
              logs: db.prepare(
                "SELECT * FROM log_transacao WHERE tabela = 'notificacao' ORDER BY id"
              ).all()
            };
          } finally {
            db.close();
          }
        },

        'trocas:consultar'(id) {
          const db = banco();

          try {
            const pedidoId = Number(id);

            if (!Number.isSafeInteger(pedidoId) || pedidoId <= 0) {
              throw new Error('Identificador de pedido inválido.');
            }

            const pedido = db.prepare(`
              SELECT id, cliente_id, status
              FROM pedido
              WHERE id = ? AND cliente_id = 1
            `).get(pedidoId);

            if (!pedido) {
              throw new Error('Pedido do cenário não encontrado.');
            }

            return {
              pedido,

              itens: db.prepare(`
                SELECT id, livro_id, titulo, quantidade
                FROM item_pedido
                WHERE pedido_id = ?
                ORDER BY id
              `).all(pedidoId),

              trocas: db.prepare(`
                SELECT id, pedido_id, status, data_autorizacao,
                       data_recebimento, retorna_estoque, cupom_id
                FROM troca
                WHERE pedido_id = ?
                ORDER BY id
              `).all(pedidoId),

              itensTroca: db.prepare(`
                SELECT it.*
                FROM item_troca it
                JOIN troca t ON t.id = it.troca_id
                WHERE t.pedido_id = ?
                ORDER BY it.id
              `).all(pedidoId),

              notificacoes: db.prepare(`
                SELECT id, cliente_id, mensagem, lida
                FROM notificacao
                WHERE cliente_id = ?
                ORDER BY id
              `).all(pedido.cliente_id),

              logsTroca: db.prepare(`
                SELECT operacao, registro_id, dados
                FROM log_transacao
                WHERE tabela = 'troca'
                  AND registro_id IN (
                    SELECT id FROM troca WHERE pedido_id = ?
                  )
                ORDER BY id
              `).all(pedidoId),

              logsItensTroca: db.prepare(`
                SELECT operacao, registro_id, dados
                FROM log_transacao
                WHERE tabela = 'item_troca'
                  AND registro_id IN (
                    SELECT it.id
                    FROM item_troca it
                    JOIN troca t ON t.id = it.troca_id
                    WHERE t.pedido_id = ?
                  )
                ORDER BY id
              `).all(pedidoId),

              logsNotificacao: db.prepare(`
                SELECT operacao, registro_id, dados
                FROM log_transacao
                WHERE tabela = 'notificacao'
                  AND registro_id IN (
                    SELECT id FROM notificacao WHERE cliente_id = ?
                  )
                ORDER BY id
              `).all(pedido.cliente_id),

              estoque: db.prepare(`
                SELECT livro_id, SUM(quantidade) AS quantidade
                FROM item_estoque
                GROUP BY livro_id
                ORDER BY livro_id
              `).all(),

              cupons: db.prepare(`
                SELECT id, codigo, tipo, cliente_id, pedido_origem_id, validade, valor_centavos, ativo
                FROM cupom
                ORDER BY id
              `).all()
            };
          } finally {
            db.close();
          }
        },

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
              const promocional = tipo === 'cupomCartao';

              const resultado = db.prepare(`
                INSERT INTO cupom (
                  codigo, tipo, cliente_id,
                  valor_centavos, validade, ativo
                )
                VALUES (?, ?, ?, ?, '2099-12-31', 1)
              `).run(
                `E2E-${n}-${i}`,
                promocional ? 'PROMOCIONAL' : 'TROCA',
                promocional ? null : 1,
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

        // Estoque e reservas de um livro, para provar a baixa (RF0053 /
        // RN0028) e o desbloqueio das compras nao aprovadas.
        'vendas:estoque'(livroId) {
          const db = banco();

          try {
            return {
              total: db.prepare(`
                SELECT COALESCE(SUM(quantidade), 0) AS n
                FROM item_estoque
                WHERE livro_id = ?
              `).get(Number(livroId)).n,

              lotes: db.prepare(`
                SELECT id, quantidade
                FROM item_estoque
                WHERE livro_id = ?
                ORDER BY data_entrada, id
              `).all(Number(livroId)),

              reservas: db.prepare(`
                SELECT COUNT(*) AS n
                FROM reserva_estoque
                WHERE livro_id = ?
              `).get(Number(livroId)).n
            };
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

              // Cupom promocional nao pertence a cliente algum.
              promocionais: db.prepare(`
                SELECT id, ativo
                FROM cupom
                WHERE tipo = 'PROMOCIONAL'
                ORDER BY id
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
        },

        // Efeito das acoes administrativas sobre o cliente 1.
        // Nao ha coluna de perfil: a simulacao vive na sessao.
        'perfil:estado'() {
          const db = banco();

          try {
            return {
              cliente: db.prepare(`
                SELECT id, nome, ativo, perfil
                FROM cliente
                WHERE id = 1
              `).get() || null,

              logs: db.prepare(`
                SELECT usuario, operacao, tabela, registro_id
                FROM log_transacao
                WHERE tabela = 'cliente' AND registro_id = 1
                ORDER BY id
              `).all()
            };
          } finally {
            db.close();
          }
        },

        'vendas:entrega'(id) {
          const db = banco();

          try {
            const pedidoId = Number(id);

            if (!Number.isSafeInteger(pedidoId) || pedidoId <= 0) {
              throw new Error('Identificador de pedido inválido.');
            }

            return {
              pedido: db.prepare(`
                SELECT id, status
                FROM pedido
                WHERE id = ?
              `).get(pedidoId) || null,

              estoque: db.prepare(`
                SELECT livro_id, SUM(quantidade) AS quantidade
                FROM item_estoque
                GROUP BY livro_id
                ORDER BY livro_id
              `).all(),

              pagamentos: db.prepare(`
                SELECT id, status
                FROM pagamento
                WHERE pedido_id = ?
                ORDER BY id
              `).all(pedidoId),

              logs: db.prepare(`
                SELECT operacao, dados
                FROM log_transacao
                WHERE tabela = 'pedido'
                  AND registro_id = ?
                ORDER BY id
              `).all(pedidoId)
            };
          } finally {
            db.close();
          }
        },

        'analise:preparar'() {
          const db = banco();

          try {
            db.exec('BEGIN IMMEDIATE');

            // Substitui somente a massa do banco de testes.
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

            const categorias = [
              'Autoajuda',
              'Fantasia',
              'Ficção Científica',
              'Literatura Clássica'
            ];

            const livros = [
              {
                titulo: 'O Hobbit',
                autor: 'J. R. R. Tolkien',
                isbn: '9788595084742',
                preco: 3000,
                categorias: ['Fantasia']
              },
              {
                titulo: 'Dom Casmurro',
                autor: 'Machado de Assis',
                isbn: '9788535914849',
                preco: 4500,
                categorias: ['Literatura Clássica']
              },
              {
                titulo: 'Duna',
                autor: 'Frank Herbert',
                isbn: '9788576573135',
                preco: 6000,
                categorias: ['Ficção Científica', 'Fantasia']
              },
              {
                titulo: 'O Poder do Hábito',
                autor: 'Charles Duhigg',
                isbn: '9788539004119',
                preco: 4000,
                categorias: ['Autoajuda']
              }
            ];

            const status = [
              'EM PROCESSAMENTO',
              'APROVADA',
              'EM TRANSPORTE',
              'ENTREGUE',
              'REPROVADA'
            ];

            const pedidos = [];
            const meses = [];

            for (let i = 0; i < 14; i++) {
              const mes = new Date(Date.UTC(2025, 8 + i, 1))
                .toISOString()
                .slice(0, 7);

              meses.push(mes);

              livros.forEach((livro, indice) => {
                status.forEach((situacao, posicao) => {
                  const contabiliza = [
                    'APROVADA',
                    'EM TRANSPORTE',
                    'ENTREGUE'
                  ].includes(situacao);

                  // Fevereiro/2026: Autoajuda tem pedidos,
                  // mas somente nos status que não contam como venda.
                  if (
                    mes === '2026-02' &&
                    indice === 3 &&
                    contabiliza
                  ) {
                    return;
                  }

                  const dia = String(
                    5 + indice * 5 + posicao
                  ).padStart(2, '0');

                  let data = `${mes}-${dia}T12:00:00`;

                  // Exercita a inclusão das extremidades do filtro.
                  if (
                    i === 0 &&
                    indice === 0 &&
                    situacao === 'APROVADA'
                  ) {
                    data = '2025-09-01T00:00:00';
                  }

                  if (
                    i === 13 &&
                    indice === 3 &&
                    situacao === 'ENTREGUE'
                  ) {
                    data = '2026-10-31T23:59:59';
                  }

                  pedidos.push({
                    data,
                    status: situacao,
                    itens: [{
                      livro: indice,
                      quantidade: 1 + ((i + indice + posicao) % 3),

                      // Preço histórico diferente do preço atual.
                      preco: livro.preco + i * 137 + posicao * 11
                    }]
                  });
                });
              });
            }

            // Pedido com dois livros de categorias diferentes.
            // Evita que o total inteiro do pedido seja atribuído
            // indevidamente a cada categoria de seus itens.
            pedidos.push({
              data: '2026-01-22T12:00:00',
              status: 'APROVADA',
              itens: [
                { livro: 0, quantidade: 1, preco: 3210 },
                { livro: 1, quantidade: 2, preco: 4789 }
              ]
            });

            const hash =
              '$2a$10$q7QLFBZUiaHQySPZvuQjQu.jOR6tDgl4.rusggAkJcYDP1WCRUa6q';

            db.prepare(`
              INSERT INTO cliente (
                id, nome, genero, data_nascimento, cpf,
                telefone, email, senha_hash, ativo, perfil
              )
              VALUES (
                ?, ?, 'Outro', '1990-01-01', ?,
                '11999990000', ?, ?, 1, ?
              )
            `).run(
              1,
              'Cliente Análise E2E',
              '52998224725',
              'analise@teste.local',
              hash,
              'CLIENTE'
            );

            db.prepare(`
              INSERT INTO cliente (
                id, nome, genero, data_nascimento, cpf,
                telefone, email, senha_hash, ativo, perfil
              )
              VALUES (
                ?, ?, 'Outro', '1990-01-01', ?,
                '11999990002', ?, ?, 1, ?
              )
            `).run(
              2,
              'Administrador Análise E2E',
              '11144477735',
              'admin-analise@teste.local',
              hash,
              'ADMIN'
            );

            const endereco = {
              nomeCurto: 'Casa Análise',
              tipo: 'ENTREGA',
              tipoResidencia: 'Casa',
              tipoLogradouro: 'Rua',
              logradouro: 'das Flores',
              numero: '100',
              bairro: 'Centro',
              cep: '08700000',
              cidade: 'Mogi das Cruzes',
              estado: 'SP',
              pais: 'Brasil',
              observacoes: ''
            };

            const enderecoId = Number(
              db.prepare(`
                INSERT INTO endereco (
                  cliente_id, nome_curto, tipo, tipo_residencia,
                  tipo_logradouro, logradouro, numero, bairro,
                  cep, cidade, estado, pais
                )
                VALUES (
                  1, ?, 'ENTREGA', 'Casa',
                  'Rua', ?, '100', 'Centro',
                  '08700000', 'Mogi das Cruzes', 'SP', 'Brasil'
                )
              `).run(
                endereco.nomeCurto,
                endereco.logradouro
              ).lastInsertRowid
            );

            endereco.id = enderecoId;

            db.prepare(`
              INSERT INTO bandeira (nome)
              VALUES ('Visa')
              ON CONFLICT DO NOTHING
            `).run();

            const cartaoId = Number(
              db.prepare(`
                INSERT INTO cartao_credito (
                  cliente_id, numero_mascarado,
                  nome_impresso, bandeira, preferencial
                )
                VALUES (
                  1, '**** **** **** 1111',
                  'CLIENTE ANALISE', 'Visa', 1
                )
              `).run().lastInsertRowid
            );

            const grupoId = Number(
              db.prepare(`
                INSERT INTO grupo_precificacao (
                  nome, margem_percentual
                )
                VALUES ('Grupo Análise E2E', 100)
              `).run().lastInsertRowid
            );

            const categoriaIds = {};

            for (const nome of categorias) {
              categoriaIds[nome] = Number(
                db.prepare(`
                  INSERT INTO categoria (nome)
                  VALUES (?)
                `).run(nome).lastInsertRowid
              );
            }

            livros.forEach(livro => {
              livro.id = Number(
                db.prepare(`
                  INSERT INTO livro (
                    autor, ano, titulo, editora, edicao, isbn,
                    paginas, sinopse,
                    altura_mm, largura_mm, profundidade_mm,
                    peso_g, grupo_precificacao_id,
                    codigo_barras, preco_centavos,
                    imagem_url, ativo
                  )
                  VALUES (
                    ?, 2020, ?, 'Editora E2E', '1ª', ?,
                    200, 'Livro da massa de análise.',
                    210, 140, 20, 400, ?, ?, ?,
                    '/img/logo.png', 1
                  )
                `).run(
                  livro.autor,
                  livro.titulo,
                  livro.isbn,
                  grupoId,
                  livro.isbn,
                  livro.preco
                ).lastInsertRowid
              );

              for (const categoria of livro.categorias) {
                db.prepare(`
                  INSERT INTO livro_categoria (
                    livro_id, categoria_id
                  )
                  VALUES (?, ?)
                `).run(
                  livro.id,
                  categoriaIds[categoria]
                );
              }
            });

            for (const pedido of pedidos) {
              const subtotal = pedido.itens.reduce(
                (soma, item) =>
                  soma + item.quantidade * item.preco,
                0
              );

              // Frete e desconto zerados para não pressupor
              // outra decisão de cálculo nesta massa.
              pedido.id = Number(
                db.prepare(`
                  INSERT INTO pedido (
                    cliente_id, data_pedido, status,
                    endereco_entrega_json,
                    subtotal_centavos, frete_centavos,
                    desconto_centavos, total_centavos
                  )
                  VALUES (1, ?, ?, ?, ?, 0, 0, ?)
                `).run(
                  pedido.data,
                  pedido.status,
                  JSON.stringify(endereco),
                  subtotal,
                  subtotal
                ).lastInsertRowid
              );

              for (const item of pedido.itens) {
                const livro = livros[item.livro];

                db.prepare(`
                  INSERT INTO item_pedido (
                    pedido_id, livro_id, titulo,
                    quantidade, preco_unitario_centavos
                  )
                  VALUES (?, ?, ?, ?, ?)
                `).run(
                  pedido.id,
                  livro.id,
                  livro.titulo,
                  item.quantidade,
                  item.preco
                );
              }

              const situacaoPagamento =
                pedido.status === 'REPROVADA'
                  ? 'RECUSADO'
                  : pedido.status === 'EM PROCESSAMENTO'
                    ? 'PENDENTE'
                    : 'APROVADO';

              db.prepare(`
                INSERT INTO pagamento (
                  pedido_id, forma, valor_centavos,
                  status, cartao_id,
                  numero_mascarado, bandeira
                )
                VALUES (
                  ?, 'CARTAO', ?, ?, ?,
                  '**** **** **** 1111', 'Visa'
                )
              `).run(
                pedido.id,
                subtotal,
                situacaoPagamento,
                cartaoId
              );
            }

            livros.forEach((livro, indice) => {
              const vendidos = pedidos
                .filter(p => [
                  'APROVADA',
                  'EM TRANSPORTE',
                  'ENTREGUE'
                ].includes(p.status))
                .flatMap(p => p.itens)
                .filter(item => item.livro === indice)
                .reduce(
                  (soma, item) => soma + item.quantidade,
                  0
                );

              db.prepare(`
                INSERT INTO item_estoque (
                  livro_id, quantidade, valor_custo_centavos,
                  fornecedor, data_entrada, referencia_entrada
                )
                VALUES (
                  ?, ?, ?, 'Fornecedor Análise E2E',
                  '2025-08-01', ?
                )
              `).run(
                livro.id,
                1000 - vendidos,
                Math.floor(livro.preco / 2),
                `ANALISE-${livro.isbn}`
              );
            });

            db.exec('COMMIT');

            // Definição da massa usada pelo oráculo independente.
            massaAnalise = {
              livros,
              pedidos,
              categorias,
              meses
            };

            return {
              dataInicio: '2025-09-01',
              dataFim: '2026-10-31',
              categorias,
              categoriaIds,
              meses,

              quantidadePedidos: pedidos.length,

              quantidadeItens: pedidos.reduce(
                (soma, p) => soma + p.itens.length,
                0
              ),

              status,

              mesSemVendas: {
                mes: '2026-02',
                categoria: 'Autoajuda'
              },

              livroDuasCategorias: {
                id: livros[2].id,
                titulo: 'Duna',
                categorias: [...livros[2].categorias]
              },

              criterioCategorias: 'INTEGRAL_EM_CADA_CATEGORIA',

              filtrosTeste: {
                umMes: {
                  dataInicio: '2026-02-01',
                  dataFim: '2026-02-28'
                },
                vinteQuatroMeses: {
                  dataInicio: '2024-11-01',
                  dataFim: '2026-10-31'
                },
                vinteCincoMeses: {
                  dataInicio: '2024-10-01',
                  dataFim: '2026-10-31'
                }
              }
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

        'analise:pedidoControle'(statusInicial) {
          const db = banco();

          try {
            if (!massaAnalise) {
              throw new Error('Execute analise:preparar primeiro.');
            }

            if (!['REPROVADA', 'EM PROCESSAMENTO'].includes(statusInicial)) {
              throw new Error('Status de controle inválido.');
            }

            const pedido = massaAnalise.pedidos.find(p =>
              p.status === statusInicial
              && p.itens.length === 1
              && p.itens[0].livro === 0
            );

            if (!pedido) {
              throw new Error('Pedido de controle ausente.');
            }

            const registro = db.prepare(
              'SELECT status FROM pedido WHERE id = ? AND cliente_id = 1'
            ).get(pedido.id);

            if (!registro) {
              throw new Error('Prepare novamente a massa de análise.');
            }

            const item = pedido.itens[0];

            return {
              id: pedido.id,
              status: registro.status,
              mes: pedido.data.slice(0, 7),
              valorCentavos: item.quantidade * item.preco,
              categorias: [...massaAnalise.livros[item.livro].categorias]
            };

          } finally {
            db.close();
          }
        },

        'analise:totais'(filtro) {
          // Mantém a mesma trava de ambiente.
          const db = banco();
          db.close();

          if (!massaAnalise) {
            throw new Error(
              'Execute analise:preparar antes de analise:totais.'
            );
          }

          const {
            dataInicio = '2025-09-01',
            dataFim = '2026-10-31',
            categorias = massaAnalise.categorias,
            statusEsperados = []
          } = filtro ?? {};

          for (const valor of [dataInicio, dataFim]) {
            const data = new Date(`${valor}T00:00:00.000Z`);

            if (
              !/^\d{4}-\d{2}-\d{2}$/.test(valor) ||
              Number.isNaN(data.getTime()) ||
              data.toISOString().slice(0, 10) !== valor
            ) {
              throw new Error(
                'Informe datas válidas no formato YYYY-MM-DD.'
              );
            }
          }

          if (dataInicio > dataFim) {
            throw new Error('Intervalo de datas invertido.');
          }

          if (
            !Array.isArray(categorias) ||
            categorias.some(c =>
              !massaAnalise.categorias.includes(c)
            )
          ) {
            throw new Error(
              'Informe categorias existentes como uma lista de nomes.'
            );
          }

          if (!Array.isArray(statusEsperados)) {
            throw new Error('statusEsperados deve ser uma lista.');
          }

          const alteracoes = new Map();

          for (const alteracao of statusEsperados) {
            if (
              !alteracao
              || !Number.isSafeInteger(alteracao.pedidoId)
              || !massaAnalise.pedidos.some(p => p.id === alteracao.pedidoId)
              || ![
                'EM PROCESSAMENTO',
                'APROVADA',
                'EM TRANSPORTE',
                'ENTREGUE',
                'REPROVADA'
              ].includes(alteracao.status)
            ) {
              throw new Error('Pedido ou status esperado inválido.');
            }

            alteracoes.set(alteracao.pedidoId, alteracao.status);
          }

          const selecionadas = massaAnalise.categorias.filter(
            c => categorias.includes(c)
          );

          const meses = [];

          const cursor = new Date(
            `${dataInicio.slice(0, 7)}-01T00:00:00.000Z`
          );

          while (
            cursor.toISOString().slice(0, 7) <= dataFim.slice(0, 7)
          ) {
            meses.push(cursor.toISOString().slice(0, 7));
            cursor.setUTCMonth(cursor.getUTCMonth() + 1);
          }

          // Começa com zero inclusive nos meses fora da massa.
          const series = selecionadas.map(categoria => ({
            categoria,
            valoresCentavos: meses.map(() => 0)
          }));

          let totalItensSemDuplicacaoCentavos = 0;

          for (const pedido of massaAnalise.pedidos) {
            const dia = pedido.data.slice(0, 10);

            const statusEsperado =
              alteracoes.get(pedido.id) ?? pedido.status;

            if (
              dia < dataInicio
              || dia > dataFim
              || ![
                'APROVADA',
                'EM TRANSPORTE',
                'ENTREGUE'
              ].includes(statusEsperado)
            ) {
              continue;
            }

            const posicao = meses.indexOf(
              pedido.data.slice(0, 7)
            );

            for (const item of pedido.itens) {
              const categoriasLivro =
                massaAnalise.livros[item.livro].categorias;

              const valor = item.quantidade * item.preco;

              let selecionado = false;

              for (const serie of series) {
                if (categoriasLivro.includes(serie.categoria)) {
                  // Decisão 4: valor integral em CADA categoria.
                  serie.valoresCentavos[posicao] += valor;
                  selecionado = true;
                }
              }

              if (selecionado) {
                totalItensSemDuplicacaoCentavos += valor;
              }
            }
          }

          const nomesMeses = [
            'jan', 'fev', 'mar', 'abr',
            'mai', 'jun', 'jul', 'ago',
            'set', 'out', 'nov', 'dez'
          ];

          for (const serie of series) {
            serie.valores = serie.valoresCentavos.map(
              valor => valor / 100
            );

            serie.totalCentavos = serie.valoresCentavos.reduce(
              (soma, valor) => soma + valor,
              0
            );
          }

          return {
            dataInicio,
            dataFim,
            meses,

            etiquetas: meses.map(m =>
              `${nomesMeses[Number(m.slice(5)) - 1]}/${m.slice(0, 4)}`
            ),

            series,

            totalItensSemDuplicacaoCentavos,

            somaCategoriasCentavos: series.reduce(
              (soma, serie) => soma + serie.totalCentavos,
              0
            ),

            criterioCategorias: 'INTEGRAL_EM_CADA_CATEGORIA'
          };
        }
      });

      return config;
    }
  }
});