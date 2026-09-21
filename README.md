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

## Executar a aplicação

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
`src/main/resources/schema.sql`. Para recomeçar com o banco vazio, pare a aplicação,
apague o arquivo `livraria.db` e suba novamente.

## Executar os testes automatizados (Cypress)

Com a aplicação **rodando** em outro terminal:

```bash
npx cypress open
```

Escolha **E2E Testing** → **Chrome** → clique em um arquivo de teste. Os testes ficam em
`cypress/e2e/` e cada arquivo é nomeado pelo requisito que valida:

| Arquivo | Requisito |
|---|---|
| `rf0021-cadastrar-cliente.cy.js` | RF0021 — Cadastrar cliente |
| `rf0022-alterar-cliente.cy.js` | RF0022 — Alterar cliente |
| `rf0023-inativar-cliente.cy.js` | RF0023 — Inativar cadastro |
| `rf0024-consultar-cliente.cy.js` | RF0024 — Consulta com filtro |
| `rnf0031-senha-forte.cy.js` | RNF0031 — Senha forte |
| `rnf0032-confirmacao-senha.cy.js` | RNF0032 — Confirmação de senha |

Para rodar todos de uma vez, sem interface (gera vídeo e relatório no terminal):

```bash
npx cypress run
```

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
