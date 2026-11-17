# Checklist — Chatbot de Recomendação com IA Generativa

**Entrega: 25/11/2026, presencial. O professor testa a funcionalidade ao vivo.**
Requisito: **RNF0044 — Recomendação com IA Generativa** (grupo "Recomendação
personalizada" do DRS).

Documento vivo. É a referência para retomar o trabalho a qualquer momento.

> ⚠️ **RNF0044 está duplicado no DRS.** O mesmo identificador nomeia
> "Formatação monetária do eixo de valores", no grupo Análise — já entregue em
> 09/11 — e "Recomendação com IA Generativa". O enunciado desta atividade cita o
> segundo. Registrado como pergunta ao professor.

---

## 1. Arquitetura — a decisão central

O critério (c) do enunciado proíbe a solução fácil:

> *"O modelo deve passar por treinamento ou ajuste fino com dados de vendas e
> feedback dos usuários. **Não basta enviar esses dados como contexto das
> perguntas.**"*

E a API do Claude **não oferece fine-tuning** — não existe endpoint de treino.
A saída é separar as duas responsabilidades:

```
Chat (Claude, com reserva local)  →  conversa, busca, responde dúvidas    item 2
       ↓ chama como ferramenta
Recomendador treinado             →  pesos aprendidos de vendas e feedback  itens 1 e 3
       ↑ retreina
vendas + feedback_resposta
```

**Justificativa, para a arguição:** ajuste fino de um LLM ensina estilo e
vocabulário, não preferência individual de cliente. Treinar um LLM com o
histórico de vendas não o faria recomendar melhor para o cliente 2 — e cada
venda nova exigiria um treino novo, o que inviabilizaria a personalização
contínua que o item 3 exige. Sistemas reais separam o recomendador da camada de
linguagem pela mesma razão.

**Risco declarado:** se o professor exigir literalmente o LLM ajustado, esta
leitura não atende. O autor avaliou o risco como aceitável e seguiu.

---

## 2. Contrato do feedback — definição funcional

Um botão só, com dois significados possíveis, é ambíguo. O escopo é restringido
em vez de escolhido:

| A mensagem recomendou livros? | O que o voto significa |
|---|---|
| **Sim** — há linhas em `recomendacao_mensagem` | Sinal de preferência pelas **categorias daqueles livros**. Participa do treinamento |
| **Não** — busca, dúvida, conversa | Avaliação da resposta. É **gravado**, mas não participa do treinamento de preferência |

Decorrências:

- A associação é **ao livro recomendado**, não à categoria. Permite rastrear
  exatamente o que foi recomendado, e a categoria é derivada.
- Na extração, cada mensagem conta **uma vez por categoria**, mesmo que tenha
  recomendado dois livros da mesma categoria → `COUNT(DISTINCT mensagem_chat_id)`.
- **O voto negativo é sinal de preferência, não prova de desinteresse.** Esta é
  a interpretação adotada nesta etapa, e está registrada como tal.

**Resposta pronta para a arguição** — *"e se o cliente só não gostou de como a
resposta foi escrita?"*: só é lido como preferência quando a mensagem de fato
recomendou livros. Os dois casos ficam gravados e distinguíveis.

---

## 3. Janelas temporais

Sem separação temporal há **vazamento de alvo**: se os votos entram como
feature e "avaliou como útil" entra no rótulo, o modelo copia a resposta em vez
de aprender preferência. As três janelas são explícitas e persistidas:

```
features   : eventos com data < data_corte
rótulo     : eventos em [data_corte, fim_observacao)
validação  : eventos em [fim_observacao, fim_validacao)   — avaliada, nunca ajustada
```

As três datas ficam gravadas em `modelo_recomendacao`. Sem `fim_observacao`
registrado, reexecutar o mesmo corte mais tarde produziria outros rótulos — a
extração deixaria de ser reproduzível.

---

## 4. O modelo

Regressão logística, gradiente descendente, Java puro. Sem biblioteca, sem GPU.

```
score(cliente, categoria) = σ( w · x̂ + viés_categoria + intercepto )
```

**Features** `x`, por par (cliente, categoria), todas anteriores ao corte:

| # | Feature | Definição |
|---|---|---|
| 1 | `quantidade` | unidades compradas na categoria |
| 2 | `valor` | centavos gastos na categoria |
| 3 | `recencia` | `1 / (1 + dias entre a última compra na categoria e data_corte)` |
| 4 | `peso_declarado` | preferência **vigente na data_corte** (linha temporal) |
| 5 | `votos_uteis` | mensagens distintas com voto útil que recomendaram a categoria |
| 6 | `votos_nao_uteis` | idem, com voto não útil |

`x̂` é a normalização z-score, com média e desvio calculados **somente sobre a
janela de features**.

**Rótulo** `y = 1` se, na janela de rótulo, o cliente comprou na categoria ou
votou útil numa recomendação dela; `y = 0` caso contrário.

**Aprendido:** 6 coeficientes, 1 intercepto, 1 viés por categoria.

---

## 5. Persistência completa do modelo

Uma versão treinada tem de ser recarregável sem recalcular estatística alguma —
por isso a normalização é persistida junto dos coeficientes.

```sql
modelo_recomendacao(
    id, versao, treinado_em,
    data_corte, fim_observacao, fim_validacao,   -- as três janelas
    amostras, iteracoes, taxa_aprendizado,
    perda_inicial, perda_final,
    metrica_validacao,                           -- medida na terceira janela
    intercepto
)

coeficiente_modelo(
    modelo_id, feature,
    coeficiente,          -- o que foi aprendido
    media, desvio         -- a normalização usada; sem ela a versão não se aplica
)

vies_categoria(modelo_id, categoria_id, vies)
```

`coeficiente_modelo` guarda, numa linha por feature, o coeficiente **e** os
parâmetros de normalização. É isso que torna a versão 3 aplicável a dados de
hoje sem depender de recalcular nada.

> Nota de nomenclatura: a tabela chamada `peso_modelo` no plano anterior
> guardava só o viés por categoria, e o nome dava a entender que *o modelo era*
> um peso por categoria — o que seria um modelo global, sem personalização.
> Renomeada para `vies_categoria`, e os coeficientes ganharam tabela própria.

---

## 6. Limitações declaradas

Quatro, todas a serem mencionadas na apresentação antes que sejam perguntadas.

**6.1 Ausência de interação é rótulo assumido.**
`y = 0` quando não há compra nem voto útil na janela futura. Isso significa
**ausência de sinal positivo observado**, não rejeição comprovada. A massa
controlada permite testar a interpretação, não validá-la como verdade.

**6.2 O evento temporal da compra.**
`pedido` só tem `data_pedido` — nenhuma data de transição de status. A compra
contabilizada é definida como: `data_pedido` dentro da janela **e** `status`
entre os que contam (`APROVADA`, `EM TRANSPORTE`, `ENTREGUE`), reaproveitando o
conjunto da RN0074, já usado na análise de vendas.

Consequência: o `status` é lido no momento da extração, então um pedido feito
antes do corte e aprovado depois aparece como compra anterior ao corte.
Mitigação nesta entrega: **a massa é construída para que nenhum pedido mude de
status depois do corte**. Correção completa, fora do escopo desta entrega:
datas de transição em `pedido`, como `troca` já tem (`data_solicitacao`,
`data_autorizacao`, `data_recebimento`). É a mesma lacuna que a entrega de
análise declarou ao agrupar por `data_pedido`.

**6.3 O sinal do coeficiente não é garantido.**
A regressão sem restrições não garante que `coeficiente(votos_nao_uteis)` será
negativo em qualquer massa. Nesta entrega o sinal é verificado como propriedade
empírica do modelo treinado sobre massa controlada. Garanti-lo exigiria uma
restrição explícita na otimização — que não foi adotada, para não impor a
conclusão que o treino deveria descobrir.

**6.4 Perda de treino não é capacidade de previsão.**
A perda cair comprova que a otimização convergiu naquela massa. A afirmação de
que o modelo prevê comportamento futuro depende da **métrica de validação**, na
terceira janela, que não participa do ajuste. Se a janela de validação for
cortada por falta de tempo, a afirmação defensável passa a ser apenas "a
otimização convergiu".

---

## 7. Esquema completo a criar

| Tabela | Finalidade |
|---|---|
| `preferencia_cliente(id, cliente_id, categoria_id, peso_declarado, vigente_de)` | **Append-only.** Critério (e), fonte explícita. O valor vigente é a linha de maior `vigente_de`; a extração usa a maior anterior à `data_corte`. `UNIQUE(cliente_id, categoria_id, vigente_de)` |
| `conversa(id, cliente_id, iniciada_em)` | Uma conversa por sessão de chat |
| `mensagem_chat(id, conversa_id, papel, texto, criada_em)` | `papel CHECK IN ('CLIENTE','ASSISTENTE')` |
| `recomendacao_mensagem(mensagem_chat_id, livro_id)` | Quais livros aquela resposta recomendou. É o que dá sentido ao contrato da seção 2 |
| `feedback_resposta(id, mensagem_chat_id, util, criada_em)` | Critério (d). `util CHECK IN (0,1)`, `UNIQUE(mensagem_chat_id)` |
| `modelo_recomendacao` | Ver seção 5 |
| `coeficiente_modelo` | Ver seção 5 |
| `vies_categoria` | Ver seção 5 |

---

## 8. Etapas

### Etapa 0 — Higiene (10 min)

- [ ] `git push` — dois commits pendentes, `64c6394` e `7fd381f`
- [ ] Parar a aplicação no ar e apagar `e2e.db`

### Etapa 1 — Esquema e massa (≈4 h)

- [ ] As oito tabelas da seção 7 no `schema.sql`
- [ ] **Cliente A** com compras de Fantasia e Ficção Científica
- [ ] **Cliente B** com compras de Autoajuda e Literatura Clássica
- [ ] Compras **dos dois lados da `data_corte`**, para os dois clientes, e
      também na janela de validação
- [ ] Nenhum pedido muda de status depois do corte (limitação 6.2)
- [ ] Preferências declaradas para **um** dos dois, e nenhuma para o outro: é o
      que prova que as duas fontes funcionam
- [ ] Feedback inicial com `recomendacao_mensagem` preenchido, senão não há o
      que treinar
- [ ] Apagar os dois `.db`, subir e conferir que as telas abrem

### Etapa 2 — O modelo treinado (≈6 h) — **o critério (c)**

- [ ] `RecomendacaoRepository.extrair(dataCorte, fimObservacao)` — devolve, por
      par (cliente, categoria), as seis features e o rótulo. Votos contados com
      `COUNT(DISTINCT mensagem_chat_id)`
- [ ] `RecomendacaoService.treinar()` — z-score, gradiente descendente, e grava
      `modelo_recomendacao` + `coeficiente_modelo` + `vies_categoria`
- [ ] `RecomendacaoService.avaliar()` — métrica na janela de validação, sem
      ajustar nada
- [ ] `RecomendacaoService.recomendar(clienteId)` — carrega a versão vigente,
      pontua as categorias, ordena os livros **com estoque**, devolve os N primeiros
- [ ] **`RecomendacaoServiceTest`, cinco testes:**

| # | Teste | O que afirma |
|---|---|---|
| 1 | Perda | `perda_final < perda_inicial` — a otimização convergiu |
| 2 | Clientes distintos | A e B recebem listas diferentes |
| 3 | Sinal aprendido | Em massa controlada onde votos negativos precedem a ausência de compra: `coeficiente("votos_nao_uteis") < 0`. Propriedade do modelo treinado, não delta entre versões |
| 4 | Efeito no score | Com o modelo **fixo**, dois clientes idênticos exceto por um voto negativo em C: `score(comVoto, C) < score(semVoto, C)`. Aplicação determinística, sem treino no meio |
| 5 | Estoque | Duna não aparece, porque está sem lote |

> Os testes 3 e 4 substituem o teste inválido do plano anterior, que comparava
> duas versões e exigia que **um** coeficiente tivesse diminuído. Não se
> sustenta: o gradiente move todos os parâmetros juntos e as outras amostras
> também pesam na otimização.

→ fecha os cenários **2, 3** e metade do **8**

### Etapa 3 — Integração com a IA (≈4 h)

- [ ] `pom.xml`: `com.anthropic:anthropic-java:3.3.0`
- [ ] `ProvedorIA` — interface
- [ ] `ClaudeProvedor` — `AnthropicOkHttpClient.fromEnv()`, modelo `claude-opus-5-5`
- [ ] `OllamaProvedor` — HTTP em `localhost:11434`
- [ ] `ProvedorComFallback` — tenta Claude; em falha de credencial ou rede cai
      para o Ollama e registra qual atendeu
- [ ] Chaves em `application.properties`: provedor principal, modelo, limite de tokens
- [ ] JUnit do fallback, com o principal lançando exceção de propósito

→ fecha o cenário **1**

### Etapa 4 — O chatbot (≈5 h)

- [ ] Três ferramentas via `Tool.builder()`:

| Ferramenta | Entrada | Cenário |
|---|---|---|
| `recomendar_livros` | — (cliente da sessão) | 2, 3 |
| `buscar_livros` | `termo`, `categoria` | 4 |
| `detalhar_livro` | `livroId` | 5, 6 |

- [ ] `ChatbotService` — laço de tool use: envia a conversa, executa cada
      `tool_use`, devolve `tool_result`, repete até `stop_reason` ser `end_turn`.
      Grava `mensagem_chat` e, quando houve recomendação, `recomendacao_mensagem`
- [ ] `ChatbotController` — `POST /chat` e `POST /chat/{mensagemId}/feedback`
- [ ] Substituir o **fragmento estático** em `fragmentos/layout.html:69`:
      histórico real do banco, campo que envia de verdade, **útil / não útil**
      em cada resposta do assistente
- [ ] Corrigir o erro do protótipo: *"O Poder do Hábito, de J.R.R. Tolkien"* —
      o autor é Charles Duhigg. Aparece hoje em 10 telas
- [ ] **Remover** o box "Recomendados para você" de `catalogo.html:34` —
      recomendação fora do chat está fora de escopo pelo item 6 do enunciado
- [ ] Tela de preferências no perfil do cliente, gravando em
      `preferencia_cliente` com `vigente_de` (append-only)
- [ ] `ProvedorIAFalso`, ativado por propriedade, para os testes

> ⚠️ Os testes Cypress **não podem** chamar a API real: custaria dinheiro, seria
> instável e não rodaria offline. A integração real é demonstrada ao vivo.

→ fecha os cenários **4, 5, 6, 7**

### Etapa 5 — Retreino contínuo (≈2 h)

- [ ] `POST /admin/recomendacao/treinar` — relê vendas e feedback e grava nova
      versão. Exige ADMIN pelo `exigirAdministrador()` que já existe
- [ ] Tela `/admin/recomendacao` — versões com data, janelas, amostras, perda e
      métrica de validação, e os coeficientes da versão vigente. É o que se
      mostra no cenário 8
- [ ] JUnit: um feedback novo aparece na versão seguinte, e a anterior fica
      preservada no histórico

→ fecha o cenário **8**

### Etapa 6 — Testes e documentação (≈3 h)

- [ ] Task `chatbot:preparar` no `cypress.config.js`, com a trava de porta 8081
- [ ] Cypress, um arquivo por cenário em `cypress/e2e/chatbot/`:

| Arquivo | Cenário |
|---|---|
| `rnf0044-integracao-ia` | 1 |
| `rnf0044-recomendacao-personalizada` | 2 |
| `rnf0044-dois-clientes-distintos` | 3 |
| `rnf0044-busca-pelo-chat` | 4 |
| `rnf0044-duvidas-sobre-livros` | 5 e 6 |
| `rnf0044-feedback-util` | 7 |
| `rnf0044-retreino-incorpora-dados` | 8 |

- [ ] Documentação: justificativa da escolha da IA (critério b), decisão sobre
      as preferências (critério e), as quatro limitações da seção 6
- [ ] `docs/validacao-ia.md` — requisito do cliente
- [ ] `docs/roteiro-apresentacao-chatbot.md` com os 8 cenários e as perguntas
      de arguição

---

## 9. Cronograma

| Dia | Etapas | Cenários fechados |
|---|---|---|
| 1 | 0, 1 e 2 | 2 e 3 |
| 2 | 3 e 4 | 1, 4, 5, 6 e 7 |
| 3 | 5 e 6 | 8 |

≈24 h. **A ordem é obrigatória até a Etapa 4:** o chatbot não tem o que
recomendar antes de o modelo existir, e é por isso que a parte mais difícil vem
primeiro.

**Se atrasar**, a ordem de corte é:
1. A janela de validação (limitação 6.4 passa a valer, e a afirmação encolhe)
2. A tela de preferências do passo da Etapa 4 — derivar tudo do histórico e declarar
3. **Nunca a Etapa 5**, que é o cenário 8 e o critério (c)

---

## 10. A providenciar, em paralelo

- [ ] Chave em `console.anthropic.com` com crédito — sem ela a Etapa 3 trava
- [ ] Ollama instalado com um modelo baixado — sem isso a apresentação depende
      da rede

---

## 11. Perguntas em aberto para o professor

1. O critério (c) exige treinamento ou ajuste fino. A arquitetura da seção 1
   treina um recomendador com vendas e feedback e usa a IA generativa apenas na
   camada de conversa. Atende?
2. **RNF0044 está duplicado no DRS** — nomeia a formatação monetária do eixo do
   gráfico gerencial e a recomendação com IA generativa.
