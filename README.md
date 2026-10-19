# Livraria Estante do Saber

E-commerce de livros desenvolvido na disciplina de Laboratório de Engenharia de Software
— FATEC Mogi das Cruzes, 2º semestre de 2026.

Gustavo Vinícius Bonifácio — RA 1840482423023 · Prof. Rodrigo Rocha Silva

**Stack:** Java 21 · Spring Boot 4 · Thymeleaf · Bootstrap 5 · SQLite (JdbcTemplate) · Cypress

---

## Pré-requisitos

| Ferramenta | Versão | Conferir com |
|---|---|---|
| Git | qualquer | `git --version` |
| JDK | **21** | `java -version` |
| Node.js | LTS (20 ou superior) | `node -v` |
| Google Chrome | atual | — (usado pelo Cypress) |

Maven **não** precisa ser instalado: o projeto inclui o Maven Wrapper (`mvnw`).

## Instalação (uma vez, com internet)

```bash
git clone https://github.com/Guvbonifacio/livraria.git
cd livraria
npm install
```

O `npm install` baixa o Cypress (~200 MB) — leva alguns minutos.

## Executar a aplicação (desenvolvimento)

**PowerShell / Prompt do Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Git Bash / Linux / macOS:**
```bash
./mvnw spring-boot:run
```

Na primeira execução o Maven baixa as dependências do Spring (alguns minutos).
A aplicação está no ar quando o terminal mostrar `Started LivrariaApplication`.

Acesse: <http://localhost:8080>

O banco `livraria.db` é criado automaticamente na primeira execução a partir de
`src/main/resources/schema.sql`, e `src/main/resources/data.sql` carrega a massa de
demonstração (categorias, livros, estoque, bandeiras e cupons promocionais). Para
recomeçar com o banco vazio, pare a aplicação, apague o `livraria.db` e suba novamente.

## Executar os testes automatizados (Cypress)

⚠️ **Os testes exigem o ambiente dedicado: porta 8081 e banco `e2e.db`.** Rodando contra
a porta 8080, os testes de vendas falham de propósito — a rotina de preparação recusa
qualquer base que não seja a de testes, para não sujar os dados de desenvolvimento.

**1. Suba a aplicação no ambiente de testes** (em um terminal):

**PowerShell:**
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --spring.datasource.url=jdbc:sqlite:e2e.db"
```

**Git Bash:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --spring.datasource.url=jdbc:sqlite:e2e.db"
```

**2. Abra o Cypress** (em outro terminal):

```bash
npx cypress open --config baseUrl=http://localhost:8081
```

Escolha **E2E Testing** → **Chrome** → clique em um arquivo de teste.

Para rodar todos de uma vez, sem interface:

```bash
npx cypress run --config baseUrl=http://localhost:8081
```

Para recomeçar do zero, apague o `e2e.db` antes de subir a aplicação.

### Testes de cadastro de cliente — `cypress/e2e/`

| Arquivo | Requisito |
|---|---|
| `rf0021-cadastrar-cliente.cy.js` | RF0021 — Cadastrar cliente |
| `rf0022-alterar-cliente.cy.js` | RF0022 — Alterar cliente |
| `rf0023-inativar-cliente.cy.js` | RF0023 — Inativar cadastro |
| `rf0024-consultar-cliente.cy.js` | RF0024 — Consulta com filtro |
| `rf0027-cartao-preferencial.cy.js` | RF0027 — Cartões e preferencial |
| `rf0028-alterar-senha.cy.js` | RF0028 — Alterar apenas a senha |
| `rnf0012-log-transacao.cy.js` | RNF0012 — Log de transação |
| `rnf0031-senha-forte.cy.js` | RNF0031 — Senha forte |
| `rnf0032-confirmacao-senha.cy.js` | RNF0032 — Confirmação de senha |
| `rnf0034-alterar-enderecos.cy.js` | RNF0034 — Alterar apenas endereços |

### Testes do fluxo de vendas — `cypress/e2e/vendas/`

| Arquivo | Requisito |
|---|---|
| `rf0031-carrinho-multiplos-itens.cy.js` | RF0031, RF0032 — Carrinho |
| `rn0031-estoque-insuficiente.cy.js` | RN0031 — Validação de estoque |
| `rf0038-compra-dados-cadastrados.cy.js` | RF0033 a RF0038 — Compra completa |
| `rf0035-novo-endereco-e-cartao.cy.js` | RF0035, RF0036 — Novos dados na compra *(reprovado: DF001)* |
| `rn0034-multiplos-cartoes.cy.js` | RN0034 — Mínimo por cartão |
| `rn0035-cupom-com-cartao.cy.js` | RN0035 — Exceção ao mínimo |
| `rn0036-cupom-excedente.cy.js` | RN0036 — Cupom de troca do excedente |

## Estrutura

```
src/main/java/com/fatec/livraria/
├── controller/   recebe as requisições e escolhe a tela
├── service/      regras de negócio e validações
├── repository/   acesso ao banco (SQL via JdbcTemplate)
└── model/        classes de domínio
src/main/resources/
├── templates/    telas Thymeleaf
├── static/       css, js e imagens
└── schema.sql    criação das tabelas
cypress/e2e/      testes automatizados de interface
docs/             diagramas, roteiros e documentação
```
