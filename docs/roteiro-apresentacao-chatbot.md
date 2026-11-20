# Roteiro — Apresentação do Chatbot de Recomendação com IA Generativa

**RNF0044 · 25/11/2026 · presencial, com uso do sistema pelo professor**

Aluno: Gustavo Vinícius Bonifácio (RA 1840482423023).
Professor/cliente: Rodrigo Rocha Silva.

> Este roteiro é para ser lido **antes** e consultado **durante**. A seção 1 é a
> única que não pode ser pulada: sem ela a demonstração sobe sem modelo treinado
> ou com um dublê de IA no lugar do Claude.

---

## 1. Antes de entrar na sala

### 1.1 O ambiente errado derruba a entrega

**Nunca demonstrar com `npm run app:test`.** Esse comando passa
`--livraria.ia.principal=falso`, que sobe o `ProvedorIAFalso` — um dublê usado
pelos testes. O professor conversaria com um simulador, e os critérios (b) e
(c) cairiam junto.

A demonstração é sempre:

```
npm run app          →  porta 8080, banco livraria.db, Claude de verdade
```

**Subir pelo seu próprio terminal.** A `ANTHROPIC_API_KEY` vive como variável de
ambiente do usuário do Windows, e um terminal aberto por outro processo não a
herda.

### 1.2 Banco recém-criado não tem modelo

`modelo_recomendacao` nasce vazio. Sem treinar, `pontuarCategorias` chama
`exigirModelo()` e falha, a ferramenta `recomendar_livros` devolve erro, e o
chatbot responde explicando a limitação em vez de recomendar.

Ordem obrigatória se o banco for recriado:

1. `npm run app`
2. Trocar o perfil para **Administrador** no menu superior e clicar **Aplicar**
3. Abrir **`/admin/recomendacao`** e clicar **Treinar nova versão**
4. Confirmar que a linha aparece como **Vigente** com **80 amostras**

Deixe o banco já treinado na véspera e **guarde uma cópia do `livraria.db`**.
É a sua restauração de um segundo se a massa for alterada durante os testes.

### 1.3 Checagem de rede — o certificado

O antivírus pode interceptar o TLS e derrubar o Claude para o Ollama sem aviso.
Rodar antes:

```powershell
$t=[Net.Sockets.TcpClient]::new('api.anthropic.com',443); $s=[Net.Security.SslStream]::new($t.GetStream()); $s.AuthenticateAsClient('api.anthropic.com'); "Emissor: " + $s.RemoteCertificate.Issuer; $s.Close(); $t.Close()
```

Tem que dizer **`Google Trust Services`**. Se disser **`AO Kaspersky Lab`**,
excluir `api.anthropic.com` da verificação de conexões criptografadas antes de
começar.

### 1.4 Lista de verificação

- [ ] `java -version` responde 21
- [ ] Emissor do certificado é `Google Trust Services`
- [ ] `npm run app` subiu, sem `ERROR` nem `WARN` no boot
- [ ] Modelo treinado, **Vigente**, 80 amostras
- [ ] Cópia de segurança do `livraria.db` treinado, fora da pasta do projeto
- [ ] Ollama no ar (`ollama list` mostra `qwen2.5:7b`) — é a rede de segurança
- [ ] Duas ou três perguntas livres já testadas no chat, com `parada=FIM` no log

---

## 2. Sequência da apresentação

| # | O quê | Onde | Minutos |
|---|---|---|---|
| 1 | Abrir o catálogo e o chat; explicar em uma frase que **o LLM não recomenda, ele consulta o modelo treinado** | `/` | 1 |
| 2 | Pergunta livre de **busca**: *"vocês têm algo do Machado de Assis?"* | chat | 1 |
| 3 | Pergunta de **dúvida**: *"o Neuromancer está disponível?"* | chat | 1 |
| 4 | **Recomendação personalizada**: escolher um cliente da massa e pedir *"me recomende um livro"* | chat | 2 |
| 5 | Marcar **útil / não útil** na resposta recomendada — critério (d) | chat | 1 |
| 6 | Abrir as **preferências declaradas** no perfil do cliente — critério (e) | `/clientes/{id}` | 2 |
| 7 | Abrir **`/admin/recomendacao`** e mostrar as janelas, as amostras e as perdas — critério (c) | admin | 3 |
| 8 | **Treinar nova versão** ao vivo e mostrar que a anterior foi preservada | admin | 2 |
| 9 | Declarar as quatro limitações da seção 7 | — | 2 |

Total ≈ 15 minutos, deixando espaço para o professor usar o sistema.

---

## 3. Critério do enunciado → onde é demonstrado

| Critério | Onde | Prova automatizada |
|---|---|---|
| **(a)** Chatbot de recomendação funcionando | Chat em todas as telas do cliente | `cypress/e2e/chatbot/` (4 specs) |
| **(b)** Escolha da IA generativa, justificada | Seção 5.1 deste roteiro | — |
| **(c)** Modelo **treinado** com dados de vendas e feedback | `/admin/recomendacao` | `RecomendacaoServiceTest`, `RetreinoRecomendacaoTest` |
| **(d)** Feedback útil / não útil no chat | Botões sob cada resposta | `rnf0044-recomendacao-personalizada.cy.js` |
| **(e)** Como as preferências do cliente são obtidas | Seção 5.3 · tela de preferências | `rnf0044-preferencias-declaradas.cy.js` |

**Fora de escopo pelo item 6 do enunciado:** recomendação fora do chat. Por isso
não existe vitrine de "recomendados para você" no catálogo — a ausência é
deliberada, e vale dizer isso antes que seja lido como falta.

---

## 4. O argumento central — critério (c)

É aqui que a entrega se ganha ou se perde. A pergunta que o professor vai fazer
é **"onde está o treinamento?"**, porque mandar dados no contexto do prompt não
conta.

### 4.1 A separação que responde a pergunta

```
Cliente  →  ChatbotService  →  Claude (decide O QUE perguntar)
                                  ↓ tool use
                            recomendar_livros
                                  ↓
                          RecomendacaoService  →  modelo treinado
                                                  (regressão logística)
```

**O LLM não escolhe os livros.** Ele decide *quando* pedir recomendação e
redige a resposta; quem ordena as categorias é o modelo treinado por descida de
gradiente, cujos coeficientes estão gravados no banco. A instrução do sistema é
explícita: *"Para recomendações, SEMPRE chame `recomendar_livros`: o modelo
treinado escolhe os livros."*

### 4.2 Os números a mostrar na tela

Valores da versão treinada em 19/11 sobre a massa atual:

| Medida | Valor | Como defender |
|---|---|---|
| Amostras | **80** | 20 clientes × 4 categorias — mais amostras que os 11 parâmetros |
| Perda inicial | **0,6931** | é exatamente `ln 2`: o que se espera de um modelo que começa chutando 0,5 |
| Perda final | **0,0641** | a otimização convergiu |
| Brier (validação) | **0,0630** | contra **0,25** de um modelo que chuta 0,5 |

**O número que importa é o Brier.** A perda de treino cair só prova convergência
naquela massa; a métrica de validação é calculada na terceira janela, que
**nunca ajusta os pesos**. Ela estar próxima da perda de treino (0,063 contra
0,064) é o sinal de que o modelo não decorou.

### 4.3 As três janelas, e por que existem

```
features   : eventos anteriores a 01/11
rótulo     : eventos de 01/11 a 10/11
validação  : eventos de 11/11 a 17/11   — avaliada, nunca ajustada
```

Sem separação temporal haveria **vazamento de alvo**: se o voto entra como
feature e "avaliou como útil" entra no rótulo, o modelo copia a resposta em vez
de aprender preferência. As três datas ficam gravadas em `modelo_recomendacao`,
porque sem `fim_observacao` registrado o mesmo corte produziria outros rótulos
mais tarde — a extração deixaria de ser reproduzível.

A massa: **21 clientes, 49 pedidos, 69 itens, 11 preferências declaradas e 119
mensagens de chat**, com compras de 10/10 a 14/11, distribuídas para que as três
janelas tenham eventos.

---

## 5. Decisões a explicar

### 5.1 Por que Claude — critério (b)

| Motivo | Detalhe |
|---|---|
| **Tool use nativo e confiável** | A arquitetura depende de o modelo chamar `recomendar_livros` em vez de inventar livros. Sem tool use robusto, a separação da seção 4.1 não se sustenta |
| **SDK Java oficial** | `com.anthropic:anthropic-java`, sem camada de terceiros entre a aplicação e a API |
| **Alternativa avaliada e mantida como reserva** | Ollama com `qwen2.5:7b`, local. Declara capability `tools`, mas responde em ~12 s contra ~5 s. Ficou como **fallback**, não como principal |

A troca é por propriedade (`livraria.ia.principal`), então a escolha é
reversível e demonstrável — não está soldada no código.

### 5.2 O que o feedback significa

Um botão com dois sentidos possíveis é ambíguo. O escopo foi **restringido**:

| A mensagem recomendou livros? | O que o voto significa |
|---|---|
| **Sim** (há linhas em `recomendacao_mensagem`) | sinal de preferência pelas **categorias daqueles livros** — participa do treinamento |
| **Não** (busca, dúvida, conversa) | avaliação da resposta — é **gravado**, mas não treina preferência |

Resposta pronta para *"e se o cliente só não gostou de como a resposta foi
escrita?"*: só é lido como preferência quando a mensagem de fato recomendou
livros. Os dois casos ficam gravados e distinguíveis.

### 5.3 Como as preferências são obtidas — critério (e)

Por **duas fontes**, deliberadamente:

1. **Declarada** — o cliente marca categorias no próprio perfil, gravadas em
   `preferencia_cliente`.
2. **Inferida** — do histórico de compras e dos votos no chat.

A tabela é **append-only**: cada alteração grava uma linha nova com `vigente_de`
novo, nunca um `UPDATE`. Isso não é capricho — é o que permite treinar sem
vazar o futuro. A extração usa a linha vigente **na data de corte**, não a
atual. Sem o histórico, uma preferência marcada hoje contaminaria o treino de
uma janela de duas semanas atrás.

---

## 6. O professor vai usar o sistema

### 6.1 O que ele pode fazer sem quebrar nada

Navegar, buscar, abrir detalhes, conversar com o chat, votar útil/não útil,
alterar preferências, cadastrar-se, comprar. Tudo isso grava no `livraria.db` e
**não afeta o modelo já treinado** — o modelo só muda quando alguém clica em
*Treinar nova versão*.

### 6.2 O que muda os números da tela de treino

Se ele treinar uma versão nova, a anterior **é preservada** — isso é parte da
demonstração, não um acidente. Mas eventos criados hoje (25/11) caem **fora**
das três janelas, que terminam em 17/11. Uma versão treinada com as mesmas datas
produzirá os mesmos 80 registros.

Vale dizer isso em voz alta antes que ele estranhe: *"o feedback que o senhor
acabou de dar só entra no treino se a data dele estiver dentro das janelas
escolhidas"*. É a mesma frase que está escrita na tela.

### 6.3 Restaurar, se precisar

Parar a aplicação, restaurar a cópia do `livraria.db` e subir de novo — três
segundos. Em último caso, apagar o `livraria.db`, subir e **treinar outra vez**
(seção 1.2). Nunca esquecer o treino: é o passo que some.

### 6.4 Rodar a suíte na frente dele

Se quiser mostrar os testes verdes, dá para rodar **sem derrubar a
demonstração**: as duas suítes usam bancos diferentes. Num segundo terminal:

```
npm run app:test     →  8081, e2e.db   (noutro terminal)
npm run e2e:run      →  Cypress
```

O 8080 com o Claude continua intacto. Mas saiba que o `e2e.db` fica, depois da
suíte, só com os quatro livros da massa de análise — não mostre o 8081 como se
fosse o sistema.

---

## 7. Declarar antes que perguntem

**7.1 Ausência de interação é rótulo assumido.** `y = 0` quando não há compra
nem voto útil na janela futura significa **ausência de sinal positivo
observado**, não rejeição comprovada. A massa controlada permite testar a
interpretação, não validá-la como verdade.

**7.2 O evento temporal da compra.** `pedido` só tem `data_pedido`, sem datas de
transição de status. A compra contabilizada é `data_pedido` dentro da janela
**e** status entre `APROVADA`, `EM TRANSPORTE` ou `ENTREGUE` — o mesmo conjunto
da RN0074, já usado na análise de vendas. Como o status é lido na extração, um
pedido feito antes do corte e aprovado depois apareceria como compra anterior ao
corte; a massa foi construída para que isso não ocorra. A correção completa
seria datas de transição em `pedido`, como `troca` já tem — fora do escopo desta
entrega, e é a mesma lacuna que a entrega de análise declarou.

**7.3 O sinal do coeficiente não é garantido.** A regressão sem restrições não
garante que o coeficiente de votos não-úteis seja negativo em qualquer massa.
Nesta entrega o sinal é verificado como **propriedade empírica** do modelo
treinado. Garanti-lo exigiria uma restrição na otimização, que não foi adotada
para não impor a conclusão que o treino deveria descobrir.

**7.4 Perda de treino não é capacidade de previsão.** A perda cair comprova
convergência naquela massa. A afirmação de que o modelo prevê comportamento
futuro se apoia na **métrica de validação** da terceira janela, que não participa
do ajuste.

---

## 8. Perguntas de arguição prováveis

**"Onde exatamente está o treinamento? Isso não é só prompt?"**
Em `RecomendacaoService`: descida de gradiente sobre regressão logística, em
Java puro. Os coeficientes, o intercepto, os vieses por categoria e os
parâmetros de normalização ficam gravados em `modelo_recomendacao`,
`coeficiente_modelo` e `vies_categoria`. O LLM nunca vê esses números — ele
recebe a lista de livros já ordenada.

**"Por que regressão logística e não uma rede neural?"**
Com 80 amostras e 11 parâmetros, modelo maior decora. E os coeficientes são
interpretáveis: dá para abrir a tela e explicar o que cada um significa, que é
o que uma arguição exige.

**"O que acontece se a API cair no meio da apresentação?"**
Cai para o Ollama local automaticamente, e o log registra o provedor que
atendeu. A resposta sai mais devagar, mas sai. Se os dois falharem, o
`ChatbotService` grava a falha e responde com aviso — não quebra a tela.

**"Por que o chat recusa uma resposta em vez de mostrar o que veio?"**
Decisão consciente: resposta truncada no meio de uma recomendação é pior que um
erro honesto. O `ChatbotService` só aceita `Parada.FIM`; `LIMITE` vira erro.

**"Como sei que o modelo aprendeu e não decorou?"**
Pela terceira janela. O Brier de 0,063 é calculado sobre eventos que não
ajustaram peso nenhum, e está próximo da perda de treino. Se tivesse decorado, a
perda de treino seria baixa e a de validação alta.

**"O voto negativo prova que o cliente não gosta da categoria?"**
Não, e isso está declarado na limitação 7.1. É sinal de preferência, não prova
de desinteresse.

**"Por que não tem recomendação no catálogo?"**
Está fora de escopo pelo item 6 do enunciado.

**"Onde ficam as regras de negócio?"**
Todas no Service. O Controller escolhe template, o Repository só tem SQL, o
template não acessa banco. Não há trigger nem procedure — as restrições `CHECK`
e `UNIQUE` do schema são rede de integridade, e a regra equivalente existe
também no Service.

---

## 9. Se algo der errado ao vivo

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| Chat responde "não foi possível concluir" | resposta truncada (`parada=LIMITE`) | repetir com pergunta mais curta; está declarado na seção 8 |
| Resposta demora ~12 s | caiu para o Ollama | dizer que é o fallback funcionando — é demonstração, não falha |
| Chat diz que não conseguiu recomendar | **modelo não treinado** | seção 1.2, treinar em 20 segundos |
| Nada responde | chave ausente no terminal | subiu do terminal errado; parar e subir do seu |
| Tela de treino recusa as datas | janela de observação ainda aberta | usar as datas padrão (01/11 → 10/11 → 17/11) |

---

## 10. Pendências conhecidas, a declarar se perguntado

- **Sem autenticação**, dispensada pelo cliente. O perfil de administrador é
  simulação de sessão, do mesmo tipo da `OperadoraSimulada` já autorizada. O
  "usuário responsável" do log de transações é valor fixo — lacuna registrada
  no CDU02.
- **CVV é capturado e não é persistido**, por PCI DSS. A RN0024 exige o campo,
  não o armazenamento.
- **O cenário de fallback não tem spec automatizada**: depende de derrubar a
  rede, o que a suíte não faz. É verificado manualmente.
- **RNF0044 está duplicado no DRS**, nomeando tanto a formatação monetária do
  eixo quanto a recomendação com IA. Já registrado como pergunta ao professor na
  entrega de análise.

---

## 11. Estado verificado em 19/11/2026

| | |
|---|---|
| JUnit | 117 testes, 0 falhas |
| Cypress | 60 de 60 specs |
| Specs de chatbot | 4, em `cypress/e2e/chatbot/` |
| Modelo | 80 amostras · 0,6931 → 0,0641 · Brier 0,0630 |
| Provedor | `claude-opus-5-5`, confirmado no log como `IA atendida provedor=CLAUDE` |
| Reserva | Ollama `qwen2.5:7b`, exercitada e funcional |
