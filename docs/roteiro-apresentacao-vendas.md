# Roteiro — Apresentação dos Testes do Fluxo de Vendas

Livraria Estante do Saber · Gustavo Vinícius Bonifácio · 19/10/2026

---

## 1. Antes de entrar na sala

- [ ] Apagar `e2e.db`
- [ ] Subir a aplicação **no ambiente de testes**:
  ```
  npm run app:test
  ```
- [ ] Conferir `Started LivrariaApplication` e abrir <http://localhost:8081>
- [ ] Abrir o Cypress: `npm run e2e` → E2E → Chrome
- [ ] Abrir o DB Browser no `e2e.db`
- [ ] Abrir o VSCode em `PagamentoService.java` — é o arquivo da arguição
- [ ] Abrir este roteiro e o Plano de Testes

> ⚠️ A porta 8081 é o padrão do `cypress.config.js` — não passar flag `--config`.
> Se a aplicação subir na 8080, os testes de vendas falham de propósito: a rotina de
> preparação recusa qualquer base que não seja a de testes.

## 2. Sequência da apresentação

1. Abrir a **tabela da seção 3** e dizer: cada teste valida requisitos específicos do DRS,
   e o nome do arquivo já diz quais.
2. Rodar os **7 testes de vendas**, narrando o que cada um prova enquanto o Chrome executa.
3. Depois do `rf0038`: abrir o **DB Browser** e mostrar as tabelas `pedido`, `item_pedido`
   e `pagamento` com o registro recém-criado e o status `EM PROCESSAMENTO`.
4. Depois do `rn0036`: mostrar na tabela `cupom` o **cupom de troca emitido** pelo excedente.
5. Abrir o **`PagamentoService`** e mostrar a estrutura: cupons → verificação de cupom
   desnecessário → saldo restante → cartões → exceção da RN0035.
6. **Declarar o DF001** antes que perguntem (seção 6).

---

## 3. Requisito → teste

| Teste | Requisitos | O que prova |
|---|---|---|
| `rf0031-carrinho-multiplos-itens` | RF0031, RF0032 | Dois livros no carrinho, quantidade editável na adição e na visualização, totais recalculados |
| `rn0031-estoque-insuficiente` | RN0031 | Recusa quantidade acima do estoque, tanto na adição quanto na alteração |
| `rf0038-compra-dados-cadastrados` | RF0033, RF0034, RF0035, RF0036, RF0038 | Compra completa com endereço e cartão já cadastrados; pedido gravado com `EM PROCESSAMENTO` |
| `rf0035-novo-endereco-e-cartao` | RF0035, RF0036, RN0023, RN0024, RN0025 | **Reprovado** — ver DF001 |
| `rn0034-multiplos-cartoes` | RN0034 | Recusa R$ 9,99; aceita R$ 10,00 + R$ 37,00; dois pagamentos gravados |
| `rn0035-cupom-com-cartao` | RN0035, RF0037 | Cupom de R$ 42 + cartão de R$ 5,00, abaixo do mínimo, aceito pela exceção |
| `rn0036-cupom-excedente` | RN0036, RF0037 | R$ 55 em cupons para compra de R$ 47; emite cupom de troca de R$ 8,00 |

---

## 4. O que foi construído, e por quê

### Arquitetura em três camadas

```
CarrinhoController / CompraController   → recebem a requisição, não validam nada
CarrinhoService, CheckoutService,
FreteService, PagamentoService,
PedidoService                           → TODAS as regras
LivroRepository, PedidoRepository,
LogRepository                           → apenas SQL, via JdbcTemplate
```

**Por que serviços separados:** cada um tem uma responsabilidade do processo —
carrinho, entrega, frete, pagamento, pedido. Isso permite testar a composição do
pagamento sem passar pelo carrinho, e mantém cada regra num lugar só.

### Carrinho em sessão, não em banco

`CarrinhoService` é `@SessionScope`: existe um carrinho por sessão de navegador.

**Por quê:** o RF0031 define o carrinho como *"repositório temporário"*. E o bloqueio
de itens (RN0044) está fora do escopo desta entrega, então não há necessidade de
persistir. Menos duas tabelas, e o comportamento é o que o requisito pede.

### Valores monetários em centavos

Todas as colunas de dinheiro são `INTEGER` em centavos: `preco_centavos`,
`total_centavos`, `valor_centavos`.

**Por quê:** `REAL` acumula erro de arredondamento. Numa compra que soma cupons e
divide o restante entre cartões, centavos somem. Em centavos inteiros, a conta fecha
exatamente — e o banco ainda verifica com
`CHECK (total_centavos = subtotal + frete - desconto)`.

Na camada de negócio uso `BigDecimal`, pelo mesmo motivo: nunca `double` para dinheiro.

### Dados históricos congelados no pedido

`pedido.endereco_entrega_json` guarda o endereço, e `item_pedido` guarda `titulo` e
`preco_unitario_centavos`.

**Por quê:** se o cliente alterar o endereço ou o livro mudar de preço depois, o pedido
não pode mudar junto. O pedido registra o que valia **no momento da compra**.

### Banco de testes isolado

Os testes rodam em `e2e.db`, na porta 8081. A rotina de preparação **recusa** executar
se o endereço não for o do ambiente de testes.

**Por quê:** sem isso, cada execução sujaria a base de desenvolvimento. E confiar na
disciplina de quem roda não funciona — por isso o código impede o engano.

### Cada teste prepara o próprio cenário

A task `vendas:preparar` cria cliente, endereços, cartões e cupons conforme o caso.

**Por quê:** um teste não pode depender do estado que encontrou. Se dependesse, a ordem
de execução mudaria o resultado, e um teste poderia passar só porque outro rodou antes.
O padrão é *preparar, agir, verificar*.

---

## 5. As regras de pagamento — a parte que vai ser cobrada

### RN0033 — um cupom promocional por compra
Contador de promocionais; o segundo é recusado. No banco há também um **índice único
parcial** por pedido, como rede de segurança.

### RN0034 — mínimo de R$ 10,00 por cartão
Verificado no `PagamentoService`.

> **Ponto importante:** esse mínimo **não** está no banco. Chegou a estar, e foi
> removido — porque a RN0035 cria uma exceção, e o banco não tem o contexto do
> pagamento inteiro para avaliá-la. Além disso, regra de negócio no banco é proibida
> pelo cliente.

### RN0035 — exceção ao mínimo
A exceção só vale quando **três condições** ocorrem juntas:

| Condição | Por quê |
|---|---|
| Há cupons na composição | a RN0035 só existe para o uso combinado |
| O saldo restante é menor que R$ 10,00 | o aproveitamento máximo dos cupons já ocorreu |
| Há apenas um cartão | com dois, o cliente poderia fatiar o valor e burlar a RN0034 |

No código é a variável `excecao`. É literalmente a tabela de decisão do Plano de Testes.

### RN0036 — cupom desnecessário e excedente

A verificação: **se eu remover o menor cupom e os demais ainda cobrem o total, há cupom
desnecessário.**

**Por que o menor:** se remover o menor já não cobre, remover qualquer outro também não
cobriria — o menor é o mais fácil de dispensar. Um teste só resolve o caso geral.

O excedente é `soma dos cupons − total`, e vira um cupom de troca emitido ao cliente,
com validade de 30 dias parametrizada em `application.properties`.

> **Se perguntarem qual combinação o sistema escolhe:** o DRS não define. O exemplo da
> própria RN0036 admite três resultados (R$ 5, R$ 10 ou R$ 25). Adotei o critério de
> **recusar o cupom dispensável** e deixar a escolha ao cliente — está registrado como
> risco R3 no Plano de Testes.

### RF0034 — frete

| Destino | Base |
|---|---|
| Brasil, SP | R$ 15,00 |
| Brasil, demais estados | R$ 25,00 |
| Outros países | R$ 50,00 |

Mais **R$ 2,00 por unidade** no carrinho.

**Por que assim:** o RF0034 exige considerar *"os itens selecionados **e** o endereço"* —
por isso tem as duas parcelas. O DRS não define fórmula, faixas nem critérios, então o
valor é demonstrativo e está declarado como risco R4 no Plano de Testes.

---

## 6. Declarar antes de perguntarem

### DF001 — cadastro de cartão durante a compra

O `rf0035` **falha**. A tela de pagamento não oferece a opção de cadastrar um cartão novo
durante a compra (RF0036). O cadastro de **endereço** novo no mesmo fluxo funciona — é a
primeira metade do teste, que passa.

Severidade Alta, registrado no Plano de Testes, correção prevista para 23/10.

**Como apresentar:** rodar o teste, deixar falhar, e mostrar a linha que falha:

```js
// Contrato da funcionalidade ainda pendente.
cy.get('[data-cy="usar-novo-cartao"]').check();
```

E dizer: *"o teste detecta a ausência da funcionalidade — é exatamente para isso que
ele existe."* Esconder daria uma suíte verde e falsa.

### Fora do escopo desta entrega

Validação do pagamento e mudanças de status (RN0037, RN0038, RF0039, RF0040), baixa em
estoque (RF0053, RN0028), bloqueio e expiração do carrinho (RN0032, RN0044, RN0045) e
todo o processo de troca (RF0041 a RF0045).

Por isso o estoque **não é alterado** após a compra, e o pedido **para** em
`EM PROCESSAMENTO`.

---

## 7. Perguntas prováveis

**"Onde está a regra de negócio?"**
Na camada Service. O Controller recebe e encaminha; o Repository só executa SQL; o
template exibe. O `PagamentoService` concentra as quatro regras de pagamento.

**"Por que o mínimo de R$ 10,00 não está no banco?"**
Porque a RN0035 cria uma exceção que depende do contexto do pagamento inteiro — quantos
cupons, qual o saldo, quantos cartões. O banco vê uma linha de cada vez. E regra no
banco é proibida pelo cliente.

**"Por que centavos em vez de reais?"**
Arredondamento. Dividir R$ 126,00 entre dois cartões e abater cupons com `double`
produz diferenças de centavos. Em inteiros, não existe fração a perder.

**"O carrinho não deveria estar no banco?"**
O RF0031 o define como repositório temporário. O bloqueio de itens, que exigiria
persistência, está fora do escopo. Se entrar, o carrinho migra para o banco.

**"Como o sistema sabe quem é o cliente, se não há login?"**
Não há autenticação, por decisão do cliente. Uso um cliente fixo identificado no
cabeçalho. Isso é uma lacuna do DRS — ele exige o "usuário responsável" no log
(RNF0012) e pressupõe perfil de administrador (RF0039), sem nenhum requisito de
autenticação. Está registrado no CDU02 e como risco R5 no Plano de Testes.

**"O que esse teste prova?"** *(sobre o `rn0034`)*
Que R$ 9,99 é recusado com a mensagem correta **e que nenhum pedido foi gravado**;
depois, que R$ 10,00 + R$ 37,00 é aceito, gera pedido com `EM PROCESSAMENTO` e grava
dois pagamentos somando exatamente o total.

**"Por que o teste cadastra tudo antes de agir?"**
Independência. Um teste não pode depender do que outro deixou no banco, nem da ordem
de execução.

**"Por que o CVV não é gravado?"**
A RN0024 exige o campo no cadastro, e ele é capturado. Mas o padrão PCI DSS veda
armazená-lo: o CVV prova posse física do cartão no momento da compra, e guardá-lo
anularia essa prova. Tensão registrada como risco R6.

**"E se o cliente esquecer a senha?"**
Gera-se uma nova. BCrypt é via única — não descriptografa. A verificação aplica o
mesmo salt à senha digitada e compara os hashes.

**"Como vocês garantem que a compra grava tudo ou nada?"**
`PedidoService.finalizar` é `@Transactional`. Pedido, itens, pagamentos, consumo de
cupons, emissão do cupom de troca e os registros de log acontecem na mesma transação.
Se qualquer passo falhar, nada é gravado.
