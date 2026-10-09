# Livraria Estante do Saber

E-commerce de livros — projeto avaliado da disciplina de Laboratório de Engenharia de
Software, FATEC Mogi das Cruzes, 2º semestre de 2026.
Aluno: Gustavo Vinícius Bonifácio (RA 1840482423023). Professor/cliente: Rodrigo Rocha Silva.
**Desenvolvimento individual.**

## Fonte da verdade

`DRS_LES_2_2026.docx` (v0.7) é a **única referência válida**. Nada de requisito, estado,
campo ou comportamento que não esteja nele. Lacunas e contradições são **registradas**,
não resolvidas por suposição.

Cópia em `C:\Users\Gusta\OneDrive\Documentos\ProjetoLES\DRS_LES_2_2026.docx`.

⚠️ Ao extrair texto do DRS, conferir o grupo "Regras de Negócio → Cadastro de Clientes"
(RN0021 a RN0028). Extrações automáticas já falharam em ler essas linhas da tabela.

## Stack

Java 21 · Spring Boot 4 · Thymeleaf · Bootstrap 5 (local, sem CDN) · SQLite via
JdbcTemplate (sem ORM) · Cypress (E2E) · Maven Wrapper.

## Arquitetura — restrições do cliente

MVC em camadas, obrigatório:

```
controller/  → recebe requisição, aciona service, escolhe template. NÃO valida.
service/     → TODAS as regras de negócio e validações. Transações.
repository/  → apenas SQL via JdbcTemplate. Sem regra.
model/       → classes de domínio.
templates/   → Thymeleaf. Não acessa banco, não tem regra.
```

Proibido pelo cliente: acesso a banco na camada de apresentação; regra de negócio no
front-end **ou no banco** (nada de trigger/procedure). Restrições `CHECK`/`UNIQUE` no
schema são rede de segurança de integridade — a regra equivalente **também** existe no
Service.

Sem autenticação (dispensada pelo cliente). O "usuário responsável" do log é um valor
fixo — lacuna registrada no CDU02.

## Convenções

- **Status seguem o DRS**, sempre: `EM PROCESSAMENTO`, `APROVADA`, `REPROVADA`,
  `EM TRANSPORTE`, `ENTREGUE`, `EM TROCA`, `TROCA AUTORIZADA`, `TROCADO`.
  Não existem `EM ABERTO`, `PAGAMENTO REALIZADO`, `EM TRÂNSITO`, `CANCELADO`,
  `ENVIADO`, `ITEM ENVIADO`, `TROCA ACEITA/NEGADA`.
- Testes Cypress: um arquivo por requisito, nomeado `rfXXXX-...` / `rnfXXXX-...`, e o
  `describe` começa com o identificador. Cada teste prepara o próprio cenário
  (cadastra antes de agir) e usa `Date.now()` para CPF/e-mail únicos.
- Toda escrita registra log (RNF0012) a partir do **Service**, dentro da transação e
  **depois** do registro principal (o `registro_id` só existe após o INSERT).
- Senha: BCrypt via `spring-security-crypto` (só o módulo crypto — o Spring Security
  completo autoconfigura login, que o cliente dispensou). `UPDATE` de dados cadastrais
  nunca toca `senha_hash`.
- CVV é capturado (RN0024 exige o campo) mas **não é persistido** (PCI DSS).
- SQLite: sem tipo booleano nem data. `INTEGER` 0/1 com `CHECK`, datas em `TEXT` ISO.
  Chaves estrangeiras exigem `PRAGMA foreign_keys=ON` (já em `application.properties`).

## Comandos

```bash
./mvnw spring-boot:run      # Git Bash   (PowerShell: .\mvnw.cmd spring-boot:run)
npx cypress open            # com a aplicação rodando
```

Banco: `livraria.db` na raiz, recriado por `src/main/resources/schema.sql`. Para
recomeçar limpo: apagar o arquivo e subir a aplicação.

## Documentação

`docs/` contém diagrama de classes, diagramas do DVP, roteiro de apresentação do CRUD e
o registro de validações com IA (`validacao-ia.md` — **atualizar a cada revisão**, é
requisito do cliente).

## Como trabalhar com o autor

Ensino antes de entrega pronta. Conduzir em passos pequenos e numerados, explicando
*o que* e *por que* antes de cada passo, com verificação de entendimento ao final — o
professor cobra arguição do código. Só escrever o código inteiro quando ele pedir.
