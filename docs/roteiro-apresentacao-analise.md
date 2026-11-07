# Roteiro — Apresentação da Análise de Vendas

Livraria Estante do Saber · Gustavo Vinícius Bonifácio · 09/11/2026, 19:00 · online

Entrega: **RF0055 a RF0058, RN0071 a RN0074, RNF0011 e RNF0043 a RNF0046.**
Demonstração ao vivo pelos **testes funcionais automatizados**.

---

## 1. Antes de entrar na chamada

- [ ] `git status` limpo e `git push` feito
- [ ] Apagar `e2e.db`
- [ ] Subir a aplicação no ambiente de testes: `npm run app:test`
- [ ] Conferir `Started LivrariaApplication` e abrir <http://localhost:8081>
- [ ] Abrir o Cypress: `npm run e2e` → E2E → Chrome
- [ ] Abrir o VSCode em `AnaliseVendasService.java` — é o arquivo da arguição
- [ ] Abrir `AnaliseVendasRepository.java` ao lado: a consulta é uma só
- [ ] Abrir este roteiro e o DRS no grupo "Análise"

> ⚠️ A porta 8081 é o padrão do `cypress.config.js`. Na 8080 a task de preparação
> recusa executar, de propósito: ela apaga todas as tabelas antes de montar a massa.

> ⚠️ Verificado em 07/11: com `e2e.db` recriado do zero, a suíte passa inteira —
> 43 specs, 56 testes. Se algo falhar, suspeite primeiro da porta.

---

## 2. Sequência da apresentação

1. Abrir a tabela da **seção 3** e dizer: cada arquivo de teste nomeia o requisito
   que prova.
2. Mostrar a **massa de dados** (seção 4) **antes** de rodar qualquer teste. É o
   ponto mais forte da entrega: a massa foi construída para que um cálculo errado
   não passe despercebido.
3. Rodar os **11 testes de `admin/analise/`**, narrando o que cada um prova.
4. Abrir a tela `/admin/analise`: 4 linhas, eixo X em mês/ano, eixo Y em reais,
   legenda embaixo, tooltip no ponto.
5. Rodar `rf0058-exportar-analise` e **abrir o CSV no Excel**, mostrando acento
   correto e vírgula decimal.
6. Rodar `rnf0011-tempo-resposta-analise`: cinco respostas HTTP medidas, todas
   abaixo de 1000 ms.
7. Abrir o `AnaliseVendasService` e percorrer a ordem: autorização → datas →
   RF0056 → RN0072 → categorias → RN0073 → soma → etiquetas.
8. **Declarar as divergências** da seção 6 antes que perguntem.

---

## 3. Cenário exigido → teste

| # | Cenário pedido no enunciado | Arquivo | Requisitos |
|---|---|---|---|
| 1 | Uma categoria em período válido | `rf0057-uma-categoria` | RF0055, RF0057 |
| 2 | Comparação de duas ou mais categorias | `rf0057-multiplas-categorias` | RF0057 |
| 3 | Gráfico de linhas com totais mensais em reais | `rnf0043-grafico-linhas-mensais` | RNF0043 a RNF0046, RN0071 |
| 4 | Alterar data inicial/final gera novo resultado | `rf0056-alterar-periodo` | RF0056 |
| 5 | Compra não entregue não é contabilizada | `rn0074-excluir-reprovada` | RN0074 |
| 6 | Compra que passa a `ENTREGUE` aparece na consulta seguinte | `rn0074-incluir-entregue` | RN0074, RF0039, RF0040 |
| 7 | Acesso permitido ao admin, negado a outro perfil | `rf0055-perfil-administrador` | RF0055 |
| 8 | Resposta em até 1 segundo | `rnf0011-tempo-tela` | RNF0011 |

**Quatro testes que o enunciado não pediu e o DRS exige:**

| Arquivo | Requisito | O que prova |
|---|---|---|
| `rf0056-fim-anterior` | RF0056 | Recusa data final anterior à inicial e preserva os campos preenchidos |
| `rn0072-intervalo-invalido` | RN0072 | Recusa intervalo menor que 1 mês e maior que 24 meses |
| `rn0073-mes-sem-vendas` | RN0073 | Fevereiro/2026 fica em zero entre dois meses com venda, sem quebrar a linha |
| `rf0058-exportar-analise` | RF0058 | Exporta os mesmos filtros e valores, com BOM e formato brasileiro |

Mais `rnf0011-tempo-resposta-analise`, que mede **cinco** respostas HTTP completas,
e `rf0023-perfil-administrador`, que prova que a simulação de perfil não altera o
perfil gravado no banco.

---

## 4. A massa de dados — o argumento mais forte

A task `analise:preparar` monta **14 meses** (set/2025 a out/2026), **4 categorias**,
4 livros e os **5 status** do DRS. Mas o que importa são as armadilhas deliberadas:
cada uma faz um erro comum de cálculo aparecer como falha.

| Armadilha na massa | O erro que ela pega |
|---|---|
| Preço do item **diferente** do preço atual do livro | Somar `livro.preco_centavos` em vez de `item_pedido.preco_unitario_centavos`. O valor histórico da venda é o que vale |
| Pedido de 22/01/2026 com **dois livros de categorias diferentes** | Atribuir o total do pedido inteiro a cada categoria dos seus itens |
| Pedidos nas **extremidades** do filtro: `2025-09-01T00:00:00` e `2026-10-31T23:59:59` | Perder o último dia por comparar `data_pedido <= dataFim`, sendo `data_pedido` data **e hora**. A consulta usa limite superior **exclusivo**, com `dataFim + 1 dia` |
| Fev/2026 tem pedidos de Autoajuda **somente** nos status que não contam | Confundir "mês sem venda" com "mês sem pedido". Pega RN0073 e RN0074 juntas |
| "Duna" pertence a **Ficção Científica e Fantasia** | Decisão declarada: conta integralmente nas duas |

Os valores esperados **não** são constantes escritas no teste: a task `analise:totais`
recalcula os totais fora da aplicação, a partir da mesma massa, e o teste compara com
o que a tela produziu. É um oráculo independente.

---

## 5. O que foi construído, e por quê

### Camadas

```
AdminController           → recebe os parâmetros, escolhe o template e traduz
                            ValidacaoException em erro de tela.
                            Não valida e não calcula.
AnaliseVendasService      → TODAS as regras: autorização, RF0056, RN0072,
                            RN0073, conversão para reais, montagem das
                            etiquetas e das séries
AnaliseVendasRepository   → uma consulta SQL, via JdbcTemplate
admin-analise.html        → recebe `etiquetas` e `series` prontas
```

### Uma consulta só, e o filtro de categoria em Java

A consulta agrupa por `strftime('%Y-%m', p.data_pedido)` e por categoria. O filtro de
categorias é aplicado **em Java**, não no SQL: a lista é pequena, e um `IN` dinâmico
complicaria a consulta sem ganho de desempenho.

### A RN0073 não sai do banco

O banco só devolve os meses que **têm** venda. A lista completa de meses do intervalo
é gerada no Service, preenchida com `0.00`, e só depois recebe os totais. É por isso
que a linha nunca quebra.

### RNF0011

Índice `ix_pedido_status_data` em `pedido(status, data_pedido)` — exatamente a ordem
do `WHERE`. O teste mede cinco respostas HTTP e exige até 1000 ms em cada uma.

---

## 6. Declarar antes que perguntem

### Divergência entre o enunciado e o DRS — status contabilizado

- **Enunciado da atividade:** "apenas pedidos com status ENTREGUE"
- **DRS, RN0074:** "Somente compras com status APROVADA, EM TRÂNSITO/TRANSPORTE ou
  ENTREGUE devem ser consideradas"

**Seguimos o DRS**, porque o cliente o definiu como única referência válida. Os dois
cenários pedidos continuam demonstráveis: o cenário 5 usa uma compra `REPROVADA`, que
a RN0074 exclui explicitamente, e o cenário 6 parte de `EM PROCESSAMENTO`, que não
conta sob nenhuma das duas leituras.

**Pergunta para o professor:** qual das duas redações vale?

### RNF0044 está duplicado no DRS

O identificador aparece duas vezes: "Formatação monetária do eixo de valores", no
grupo Análise, e "Recomendação com IA Generativa", no grupo de recomendação
personalizada. Implementamos o primeiro.

### Perfil de administrador sem autenticação

O DRS usa o termo "usuário com perfil de administrador" (RF0039, RF0040, RF0055 a
RF0058), mas **não define autenticação** — que o cliente dispensou. A coluna
`cliente.perfil` existe e guarda o conceito; a troca de perfil é uma **simulação de
sessão explícita**, do mesmo tipo da `OperadoraSimulada` já autorizada.

Hoje nenhum código lê essa coluna: a autorização vem da sessão. A coluna é onde o
perfil passará a ser lido quando houver autenticação. O log registra o responsável
como `usuario-teste (ADMIN)`, então fica provado no log quem agiu.

### Decisões de cálculo declaradas

| Decisão | Razão |
|---|---|
| O período filtra e agrupa `data_pedido` | A RN0074 conta compras `APROVADA`, que não foram entregues e não têm data de entrega. Agrupar por entrega seria impossível |
| O valor vem de `item_pedido`, sem frete e sem desconto | Frete e desconto pertencem ao pedido, não a uma categoria |
| Livro em duas categorias conta nas duas | A RF0057 compara categorias; não reparte o faturamento. A soma das linhas, portanto, não é o faturamento |

### Fora do escopo desta entrega

Comparação por produto, projeções, outros indicadores e recomendação com IA.
A exportação (RF0058) **está** implementada, embora o enunciado a tenha posto fora de
escopo: ela é requisito do DRS.

---

## 7. Perguntas de arguição prováveis

**"Por que o eixo X usa a data do pedido e não a da entrega?"**
Porque a RN0074 manda contar compras `APROVADA`, que ainda não foram entregues e não
possuem data de entrega. A RN0071 confirma: a soma é "das vendas de cada categoria
dentro do respectivo mês" — o mês é o da venda.

**"Se um livro está em duas categorias, a soma das linhas dá o faturamento?"**
Não, e é proposital. O gráfico responde "quanto a categoria Fantasia vendeu", e esse
livro vendeu como Fantasia. Somar as linhas contaria o mesmo item duas vezes.

**"Onde está a regra? Não pode estar no front-end."**
No `AnaliseVendasService`. O template só recebe `etiquetas` e `series` prontas, e o
`AdminController` não valida nada. O índice no banco é rede de segurança de
desempenho, não regra.

**"Como o teste sabe que o valor está certo?"**
Pela task `analise:totais`, que recalcula os totais fora da aplicação a partir da
mesma massa. Se a consulta e o oráculo discordarem, o teste falha.

**"Por que o limite superior da data é exclusivo?"**
Porque `data_pedido` guarda data e hora. Com `<= '2026-10-31'`, uma venda às
23:59:59 do dia 31 ficaria de fora. A massa tem um pedido exatamente nesse instante
para provar que não fica.

**"Por que a tela de análise fica vazia na porta 8080?"**
Porque o `data.sql` não insere nenhum pedido, e a massa de análise é montada pela
task do Cypress, que só roda no ambiente de testes. A tela responde corretamente
"Sem vendas no período".

**"O cupom promocional não deveria ser desativado após o uso?"**
O DRS não diz isso. A RN0033 apenas limita a **um cupom promocional por compra**.
Cupom de troca é crédito de um cliente e é consumido; cupom promocional é campanha e
continua valendo. O `UPDATE` de consumo é escopado em `tipo = 'TROCA'`.

---

## 8. Situação das pendências de 28/10

Todas as pendências funcionais foram fechadas e têm teste:

| Item | Teste |
|---|---|
| RF0044 + RN0042 — confirmar recebimento → `TROCADO` | `rf0044-receber-troca` |
| RF0045 + RF0054 — cupom de troca e reentrada em estoque | `rf0045-cupom-troca` |
| RN0032 — as três ações no checkout | `rn0032-revalidar-estoque-checkout` |
| RN0044 + RN0045 — bloqueio e expiração do carrinho | `rn0044-expiracao-carrinho` |
| `CupomController` — última chamada a `DadosFalsos`, substituída | `rf0045-cupom-troca` |

**Resta apenas documentação:**

- O DVP diz "Spring Boot 3"; o projeto usa a **4**
- O CDU02 precisa recuperar a seção "Controle de Estados da Compra" e corrigir o
  sumário, que ainda é o do template
- O `roteiro-apresentacao-crud.md` afirma por engano que o DRS não tem regras de
  negócio para cliente — tem: RN0021 a RN0028
- Mover os cartões no Planner e tirar print do quadro
