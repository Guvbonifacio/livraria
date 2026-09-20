# Roteiro — Apresentação do CRUD de Cliente

Livraria Estante do Saber · Gustavo Vinícius Bonifácio · 21/09/2026

---

## 1. Antes de entrar na sala

- [ ] Apagar o `livraria.db` e subir a aplicação uma vez → banco limpo, só com o schema
- [ ] Aplicação rodando (`Started LivrariaApplication` no terminal)
- [ ] Cypress aberto (`npx cypress open`), na tela de lista de testes
- [ ] DB Browser aberto no `livraria.db`, aba *Browse Data*, tabela `cliente`
- [ ] VSCode aberto em `ClienteService.java` (é o arquivo da arguição)
- [ ] Este roteiro aberto
- [ ] Wi-Fi desligado ou não — o sistema não depende de internet

## 2. Sequência da apresentação

1. **Abrir este roteiro** e mostrar a tabela da seção 3: "cada teste valida um requisito do DRS, e o nome do arquivo já diz qual".
2. **Rodar os testes** no Cypress, um por um, na ordem da tabela. Enquanto o Chrome preenche sozinho, narrar o que está sendo provado.
3. Após o RF0021: **mostrar o DB Browser** — a linha nova, e a coluna `senha_hash` começando com `$2a$` (RNF0033).
4. Após o RF0023: apontar a última linha do teste — *o cliente continua na consulta*. Explicar: inativar é `UPDATE ativo = 0`, não `DELETE`, porque o RF0025 exige consultar as transações do cliente e excluir destruiria o histórico.
5. **Abrir o `ClienteService`** e mostrar a estrutura: valida tudo → acumula erros → lança → transforma → grava. Apontar o `@Transactional`.
6. **Declarar o que falta** (seção 4) antes que perguntem.

## 3. Requisito → teste

| Requisito do DRS | Arquivo de teste | O que o teste prova |
|---|---|---|
| **RF0021** Cadastrar cliente | `rf0021-cadastrar-cliente.cy.js` | Após salvar, redireciona para `/clientes/{id}` com id gerado e sem mensagem de erro |
| **RF0023** Inativar cadastro | `rf0023-inativar-cliente.cy.js` | Perfil exibe *Inativo* **e** o cliente continua aparecendo na consulta — inativado, não excluído |
| **RF0024** Consulta com filtro | `rf0024-consultar-cliente.cy.js` | Filtro por nome exibe o cliente certo e oculta os demais |
| **RF0026** Vários endereços com nome curto | dentro do RF0021 | Endereço "Casa" do tipo ENTREGA gravado na tabela `endereco` |
| **RF0027** Vários cartões, um preferencial | dentro do RF0021 | Cartão gravado com `preferencial = 1`; Service impede dois preferenciais |
| **RNF0031** Senha forte | `rnf0031-senha-forte.cy.js` | Senha `abc123` → mensagem de erro, nada gravado |
| **RNF0032** Confirmação de senha | `rnf0032-confirmacao-senha.cy.js` | Confirmação diferente → mensagem de erro, nada gravado |
| **RNF0033** Senha criptografada | DB Browser | Coluna `senha_hash` com BCrypt (`$2a$`, 60 caracteres) |
| **RNF0035** Código único | dentro do RF0021 | Id gerado pelo `AUTOINCREMENT`, visível na URL |
| Validação implícita: CPF único | dentro do RF0021 | Segundo cadastro com o mesmo CPF → "CPF já cadastrado" |

## 4. O que ainda não está implementado

Dizer isto **antes** de ser perguntado:

| Requisito | Situação | Previsão |
|---|---|---|
| **RF0022** Alterar cliente | Não implementado | Próxima semana |
| **RF0025** Consulta de transações | Seção existe no perfil; pedidos ainda não estão no banco | Entra com o módulo de vendas |
| **RF0028** Alterar apenas a senha | Não implementado | Próxima semana |
| **RNF0034** Alterar apenas endereços | Não implementado | Próxima semana |
| **RNF0012** Log de transação | Tabela `log_transacao` criada no schema; gravação não implementada | Próxima semana |

O DRS não possui regras de negócio (RN) específicas para o cadastro de cliente — o grupo está vazio no documento. As regras aplicadas são os RNFs acima mais as validações inerentes a qualquer cadastro: campos obrigatórios, CPF válido, CPF e e-mail únicos.

## 5. Perguntas prováveis e respostas

**"Onde está a regra de negócio?"**
No `ClienteService`. O Controller só coordena; o Repository só executa SQL; a View é um template que exibe o que recebeu. Nenhum dos três valida nada.

**"Por que inativar em vez de excluir?"**
O RF0023 diz inativar. E o RF0025 exige consultar as transações do cliente — excluir o cadastro destruiria o histórico de pedidos. Inativar é `UPDATE cliente SET ativo = 0`; o registro permanece.

**"O banco tem `UNIQUE` no CPF. Por que validar no Service também?"**
Três motivos: a mensagem do banco é ilegível para o usuário; o Service valida antes de gravar, o banco só reclama depois; e regra de negócio no banco é proibida. O `UNIQUE` é restrição de integridade — rede de segurança —, a regra mora no Service.

**"Para que serve o `@Transactional`?"**
Para que os três `INSERT`s — cliente, endereço, cartão — aconteçam juntos ou nenhum aconteça. Se o cartão falhar, o cliente e o endereço são desfeitos. Não tem relação com a lista de erros: a lista é para o usuário ver todos os problemas de uma vez; a transação é para o banco não ficar pela metade.

**"Como a senha é protegida?"**
BCrypt, via `spring-security-crypto`. Só o módulo de criptografia — o Spring Security completo se autoconfigura com tela de login, que o cliente dispensou. A senha é validada em texto (não dá para validar um hash) e criptografada logo antes de gravar.

**"Por que `?` no SQL?"**
Comando e dados viajam separados. O valor nunca é interpretado como parte do comando — é a proteção contra SQL injection.

**"O que esse teste prova?"** (sobre o RF0021)
Que após o clique houve redirecionamento para `/clientes/{id}` com um id numérico gerado, e que não há alerta de erro na tela. Se o cadastro falhasse, o formulário voltaria com o alerta e a URL não mudaria.

**"Por que a View não acessa o banco?"**
Porque não é código executável — é um template processado no servidor, que só enxerga os dados que o Controller colocou no Model. Não tem como buscar nada por conta própria.

**"Por que SQLite?"**
Requisito era banco relacional. SQLite dispensa servidor, é um arquivo único, roda em qualquer máquina sem instalação. Adequado ao porte. A limitação: não tem tipo booleano nem data — por isso `ativo` é `INTEGER` com `CHECK (ativo IN (0,1))`, e as chaves estrangeiras exigem `PRAGMA foreign_keys=ON`, que está no `application.properties`.

**"Por que o CVV não é gravado?"**
Padrão PCI DSS: o CVV prova posse física do cartão no momento da compra; armazená-lo anularia essa prova.

**"O Cypress grava no banco de verdade?"**
Sim, no `livraria.db`, com CPF e e-mail gerados a partir do relógio para nunca repetir. O próximo passo é um banco separado para testes, via *profile* do Spring.
