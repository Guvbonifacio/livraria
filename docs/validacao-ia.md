# Registro de validações com Inteligência Artificial

Requisito do cliente: utilizar IA para validação do trabalho produzido.
Ferramenta: **Claude Code** (Anthropic), usada como apoio ao desenvolvimento e como
revisora de código e documentação.

> Este arquivo registra **o que foi revisado, o que a IA apontou e o que foi decidido** —
> inclusive quando a sugestão não foi seguida. Atualizar a cada revisão.

---

| Data | Artefato revisado | Principais apontamentos | Decisão |
|---|---|---|---|
| 17/08/2026 | Documento de tema e tecnologias | Escopo descrevia o trabalho acadêmico, não o produto; faltavam categorias de livro e escopo negativo; justificativas sem alternativa descartada; chatbot e recomendação tratados como um só requisito | Aceito. Documento reescrito com descrição do produto, categorias definidas e os dois módulos de IA separados |
| 17/08/2026 | Documento de tema e tecnologias (2ª passada) | JUnit ausente da tabela; justificativa do Planner faltando; "Thymelead" grafado errado; estilo Subtítulo em vez de Título 2 | Aceito. Correções aplicadas antes da entrega |
| 27/08/2026 | Tela de análise de vendas | Nome do arquivo divergia do retorno do Controller (erro 500); dados do gráfico escritos em JavaScript, violando a proibição de regra de negócio no front-end; rótulos semanais em vez de mensais (RN0071) | Aceito. Arquivo renomeado, dados movidos para o Controller via `SerieVendas`, rótulos corrigidos para mês/ano |
| 27/08/2026 | Varredura geral dos templates | Bootstrap JS ausente em 12 telas, deixando 8 modais e collapses inoperantes; chatbot presente nas telas do admin e ausente nas do cliente | Aceito. Script movido para o fragmento `cabeca`; chatbot realocado para as telas do cliente |
| 01/09/2026 | Modelo de domínio | `Livro.categoria` como texto, incompatível com a RN0012 (muitos-para-muitos); `status` como `String` em `Pedido` e `Troca`, sem restrição de valores | Registrado como evolução prevista no diagrama de classes e no DVP. Correção adiada para a etapa de banco de dados |
| 20/09/2026 | `ClienteService` e `ClienteRepository` | CPF validado sem pontuação mas gravado e consultado com pontuação, abrindo furo na unicidade; e-mail sem normalização | Aceito. Normalização de CPF e e-mail aplicada no início de `cadastrar` e `alterar` |
| 20/09/2026 | Suíte Cypress | Asserção final do RF0021 passava tanto no sucesso quanto no erro, não provando nada; nomes de arquivo sem o identificador do requisito | Aceito. Asserções trocadas por verificação de URL e ausência de alerta; arquivos renomeados com o código do RF/RNF |
| 20/09/2026 | Fluxo de alteração (RF0022) | Risco de corromper a senha caso o `UPDATE` incluísse `senha_hash`; unicidade de CPF colidiria com o próprio registro na alteração | Aceito. `UPDATE` não menciona `senha_hash`; `existePorCpf` e `existePorEmail` passaram a ignorar o próprio id |
| 30/09/2026 | Especificação do CDU02 – Gestão de Vendas | Operadora de cartão de crédito ausente da seção de atores, embora acionada pela RN0037 | Aceito. Incluída como ator secundário, com registro de que o sistema em especificação não é ator de si mesmo |
| 20/10/2026 | Tela de perfil do cliente (regressão apontada pelo Cypress) | O teste `rnf0034` falhou ao não encontrar `endereco-nome`: a reescrita do perfil no commit `23fcafe` removeu a exibição do apelido e do tipo do endereço, deixando o cliente sem como distinguir os endereços nem conferir a RN0021/RN0022 | Aceito. Apelido e tipo repostos em `cliente-perfil.html`. Varredura dos demais seletores `data-cy` confirmou que só resta `usar-novo-cartao`, já registrado como DF001 |
| 20/10/2026 | Suíte Cypress completa e ambiente de execução | Dois defeitos silenciosos: o `PagamentoService` tinha duas redações diferentes para a mesma RN0034, quebrando o teste `rn0034` desde o commit `00e7560`; e o Tomcat anexava `;jsessionid` à URL enquanto não confirmava o suporte a cookies, quebrando o `rf0028` e expondo o identificador de sessão em histórico, logs e `Referer` | Aceito. Mensagem unificada na constante `MINIMO_POR_CARTAO`; `server.servlet.session.tracking-modes=cookie` no `application.properties`. Ambiente padronizado em 8081/`e2e.db` para que a suíte inteira rode com um comando |
| 20/10/2026 | Etapa 2 — processamento do pagamento | Falha intermitente em `rf0027` e `rf0028`: os testes digitavam no modal do Bootstrap durante a animação de *fade* e perdiam caracteres, quebrando a suíte só na execução completa. A tela do administrador não tinha nenhum atributo `data-cy`, impedindo testar o processamento. Revisão da RN0032 contra o DRS mostrou que a regra exige três ações no checkout (notificar, atualizar quantidade, remover indisponíveis) e não a recusa que estava implementada no processamento | Aceito. Asserção `have.css opacity 1` antes de interagir com cada modal; `data-cy` acrescentados em `admin-pedido-detalhe.html`; cenários 1 e 5 escritos em Cypress. A RN0032 foi registrada no checklist com o texto literal do DRS e separada em dois ajustes, por ocorrerem em momentos distintos |
| 20/10/2026 | Etapa 3 — despacho e entrega | O atributo `data-cy="status-pedido"` foi colocado no badge de status do **pagamento**, dentro do laço de pagamentos, e não no status do pedido: os testes `rf0039` e `rf0040` liam `PENDENTE`/`APROVADO` e falhavam. A tela ficou ainda com dois ganchos de nomes quase iguais (`pedido-status` e `status-pedido`) para a mesma informação | Aceito. Gancho único `status-pedido` no status do pedido, expondo o valor do DRS em `data-status` para que o teste verifique o status literal sem depender do rótulo exibido; o badge do pagamento passou a `status-pagamento`. Os quatro arquivos de teste foram alinhados ao gancho único |

---

## Observações sobre o uso da ferramenta

- A IA foi utilizada como **revisora e apoio ao aprendizado**, não como geradora de código
  pronto: cada etapa foi implementada pelo autor e submetida a revisão em seguida.
- Nem toda sugestão foi aceita de imediato. As correções do modelo de domínio
  (categorias e `enum` de status) foram **conscientemente adiadas** para a etapa de banco
  de dados, por não afetarem as entregas em curso.
- As revisões apontaram defeitos que não apareciam em execução — como os modais
  inoperantes e a asserção de teste que passava indevidamente — o que reforça o valor da
  revisão sobre a verificação manual.
