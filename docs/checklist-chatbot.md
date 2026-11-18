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

**Notação, para não confundir na arguição:** acima os intervalos estão escritos
com fim exclusivo, mas o `PeriodoRecomendacao` guarda **datas finais
inclusivas** e converte para limite exclusivo ao montar o SQL
(`fimObservacao.plusDays(1).atStartOfDay()`). Com `PADRAO` = 01/11 → 10/11 →
17/11, a janela de rótulo é `[01/11, 11/11)` e a de validação `[11/11, 18/11)`.

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

## 8. Situação em 17/11/2026 — metade feita, e é a metade difícil

| Etapa | Situação | Evidência |
|---|---|---|
| 0 — Higiene | ✅ | árvore limpa, nada pendente de push |
| 0.5 — Acessos | ✅ | chave configurada · Ollama com `qwen2.5:7b`, capability `tools` |
| 1 — Esquema e massa | ✅ | 8 tabelas · `preferencia_cliente` append-only com `vigente_de` · `modelo_recomendacao` com `fim_validacao` e `metrica_validacao` |
| 2 — Modelo treinado | ✅ | `RecomendacaoRepository` · `RecomendacaoService` (612 linhas) · 6 testes |
| 3 — Integração com a IA | ❌ | `anthropic` ausente do `pom.xml`, nenhum `ProvedorIA` |
| 4 — Chatbot | ❌ | fragmento ainda estático, nenhum `ChatbotController` |
| 5 — Retreino | ❌ | nenhum endpoint, nenhuma tela |
| 6 — Testes e docs | ❌ | `cypress/e2e/chatbot/` não existe |

**Suítes em 17/11, com os dois `.db` recriados do zero:**
85 testes JUnit, 0 falhas · 43 specs e 56 testes Cypress, *All specs passed*.

O critério (c) — o risco real desta entrega — já tem modelo treinado, persistido
e testado. O que falta é a camada de conversa em volta dele.

### Lição de ambiente, registrada

A primeira execução da suíte em 17/11 reprovou **19 das 43 specs**, todas com o
mesmo erro: `table preferencia_cliente has no column named vigente_de`. O
esquema havia sido corrigido no commit, mas o `e2e.db` em disco era o antigo, e
`CREATE TABLE IF NOT EXISTS` **nunca altera tabela existente**. Depois de apagar
os dois `.db` e subir de novo, as 43 specs passaram.

A regra do `CLAUDE.md` — apagar os dois `.db` ao mudar o esquema — vale também
depois de um `git pull` ou ao trocar de máquina: o arquivo não vem no
repositório, mas o esquema vem.

### Por que a massa pode crescer sem medo

Verificado em `cypress.config.js`:

- `vendas:preparar` apaga todas as tabelas, recarrega o `data.sql` e **apaga de
  novo** clientes, pedidos e tabelas do chat, preservando só o catálogo.
- `analise:preparar` apaga todas as tabelas e monta o próprio cenário, **sem
  ler** o `data.sql`.

Ou seja: das duas suítes, só o **catálogo de livros** é fixture compartilhada.
Acrescentar clientes e compras à massa não pode quebrar nem vendas nem análise.

---

## 9. Etapa 2.5 — Corrigir o que já está pronto (≈2 h 30)

**Por que esta etapa existe:** o modelo funciona, mas não tem o que aprender.

**Problema 1 — 8 amostras para 11 parâmetros.** As amostras são pares
(cliente, categoria): 2 clientes × 4 categorias = 8. O modelo tem 6
coeficientes + 1 intercepto + 4 vieses = 11 parâmetros. Com mais parâmetros que
amostras, o treino **decora** em vez de aprender, a perda cai a quase zero por
construção, e o teste `perdaCaiENormalizacaoEModeloSaoPersistidos` passa sem
provar nada. É o ponto exato onde a arguição pega.

**Problema 2 — janela de validação vazia.** As janelas em
`PeriodoRecomendacao.PADRAO` são 01/11 → 10/11 → 17/11, mas a massa termina em
**06/11**. O `avaliarModelo` monta as features com tudo anterior a 11/11 e busca
os rótulos dentro de `[11/11, 18/11)`, onde não há nenhum evento: todos os
rótulos valem 0, o modelo pontua alto nas categorias que o cliente compra, e a
métrica fica péssima sem significar nada.

**Problema 3 — `avaliar()` trava até 18/11.** O método exige
`agora >= fimValidacao + 1 dia`. Em 18/11 destrava sozinho.

Os três se resolvem na mesma edição do `data.sql`.

1. **Gerar 18 CPFs válidos, com dígito verificador.** Guardar a lista antes de
   escrever o SQL. O `data.sql` escreve direto no banco e passa por fora do
   Service, então um CPF inválido entra — mas se o professor abrir um desses
   clientes na tela e salvar, a validação da RN0023 recusa e a demonstração
   trava. CPF válido é barato agora e caro na hora da apresentação.

2. **Acrescentar 18 clientes sintéticos** ao `data.sql`, antes do bloco
   `demo_chat_compras`. Manter os clientes **1 e 3** como os dois que o
   professor vai ver nos cenários 2 e 3. `cpf` e `email` são `UNIQUE`: usar
   valores determinísticos, não `Date.now()` — o `data.sql` precisa ser
   idempotente, porque o `vendas:preparar` o recarrega.

3. **Dar endereço a cada cliente novo.** `pedido.endereco_entrega_json` é
   `NOT NULL` e o `INSERT` do pedido copia o endereço do cliente com
   `json_object`. Cliente sem endereço faz a subconsulta devolver `NULL` e a
   carga inteira falha na restrição.

4. **Variar os perfis de compra** entre os 18. Alguns comprando em uma só
   categoria, outros em duas, um ou dois sem compra nenhuma. Sem variação não há
   o que aprender: se todos comprarem igual, os coeficientes não se distinguem.

5. **Dar preferência declarada a parte deles, e a outros não.** Hoje só o
   cliente 1 tem. A feature `peso_declarado` precisa variar entre as amostras,
   senão seu coeficiente fica indeterminado. Toda linha nova com `vigente_de`
   **anterior à `data_corte`**, ou vaza informação futura.

6. **Terceira onda de compras, entre 11/11 e 17/11**, para os clientes 1 e 3 e
   para parte dos sintéticos. É o que enche a janela de validação. Acrescentar
   as linhas em `demo_chat_compras` — o resto da carga se monta sozinho.

7. **Terceira onda de feedback**, também entre 11/11 e 17/11, em
   `demo_chat_mensagens`, com votos positivos e negativos misturados. Lembrar
   que a lista de ISBNs preenche o `recomendacao_mensagem`: voto sem ISBN não
   se liga a categoria nenhuma e não vira feature.

8. **Nenhum pedido muda de status depois do corte** — é a mitigação da
   limitação 6.2.

9. **Conferir as três janelas por consulta**, antes de rodar qualquer teste:
   contar eventos em `< 01/11`, em `[01/11, 11/11)` e em `[11/11, 18/11)`. As
   três têm de ter dados para o cliente 1 **e** para o 3. Se a terceira vier
   vazia, o resto da etapa é perda de tempo.

10. **Rodar o JUnit.** Os testes 2, 4 e 5 do recomendador dependem da massa
    (`clientesUmETresRecebemListasDiferentes`, `votoNegativoReduzScoreSemRetreinar`,
    `dunaNaoAparecePorqueEstaSemEstoque`) e são os primeiros a acusar erro.

11. **Apagar os dois `.db`, subir e rodar o Cypress inteiro.** Pela seção 8 não
    deveria quebrar nada, mas é exatamente o tipo de previsão que se confirma
    rodando.

12. **Treinar e avaliar de verdade**, já em 18/11. Anotar `amostras`,
    `perda_inicial`, `perda_final` e `metrica_validacao` da versão gerada: são
    os números que você vai citar na arguição. Com 20 clientes, `amostras` deve
    dar **80**.

---

## 10. Etapa 3 — Integração com a IA generativa (≈4 h)

**Por que primeiro a integração e não a tela:** a tela sem provedor não tem o
que mostrar, e o provedor sem tela se testa por JUnit. Esta ordem deixa cada
peça verificável sozinha.

13. **`pom.xml`:** `com.anthropic:anthropic-java:3.3.0`.

14. **Interface `ProvedorIA`**, com um método que recebe a conversa e as
    ferramentas disponíveis e devolve a resposta. É o que permite trocar Claude
    por Ollama sem o `ChatbotService` saber da diferença.

15. **`ClaudeProvedor`** — `AnthropicOkHttpClient.fromEnv()`, modelo
    `claude-opus-5-5`. O `fromEnv()` lê a `ANTHROPIC_API_KEY` do ambiente, e é
    por isso que a chave **nunca** entra no `application.properties`, que é
    versionado.

16. **`OllamaProvedor`** — HTTP em `localhost:11434`, modelo `qwen2.5:7b`. O
    modelo baixado declara capability `tools`, então o fallback também consegue
    usar as três ferramentas, não só conversar.

17. **`ProvedorComFallback`** — tenta o Claude; em falha de credencial ou de
    rede, cai para o Ollama e **registra qual provedor atendeu**. Esse registro
    é o que te deixa provar na apresentação que a resposta veio do Claude.

18. **Chaves no `application.properties`:** provedor principal, modelo, limite
    de tokens. Nunca a chave de API.

19. **JUnit do fallback**, com o provedor principal lançando exceção de
    propósito. Testar o fallback só desligando a rede na hora não é teste.

20. ⚠️ **Subir a aplicação pelo seu terminal** para exercitar o Claude de
    verdade. Os shells que eu abro não herdam a `ANTHROPIC_API_KEY`; os seus
    herdam.

→ fecha o **cenário 1**

---

## 11. Etapa 4 — O chatbot (≈5 h)

21. **Três ferramentas via `Tool.builder()`:** `recomendar_livros` (chama o
    `RecomendacaoService`), `buscar_livros` e `detalhar_livro`. É aqui que o
    modelo treinado entra na conversa: o LLM não recomenda por conta própria,
    ele **consulta** o recomendador. Essa separação é a resposta à pergunta
    "onde está o treinamento?".

22. **`ChatbotService`** — laço de tool use: envia a conversa, executa cada
    ferramenta pedida, devolve o resultado, repete até `stop_reason` ser
    `end_turn`. Grava `mensagem_chat` e, quando houve recomendação,
    `recomendacao_mensagem`.

23. **`ChatbotController`** — `POST /chat` e `POST /chat/{mensagemId}/feedback`.
    Sem regra: a validação fica no Service.

24. **Substituir o fragmento estático** em `fragmentos/layout.html:69` por
    histórico real, campo que envia e **útil / não útil** por resposta.

25. **Corrigir o erro do protótipo:** `layout.html:111` diz *"O Poder do Hábito,
    de J.R.R. Tolkien"* — o autor é Charles Duhigg. O fragmento aparece em 10
    telas do cliente.

26. **Remover o box "Recomendados para você"** de `catalogo.html:35`.
    Recomendação fora do chat está **fora de escopo** pelo item 6 do enunciado,
    e deixar o box sugere uma funcionalidade que não existe.

27. **Tela de preferências no perfil do cliente**, gravando em
    `preferencia_cliente` com `vigente_de` novo a cada alteração — nunca
    `UPDATE`. É o critério (e): a preferência é obtida **declarada pelo cliente
    e inferida do histórico**, e o histórico de alterações é o que permite
    treinar sem vazar o futuro.

28. **`ProvedorIAFalso`**, ativado por propriedade, para os testes. Suíte que
    depende de rede e de crédito não é suíte.

→ fecha os **cenários 4, 5, 6 e 7**

---

## 12. Etapa 5 — Retreino contínuo (≈2 h)

**Por que nunca cortar esta etapa:** é o cenário 8 e a prova do critério (c).
Sem ela, "o modelo é treinado" é afirmação sem demonstração.

29. **`POST /admin/recomendacao/treinar`** — relê vendas e feedback e grava uma
    versão nova, exigindo perfil ADMIN via `exigirAdministrador()`.

30. **Tela `/admin/recomendacao`** — versões com data, as três janelas,
    amostras, perdas, `metrica_validacao` e os coeficientes da versão vigente.
    É a tela que você abre na apresentação para mostrar os números.

31. **JUnit:** um feedback novo aparece na versão seguinte, e a versão anterior
    fica preservada. Versionar sem provar que a anterior sobrevive não é
    versionar.

→ fecha o **cenário 8**

---

## 13. Etapa 6 — Testes e documentação (≈3 h)

32. **Task `chatbot:preparar`** no `cypress.config.js`, com a mesma trava de
    porta 8081 que as outras usam.

33. **Sete specs em `cypress/e2e/chatbot/`**, uma por cenário, nomeadas
    `rnf0044-...`, com o `describe` começando pelo identificador.

34. **Justificativa da escolha da IA** (critério b) e **decisão sobre como as
    preferências são obtidas** (critério e), por escrito.

35. **As quatro limitações da seção 6**, escritas para você declarar antes que
    perguntem.

36. **`docs/validacao-ia.md`** — requisito do cliente, e está parado em 07/11.
    Esta revisão rende um registro honesto: o esquema foi corrigido certo, mas o
    banco de teste ficou defasado e teria escondido 19 specs quebradas até a
    véspera.

37. **`docs/roteiro-apresentacao-chatbot.md`** com os 8 cenários e as perguntas
    de arguição.

---

## 14. Definição de pronto

- [ ] Os 8 cenários demonstráveis, cada um com teste automatizado
- [ ] As duas suítes verdes **com os dois `.db` recriados do zero**
- [ ] `amostras = 80` na versão treinada, e `metrica_validacao` preenchida
- [ ] O Claude respondendo de verdade — confirmado no registro do provedor
- [ ] O fallback testado com a rede desligada
- [ ] As quatro limitações escritas no roteiro
- [ ] Ensaio completo: apagar `e2e.db`, subir pelo seu terminal, rodar as specs
      do chatbot, abrir o chat e fazer três perguntas livres
- [ ] `git push` com tudo commitado

---

## 15. Cronograma — 18 a 25/11

| Dia | Etapas | Horas | Cenários fechados |
|---|---|---|---|
| 18/11 | 2.5 e 3 | 6,5 | 1 |
| 19/11 | 4 | 5 | 4, 5, 6 e 7 |
| 20/11 | 5 e 6 | 5 | 8 |
| 21 a 24/11 | folga e ensaio | — | — |
| 25/11 | **apresentação** | | |

≈16,5 h. **A ordem é obrigatória até a Etapa 4:** o chatbot não tem o que
recomendar antes de o modelo existir — e o modelo não tem o que aprender antes
da Etapa 2.5.

Os cenários 2 e 3 já estão fechados pela Etapa 2, mas só valem a afirmação
depois da Etapa 2.5: com 8 amostras, "clientes diferentes recebem listas
diferentes" é coincidência, não aprendizado.

**Se atrasar**, a ordem de corte:
1. A tela de preferências do passo 27 — derivar tudo do histórico e declarar
   como limitação, ajustando a resposta do critério (e)
2. A janela de validação — a limitação 6.4 passa a valer e a afirmação encolhe
3. **Nunca a Etapa 5**, que é o cenário 8 e o critério (c)

---

## 16. A providenciar, em paralelo

- [x] Chave em `console.anthropic.com` com crédito
- [x] Ollama instalado com modelo baixado — `qwen2.5:7b`, com capability `tools`

---

## 17. Perguntas em aberto para o professor

1. O critério (c) exige treinamento ou ajuste fino. A arquitetura da seção 1
   treina um recomendador com vendas e feedback e usa a IA generativa apenas na
   camada de conversa. Atende?
2. **RNF0044 está duplicado no DRS** — nomeia a formatação monetária do eixo do
   gráfico gerencial e a recomendação com IA generativa.
