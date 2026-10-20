# Checklist do projeto — atualizado em 21/10/2026

**Entrega do UC Gerenciar Vendas Eletrônicas: 28/10/2026. Meta: sem pendências.**

Documento vivo. Marcar o que for concluído e manter atualizado: é a referência para
retomar o trabalho a qualquer momento, inclusive em outra máquina ou nova sessão.

---

## Situação atual

**Entregue:** tema e tecnologias (17/08) · protótipo das 21 telas (24/08) · estimativa e
DVP (05/09) · CRUD de cliente apresentado (21/09) · especificação CDU02 (29/09) ·
Plano de Testes (18/10).

**Não apresentado:** os testes do fluxo de vendas (19/10) — ficou para a última hora.

**Implementado e testado:** CRUD de cliente completo, carrinho, endereço, frete,
pagamento com múltiplos cartões e cupons, finalização do pedido com status
`EM PROCESSAMENTO`. 17 testes Cypress, sendo 16 aprovados e 1 reprovado (DF001).

**Área administrativa:** ainda é protótipo — `AdminController` tem 12 chamadas a
`DadosFalsos`.

---

## 0. Higiene — fazer agora (15 min)

- [ ] Commitar os 4 arquivos pendentes (`README.md`, `PagamentoService.java`,
      `docs/roteiro-apresentacao-vendas.md`, `.gitignore`)
- [ ] `git push`
- [ ] Conferir que `cypress/screenshots/` não entrou no commit

---

## 1. Destravar o restante (≈7 h) — **gargalo**

Nada da entrega de vendas avança sem isto.

- [ ] Criar as tabelas `troca` e `item_troca` no `schema.sql`
- [ ] Criar a tabela `reserva_estoque` (bloqueio temporário: livro, quantidade, expiração)
- [ ] Criar a tabela `notificacao` (RN0046)
- [ ] `AdminController`: substituir as 12 chamadas a `DadosFalsos` pelo `PedidoRepository` real
- [ ] Apagar `livraria.db`, subir e conferir que as telas do admin continuam abrindo

## 2. Validação do pagamento e estoque (≈6 h)

- [ ] `RN0037` — validar validade e veracidade dos cupons
- [ ] `RN0037` — simular o aceite da operadora de cartão (autorizado pelo professor)
- [ ] `RN0038` — definir status `APROVADA` ou `REPROVADA`
- [ ] `RF0053` + `RN0028` — baixa de estoque **somente** para compras aprovadas
- [ ] `RN0028` — desbloquear itens das compras não aprovadas, mantendo-os em estoque
- [ ] `RN0032` — revalidar estoque entre o carrinho e a finalização, notificando o cliente

## 3. Fluxo do administrador (≈4 h)

- [ ] `RF0039` + `RN0039` — despachar: `APROVADA` → `EM TRANSPORTE`
- [ ] `RF0040` + `RN0040` — confirmar entrega: `EM TRANSPORTE` → `ENTREGUE`

## 4. Bloqueio e expiração do carrinho (≈8 h) — **o mais difícil**

Único ponto do sistema com comportamento dependente de relógio.

- [ ] `RN0044` — bloquear o item ao incluir no carrinho; prazo contado a partir do
      **último** item incluído
- [ ] `RN0045` — ao expirar, retirar do carrinho **todos** os itens do mesmo produto
- [ ] `RNF0042` — exibir os itens removidos e desabilitar o botão de compra
- [ ] Parâmetro de prazo em `application.properties`

## 5. Ciclo de troca (≈10 h)

- [ ] `RF0041` + `RN0041` + `RN0043` — cliente solicita troca de item ou da compra
      inteira, somente se `ENTREGUE` → status `EM TROCA`
- [ ] `RF0043` — administrador visualiza os pedidos com status `EM TROCA`
- [ ] `RF0042` — administrador autoriza → `TROCA AUTORIZADA`
- [ ] `RN0046` — notificar o cliente da autorização
- [ ] `RF0044` + `RN0042` — confirmar recebimento dos itens → `TROCADO`
- [ ] `RF0045` — emitir cupom de troca e disponibilizá-lo ao cliente
- [ ] `RF0054` — reentrada dos itens no estoque

## 6. Pendências antigas (≈7 h)

- [ ] **DF001** — cadastro de cartão novo durante a compra (`RF0036`); o teste
      `rf0035` falha hoje por causa disso
- [ ] Cliente consulta pedidos, status e cupons de troca disponíveis
- [ ] `RN0021` / `RN0022` — exigir ao menos um endereço de cobrança e um de entrega
- [ ] `RN0026` — telefone composto de tipo, DDD e número

## 7. Os 11 cenários de teste exigidos (≈10 h)

| # | Cenário | Situação |
|---|---|---|
| 1 | Compra aprovada com um cartão | depende da etapa 2 |
| 2 | Dois cartões, tentativa abaixo de R$ 10,00 bloqueada | ✅ pronto (`rn0034`) |
| 3 | Cupom **promocional** + cartão abaixo de R$ 10,00 | ⚠️ o teste usa cupom de troca; trocar na preparação (15 min) |
| 4 | Cupons de troca acima do valor, com cupom da diferença | ✅ pronto (`rn0036`) |
| 5 | Compra `REPROVADA`, itens desbloqueados, sem baixa | depende da etapa 2 |
| 6 | Expiração do bloqueio no carrinho | depende da etapa 4 |
| 7 | Admin: `APROVADA` → `EM TRANSPORTE` → `ENTREGUE` | depende das etapas 1, 2 e 3 |
| 8 | Troca bloqueada em pedido não `ENTREGUE` | depende da etapa 5 |
| 9 | Troca de um item e de uma compra inteira | depende da etapa 5 |
| 10 | Fluxo completo da troca | depende da etapa 5 |
| 11 | Usar o cupom gerado pela troca em nova compra | depende da etapa 5 |

- [ ] Escrever um arquivo de teste por cenário, em `cypress/e2e/vendas/`
- [ ] Rodar os testes do zero: apagar `e2e.db`, subir na porta 8081, executar todos

## 8. Documentação e processo (≈4 h)

- [ ] Atualizar `docs/validacao-ia.md` a cada revisão — **é requisito do cliente**
- [ ] Corrigir no `docs/roteiro-apresentacao-crud.md` a afirmação errada de que o DRS
      não tem regras de negócio para cliente (tem: RN0021 a RN0028)
- [ ] Atualizar o DVP: diz "Spring Boot 3", o projeto usa a **4**
- [ ] Atualizar o documento da 1ª entrega: incluir Livros e Estoque no escopo e trocar
      "excluir" por "inativar"
- [ ] Recolocar no CDU02 a seção "Controle de Estados da Compra" e atualizar o sumário
      (o sumário entregue ainda é o do template, com "Escola Cadastrada")
- [ ] Mover os cartões no Planner e tirar print do quadro
- [ ] Atualizar a apresentação com os slides da entrega de vendas

---

## Cronograma — entrega em 28/10/2026

**Regra que mudou:** o teste de cada cenário é escrito **junto com a funcionalidade**,
não no fim. Deixar os 11 testes para o último dia foi o que inviabilizou a apresentação
de 19/10.

| Dia | Trabalho | Cenários fechados | Horas |
|---|---|---|---|
| 21/10 | Etapa 1 — destravar: tabelas e admin fora do protótipo | — | 7 |
| 22/10 | Etapa 2 — validação do pagamento, APROVADA/REPROVADA, baixa de estoque | 1 e 5 | 8 |
| 23/10 | Etapa 3 — despachar e entregar · DF001 (cartão novo na compra) | 7 | 9 |
| 24/10 | Etapa 5a — solicitar troca, autorizar, notificar | 8 e 9 | 8 |
| 25/10 | Etapa 5b — confirmar recebimento, cupom, reentrada em estoque | 10 e 11 | 8 |
| 26/10 | Etapa 4 — bloqueio e expiração do carrinho | 6 | 9 |
| 27/10 | Etapa 8 — documentação, Planner, ensaio da apresentação | — | 6 |
| 28/10 | **Entrega** | | |

Cenários 2 e 4 já estão prontos. O cenário 3 é um ajuste de 15 minutos: trocar o cupom
de troca por promocional na rotina de preparação do `rn0035`.

**Se algum dia atrasar**, o corte é a Etapa 4 (26/10): custa 9 horas e entrega um único
cenário. Declarar como pendente é defensável — é a única parte do sistema com
comportamento dependente de relógio.


---

## Perguntas em aberto para o professor

2. A numeração de RF do enunciado está deslocada em um a partir de "finalizar compra"
   (enunciado `RF0037` = DRS `RF0038`, e assim por diante até `RF0044` = `RF0045`).
   Seguir o DRS?
3. O enunciado cita `RNF0045` e `RNF0046`; no DRS são `RN0045` e `RN0046`.
4. Ficaram dois `[DECISÃO]` não preenchidos na tabela de status do enunciado.
5. No Plano de Testes, os dois `[N]` do enunciado não foram definidos.

---

## Esforço total estimado

| Etapa | Horas |
|---|---|
| 0. Higiene | 0,25 |
| 1. Destravar | 7 |
| 2. Pagamento e estoque | 6 |
| 3. Fluxo do administrador | 4 |
| 4. Bloqueio no carrinho | 8 |
| 5. Ciclo de troca | 10 |
| 6. Pendências antigas | 7 |
| 7. Os 11 cenários | 10 |
| 8. Documentação | 4 |
| **Total** | **≈56 h** |

**Ordem obrigatória:** 0 → 1 → 2 → 3 → 5 → 7. A etapa 4 é independente e pode ser feita
em paralelo ou adiada. A etapa 6 pode ser adiada, mas o DF001 reprova um teste que já
existe. A etapa 8 pode ser feita em qualquer momento.

**Se o prazo for curto**, a ordem que entrega mais cenários por hora é
1 → 2 → 3 (cenários 1, 5 e 7) e depois 5 (cenários 8 a 11). A etapa 4, sozinha, entrega
um único cenário e custa 8 horas — é a primeira candidata a ser declarada como pendente.
