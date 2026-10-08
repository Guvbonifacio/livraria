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
