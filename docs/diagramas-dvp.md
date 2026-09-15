# Diagramas do DVP — Livraria Estante do Saber

Fontes dos diagramas referenciados no Documento de Visão de Projeto.

**Como gerar as imagens:** abra <https://mermaid.live>, cole o conteúdo de um bloco
(sem as crases), e use **Actions → PNG** ou **SVG**. Depois insira a imagem no DVP no
lugar da marcação `«INSERIR AQUI A FIGURA N — ...»`, apagando a marcação.

Prefira **SVG** quando o Word aceitar: a imagem não perde qualidade ao ser ampliada.

> **Figura 2 (casos de uso):** o Mermaid não possui diagrama de casos de uso UML.
> O bloco abaixo é uma aproximação para conferência do conteúdo. Para a entrega,
> **redesenhe no draw.io** usando a notação correta — atores como bonecos, casos de
> uso como elipses, e os estereótipos `<<include>>` e `<<extend>>` nas setas
> tracejadas. O professor avalia a correção da notação.

---

## Figura 1 — Representação arquitetural do sistema

```mermaid
flowchart TB
    subgraph EST["Estação de trabalho"]
        NAV["Navegador<br/>HTML · CSS · Bootstrap 5"]
        subgraph APP["Aplicação Spring Boot 3 · Java 21"]
            direction TB
            APRES["<b>Camada de Apresentação</b><br/>Templates Thymeleaf<br/>pacote controller"]
            NEG["<b>Camada de Negócio</b><br/>pacote service · pacote model"]
            PERS["<b>Camada de Persistência</b><br/>pacote repository · JdbcTemplate"]
            APRES --> NEG
            NEG --> PERS
        end
        BD[("Banco de dados<br/>SQLite<br/>arquivo único")]
    end
    IA["Serviço externo<br/>API de IA generativa"]

    NAV -->|"HTTP · porta 8080"| APRES
    APRES -->|"HTML renderizado"| NAV
    PERS -->|"JDBC · SQL"| BD
    NEG -->|"HTTPS"| IA
```

---

## Figura 2 — Diagrama de casos de uso geral (aproximação; redesenhar no draw.io)

```mermaid
flowchart LR
    CLI(("Cliente"))
    ADM(("Administrador"))
    IA(("Serviço de IA"))

    subgraph SIS["Sistema Livraria Estante do Saber"]
        UC01["Manter cadastro de cliente"]
        UC02["Consultar catálogo"]
        UC03["Gerenciar carrinho"]
        UC04["Realizar compra"]
        UC05["Selecionar endereço de entrega"]
        UC06["Compor forma de pagamento"]
        UC07["Aplicar cupom"]
        UC08["Consultar pedidos"]
        UC09["Confirmar recebimento"]
        UC10["Cancelar pedido"]
        UC11["Solicitar troca"]
        UC12["Informar despacho do item"]
        UC13["Consultar cupons"]
        UC14["Recomendar livros ao cliente"]
        UC15["Consultar clientes e pedidos"]
        UC16["Alterar status do pedido"]
        UC17["Processar troca"]
        UC18["Analisar histórico de vendas"]
        UC19["Manter catálogo e estoque"]
    end

    CLI --- UC01
    CLI --- UC02
    CLI --- UC03
    CLI --- UC04
    CLI --- UC08
    CLI --- UC09
    CLI --- UC10
    CLI --- UC11
    CLI --- UC12
    CLI --- UC13
    CLI --- UC14

    ADM --- UC15
    ADM --- UC16
    ADM --- UC17
    ADM --- UC18
    ADM --- UC19

    UC14 --- IA

    UC04 -.->|include| UC05
    UC04 -.->|include| UC06
    UC06 -.->|extend| UC07
    UC17 -.->|include| UC13
```

**Relações a representar no desenho final:**

| Origem | Relação | Destino | Justificativa |
|---|---|---|---|
| Realizar compra | `<<include>>` | Selecionar endereço de entrega | RF0035 — sempre ocorre |
| Realizar compra | `<<include>>` | Compor forma de pagamento | RF0036 — sempre ocorre |
| Compor forma de pagamento | `<<extend>>` | Aplicar cupom | RF0037 — opcional |
| Processar troca | `<<include>>` | Gerar cupom de troca | RF0045 — decorre do recebimento |

---

## Figura 3 — Diagrama de camadas (visão lógica)

```mermaid
flowchart TB
    subgraph L1["Camada de Apresentação"]
        T["resources/templates<br/><i>telas e fragmentos Thymeleaf</i>"]
        S["resources/static<br/><i>css · js · imagens</i>"]
        C["com.fatec.livraria.controller<br/><i>Catalogo · Cliente · Compra<br/>Pedido · Cupom · Admin</i>"]
    end
    subgraph L2["Camada de Negócio"]
        SV["com.fatec.livraria.service<br/><i>regras de negócio</i>"]
        M["com.fatec.livraria.model<br/><i>classes de domínio</i>"]
    end
    subgraph L3["Camada de Persistência"]
        R["com.fatec.livraria.repository<br/><i>acesso a dados via JdbcTemplate</i>"]
    end
    BD[("SQLite")]

    T -.->|renderizado por| C
    C --> SV
    SV --> M
    SV --> R
    R --> M
    R --> BD
```

---

## Figura 4 — Classes da camada de controle

```mermaid
classDiagram
    direction TB
    class CatalogoController {
        +catalogo(Model) String
        +detalharLivro(Long, Model) String
    }
    class ClienteController {
        +novoCliente(Model) String
        +salvarCliente(Cliente) String
        +detalharCliente(Long, Model) String
    }
    class CompraController {
        +exibirCarrinho(Model) String
        +adicionarItem(Long, int) String
        +checkoutEndereco(Model) String
        +processarEndereco(String) String
        +checkoutPagamento(Model) String
        +processarPagamento(Long, Long, String) String
        +checkoutConfirmacao(Long, Model) String
    }
    class PedidoController {
        +pedidos(Model) String
        +detalhesPedido(Long, Model) String
        +solicitarTroca(Long, Model) String
        +informarDespacho(Long, Model) String
    }
    class CupomController {
        +cupons(Model) String
    }
    class AdminController {
        +listarClientes(Model) String
        +detalheCliente(Long, Model) String
        +listarPedidos(Model) String
        +alterarStatusPedido(Long, String) String
        +listarTrocas(Model) String
        +analisarTroca(Long, String) String
        +receberItemTroca(Long, Boolean) String
        +analise(Model, String, String, List~String~) String
    }
```

---

## Figura 5 — Classes do modelo de domínio

Use o **diagrama da seção 1** de [diagrama-de-classes.md](diagrama-de-classes.md),
que já traz atributos, métodos, visibilidade e relacionamentos.

---

## Figura 6 — Diagrama de sequência: Cadastrar Cliente

```mermaid
sequenceDiagram
    autonumber
    actor U as Cliente
    participant N as Navegador
    participant C as ClienteController
    participant S as ClienteService
    participant R as ClienteRepository
    participant BD as SQLite

    U->>N: preenche o formulário e aciona Salvar
    N->>C: POST /clientes/salvar
    C->>S: cadastrar(cliente)
    S->>S: validar campos obrigatórios e CPF
    S->>S: validar força e confirmação da senha
    S->>R: buscarPorEmail(email)
    R->>BD: SELECT
    BD-->>R: resultado
    R-->>S: cliente existente ou vazio

    alt e-mail já cadastrado
        S-->>C: erro de validação
        C-->>N: reexibe o formulário com a mensagem
    else dados válidos
        S->>S: criptografar senha
        S->>R: salvar(cliente)
        R->>BD: INSERT cliente
        R->>BD: INSERT endereco
        R->>BD: INSERT cartao_credito
        BD-->>R: identificador gerado
        R-->>S: cliente persistido
        S->>R: registrarLog(operação, usuário, dados)
        R->>BD: INSERT log_transacao
        S-->>C: cliente cadastrado
        C-->>N: redireciona para a confirmação
    end
    N-->>U: apresenta o resultado
```

---

## Figura 7 — Visão de implantação

```mermaid
flowchart TB
    subgraph NODE1["&lt;&lt;device&gt;&gt; Estação de trabalho — Windows 11"]
        NAV["&lt;&lt;execution environment&gt;&gt;<br/>Navegador<br/>Chrome · Edge · Firefox"]
        subgraph JVM["&lt;&lt;execution environment&gt;&gt; JVM — Java 21"]
            JAR["&lt;&lt;artifact&gt;&gt;<br/>livraria.jar<br/><i>Tomcat embutido · porta 8080</i>"]
        end
        DB["&lt;&lt;artifact&gt;&gt;<br/>livraria.db<br/><i>SQLite — arquivo único</i>"]
    end
    subgraph NODE2["&lt;&lt;device&gt;&gt; Servidor externo"]
        API["&lt;&lt;service&gt;&gt;<br/>API de IA generativa"]
    end

    NAV -->|"HTTP<br/>localhost:8080"| JAR
    JAR -->|"JDBC<br/>sistema de arquivos"| DB
    JAR -->|"HTTPS<br/>internet"| API
```

---

## Figura 8 — Modelo lógico de dados (DER)

```mermaid
erDiagram
    CLIENTE ||--o{ ENDERECO : possui
    CLIENTE ||--o{ CARTAO_CREDITO : possui
    CLIENTE ||--o{ PEDIDO : realiza
    CLIENTE ||--o{ CUPOM : detem

    GRUPO_PRECIFICACAO ||--o{ LIVRO : define_margem
    LIVRO }o--o{ CATEGORIA : classificado_em
    LIVRO ||--o{ ITEM_ESTOQUE : possui
    FORNECEDOR ||--o{ ITEM_ESTOQUE : fornece

    PEDIDO ||--|{ ITEM_PEDIDO : contem
    LIVRO ||--o{ ITEM_PEDIDO : referenciado_em
    PEDIDO ||--o{ HISTORICO_STATUS : registra
    PEDIDO ||--|{ PAGAMENTO : liquidado_por
    CARTAO_CREDITO ||--o{ PAGAMENTO : utilizado_em
    CUPOM ||--o{ PAGAMENTO : utilizado_em
    ENDERECO ||--o{ PEDIDO : entregue_em

    PEDIDO ||--o{ TROCA : origina
    TROCA ||--|{ ITEM_TROCA : contem
    ITEM_PEDIDO ||--o{ ITEM_TROCA : devolvido_em
    TROCA ||--o| CUPOM : gera
```

---

## Figura 9 — Modelo físico de dados

```mermaid
erDiagram
    CLIENTE {
        INTEGER id PK
        TEXT nome
        TEXT cpf UK
        TEXT email UK
        TEXT telefone
        TEXT genero
        TEXT data_nascimento
        TEXT senha_hash
        INTEGER ativo
    }
    ENDERECO {
        INTEGER id PK
        INTEGER cliente_id FK
        TEXT nome_curto
        TEXT tipo_residencia
        TEXT logradouro
        TEXT numero
        TEXT bairro
        TEXT cep
        TEXT cidade
        TEXT estado
    }
    CARTAO_CREDITO {
        INTEGER id PK
        INTEGER cliente_id FK
        TEXT numero_mascarado
        TEXT nome_impresso
        TEXT bandeira
        INTEGER preferencial
    }
    GRUPO_PRECIFICACAO {
        INTEGER id PK
        TEXT descricao
        REAL margem_lucro
    }
    CATEGORIA {
        INTEGER id PK
        TEXT nome UK
    }
    LIVRO {
        INTEGER id PK
        INTEGER grupo_precificacao_id FK
        TEXT titulo
        TEXT autor
        TEXT editora
        TEXT edicao
        TEXT isbn UK
        INTEGER ano
        INTEGER paginas
        TEXT sinopse
        TEXT dimensoes
        REAL preco_venda
        INTEGER ativo
        TEXT motivo_inativacao
    }
    LIVRO_CATEGORIA {
        INTEGER livro_id PK_FK
        INTEGER categoria_id PK_FK
    }
    FORNECEDOR {
        INTEGER id PK
        TEXT nome
        TEXT cnpj UK
    }
    ITEM_ESTOQUE {
        INTEGER id PK
        INTEGER livro_id FK
        INTEGER fornecedor_id FK
        INTEGER quantidade
        REAL valor_custo
        TEXT data_entrada
    }
    PEDIDO {
        INTEGER id PK
        INTEGER cliente_id FK
        INTEGER endereco_id FK
        TEXT data_pedido
        TEXT status
        REAL valor_frete
        REAL valor_total
    }
    ITEM_PEDIDO {
        INTEGER id PK
        INTEGER pedido_id FK
        INTEGER livro_id FK
        INTEGER quantidade
        REAL valor_unitario
    }
    HISTORICO_STATUS {
        INTEGER id PK
        INTEGER pedido_id FK
        TEXT status
        TEXT data_alteracao
    }
    PAGAMENTO {
        INTEGER id PK
        INTEGER pedido_id FK
        INTEGER cartao_id FK
        TEXT cupom_codigo FK
        TEXT tipo
        REAL valor
    }
    CUPOM {
        TEXT codigo PK
        INTEGER cliente_id FK
        INTEGER troca_id FK
        TEXT tipo
        REAL valor
        TEXT validade
        TEXT situacao
    }
    TROCA {
        INTEGER id PK
        INTEGER pedido_id FK
        TEXT motivo
        TEXT status
        TEXT data_solicitacao
        INTEGER retorna_estoque
    }
    ITEM_TROCA {
        INTEGER id PK
        INTEGER troca_id FK
        INTEGER item_pedido_id FK
        INTEGER quantidade
    }
    LOG_TRANSACAO {
        INTEGER id PK
        TEXT data_hora
        TEXT usuario
        TEXT operacao
        TEXT tabela_afetada
        TEXT dados_alterados
    }

    CLIENTE ||--o{ ENDERECO : ""
    CLIENTE ||--o{ CARTAO_CREDITO : ""
    CLIENTE ||--o{ PEDIDO : ""
    CLIENTE ||--o{ CUPOM : ""
    GRUPO_PRECIFICACAO ||--o{ LIVRO : ""
    LIVRO ||--o{ LIVRO_CATEGORIA : ""
    CATEGORIA ||--o{ LIVRO_CATEGORIA : ""
    LIVRO ||--o{ ITEM_ESTOQUE : ""
    FORNECEDOR ||--o{ ITEM_ESTOQUE : ""
    ENDERECO ||--o{ PEDIDO : ""
    PEDIDO ||--|{ ITEM_PEDIDO : ""
    LIVRO ||--o{ ITEM_PEDIDO : ""
    PEDIDO ||--o{ HISTORICO_STATUS : ""
    PEDIDO ||--|{ PAGAMENTO : ""
    CARTAO_CREDITO ||--o{ PAGAMENTO : ""
    CUPOM ||--o{ PAGAMENTO : ""
    PEDIDO ||--o{ TROCA : ""
    TROCA ||--|{ ITEM_TROCA : ""
    ITEM_PEDIDO ||--o{ ITEM_TROCA : ""
    TROCA ||--o| CUPOM : ""
```

### Observações sobre o modelo físico

- O SQLite trabalha com poucos tipos: `INTEGER`, `REAL`, `TEXT`, `BLOB` e `NUMERIC`.
  Datas são armazenadas como `TEXT` no formato ISO-8601 (`AAAA-MM-DD HH:MM:SS`),
  e valores lógicos como `INTEGER` (0 ou 1) — não existe tipo booleano nem tipo data.
- `senha_hash` guarda a senha criptografada, nunca o texto digitado (RNF0033).
- `numero_mascarado` guarda apenas os últimos dígitos do cartão.
- `LIVRO_CATEGORIA` é a tabela associativa que resolve o muitos-para-muitos exigido
  pela RN0012.
- `valor_unitario` é copiado para `ITEM_PEDIDO` no momento da venda: o preço do livro
  pode mudar depois, e o pedido precisa preservar o valor praticado.
- `PAGAMENTO` aceita `cartao_id` **ou** `cupom_codigo`, conforme o `tipo` — é a
  tabela que viabiliza o pagamento combinado exigido pela RF0037.
