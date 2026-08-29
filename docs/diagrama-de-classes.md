# Diagrama de Classes — Livraria Estante do Saber

Projeto de Laboratório de Engenharia de Software · FATEC Mogi das Cruzes · 2026
Gustavo Vinícius Bonifácio — RA 1840482423023

---

## Legenda de visibilidade

| Símbolo | Significado |
|---------|-------------|
| `-` | `private` — acessível apenas dentro da própria classe |
| `+` | `public` — acessível de qualquer lugar |
| `#` | `protected` — acessível na própria classe e nas subclasses |

Todos os atributos do projeto são `private` e todo o acesso a eles ocorre por
métodos `public` (getters e setters). Esse é o princípio do **encapsulamento**:
a classe controla como seus dados são lidos e alterados.

---

## 1. Modelo de domínio implementado

Estas são as classes que existem hoje no pacote `com.fatec.livraria.model`.

```mermaid
classDiagram
    direction TB

    class Livro {
        -Long id
        -String titulo
        -String autor
        -String categoria
        -int ano
        -String editora
        -String edicao
        -String isbn
        -int paginas
        -String sinopse
        -String dimensoes
        -double preco
        -int estoque
        -String imagemUrl
        +Livro(Long, String, String, String, int, String, String, String, int, String, String, double, int, String)
        +getId() Long
        +getTitulo() String
        +getAutor() String
        +getCategoria() String
        +getAno() int
        +getEditora() String
        +getEdicao() String
        +getIsbn() String
        +getPaginas() int
        +getSinopse() String
        +getDimensoes() String
        +getPreco() double
        +getEstoque() int
        +getImagemUrl() String
    }

    class Cliente {
        -Long id
        -String nome
        -String genero
        -String dataNascimento
        -String cpf
        -String telefone
        -String email
        -String senha
        -List~Endereco~ enderecos
        -List~CartaoCredito~ cartoes
        +Cliente()
        +Cliente(Long, String, String, String, String, String, String, String)
        +getId() Long
        +setId(Long) void
        +getNome() String
        +setNome(String) void
        +getGenero() String
        +setGenero(String) void
        +getDataNascimento() String
        +setDataNascimento(String) void
        +getCpf() String
        +setCpf(String) void
        +getTelefone() String
        +setTelefone(String) void
        +getEmail() String
        +setEmail(String) void
        +getSenha() String
        +setSenha(String) void
        +getEnderecos() List~Endereco~
        +setEnderecos(List~Endereco~) void
        +getCartoes() List~CartaoCredito~
        +setCartoes(List~CartaoCredito~) void
    }

    class Endereco {
        -String nomeCurto
        -String tipoResidencia
        -String logradouro
        -String numero
        -String bairro
        -String cep
        -String cidade
        -String estado
        +Endereco(String, String, String, String, String, String, String, String)
        +getNomeCurto() String
        +getTipoResidencia() String
        +getLogradouro() String
        +getNumero() String
        +getBairro() String
        +getCep() String
        +getCidade() String
        +getEstado() String
    }

    class CartaoCredito {
        -Long id
        -String numero
        -String nomeImpresso
        -String bandeira
        -String codigoSeguranca
        -boolean preferencial
        +CartaoCredito(Long, String, String, String, String, boolean)
        +getId() Long
        +setId(Long) void
        +getNumero() String
        +getNomeImpresso() String
        +getBandeira() String
        +getCodigoSeguranca() String
        +isPreferencial() boolean
    }

    class Pedido {
        -Long id
        -LocalDateTime dataPedido
        -String status
        -Cliente cliente
        -List~ItemCarrinho~ itens
        -double valorFrete
        -double valorTotal
        -List~HistoricoStatus~ historico
        +Pedido(Long, Cliente, List~ItemCarrinho~, double, double)
        +getId() Long
        +setId(Long) void
        +getDataPedido() LocalDateTime
        +setDataPedido(LocalDateTime) void
        +getStatus() String
        +setStatus(String) void
        +getCliente() Cliente
        +setCliente(Cliente) void
        +getItens() List~ItemCarrinho~
        +setItens(List~ItemCarrinho~) void
        +getValorFrete() double
        +setValorFrete(double) void
        +getValorTotal() double
        +setValorTotal(double) void
        +getHistorico() List~HistoricoStatus~
        +setHistorico(List~HistoricoStatus~) void
    }

    class ItemCarrinho {
        -Livro livro
        -int quantidade
        +ItemCarrinho(Livro, int)
        +getSubtotal() double
        +getLivro() Livro
        +setLivro(Livro) void
        +getQuantidade() int
        +setQuantidade(int) void
    }

    class HistoricoStatus {
        -LocalDateTime data
        -String status
        +HistoricoStatus(LocalDateTime, String)
        +getData() LocalDateTime
        +getStatus() String
    }

    class Troca {
        -Long id
        -Pedido pedido
        -List~ItemCarrinho~ itens
        -String motivo
        -String status
        -LocalDateTime dataSolicitacao
        -boolean retornaEstoque
        +Troca(Long, Pedido, List~ItemCarrinho~, String)
        +getId() Long
        +getPedido() Pedido
        +getItens() List~ItemCarrinho~
        +getMotivo() String
        +getStatus() String
        +setStatus(String) void
        +getDataSolicitacao() LocalDateTime
        +isRetornaEstoque() boolean
        +setRetornaEstoque(boolean) void
    }

    class Cupom {
        -String codigo
        -String tipo
        -double valor
        -LocalDate validade
        -String situacao
        +Cupom(String, String, double, LocalDate, String)
        +getCodigo() String
        +getTipo() String
        +getValor() double
        +getValidade() LocalDate
        +getSituacao() String
    }

    class SerieVendas {
        -String categoria
        -List~Double~ valores
        +SerieVendas(String, List~Double~)
        +getCategoria() String
        +getValores() List~Double~
    }

    Cliente "1" *-- "0..*" Endereco : possui
    Cliente "1" *-- "0..*" CartaoCredito : possui
    Pedido "0..*" --> "1" Cliente : pertence a
    Pedido "1" *-- "1..*" ItemCarrinho : contem
    Pedido "1" *-- "0..*" HistoricoStatus : registra
    ItemCarrinho "0..*" --> "1" Livro : referencia
    Troca "0..*" --> "1" Pedido : origina-se de
    Troca "1" o-- "1..*" ItemCarrinho : devolve
```

### Tipos de relacionamento usados

| Notação | Nome | Significado |
|---------|------|-------------|
| `*--` | Composição | A parte não existe sem o todo. Um `Endereco` só faz sentido dentro de um `Cliente`; apagado o cliente, some o endereço. |
| `o--` | Agregação | A parte existe independentemente. Os itens da `Troca` continuam existindo no `Pedido` original. |
| `-->` | Associação | Uma classe usa a outra, sem posse. `ItemCarrinho` aponta para um `Livro` do catálogo. |

### Multiplicidades

- `Cliente 1 — 0..* Endereco` — vários endereços por cliente (**RF0026**)
- `Cliente 1 — 0..* CartaoCredito` — vários cartões, um deles preferencial (**RF0027**)
- `Pedido 1 — 1..* ItemCarrinho` — todo pedido tem ao menos um item
- `ItemCarrinho 0..* — 1 Livro` — o mesmo livro aparece em muitos pedidos

---

## 2. Classe utilitária

`DadosFalsos` não faz parte do domínio: é uma classe de apoio que fornece dados
de exemplo ao protótipo, no lugar do banco de dados. Todos os seus métodos são
`static`, o que significa que são chamados diretamente pela classe, sem criar um
objeto (`DadosFalsos.listaDeLivros()`).

```mermaid
classDiagram
    class DadosFalsos {
        +listaDeLivros()$ List~Livro~
        +buscarLivroPorId(Long)$ Livro
        +categorias()$ List~String~
        +nomeClienteAtual()$ String
        +clienteExemplo(Long)$ Cliente
        +carrinhoExemplo()$ List~ItemCarrinho~
        +itensExpiradosExemplo()$ List~ItemCarrinho~
        +cuponsExemplo()$ List~Cupom~
        +mesesVendas()$ List~String~
        +seriesVendas()$ List~SerieVendas~
    }
```

> O símbolo `$` após o método indica um membro `static`, conforme a notação UML.

Na segunda grande entrega esta classe será substituída pelas camadas **Service**
e **Repository**, que buscarão os dados no banco SQLite. As classes de domínio
acima permanecerão as mesmas.

---

## 3. Camadas MVC

O diagrama abaixo mostra como as classes se organizam nas camadas exigidas pelo
padrão MVC. Note que os Controllers conhecem o Model, mas o Model **não conhece**
os Controllers, e a View não conhece nenhum dos dois além dos dados recebidos.

```mermaid
classDiagram
    direction LR

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
        +admin() String
        +listarClientes(Model) String
        +detalheCliente(Long, Model) String
        +inativarCliente(Long) String
        +listarPedidos(Model) String
        +detalhePedidoAdmin(Long, Model) String
        +alterarStatusPedido(Long, String) String
        +listarTrocas(Model) String
        +detalheTroca(Long, Model) String
        +analisarTroca(Long, String) String
        +receberItemTroca(Long, Boolean) String
        +finalizarTroca(Long) String
        +analise(Model, String, String, List~String~) String
    }

    class Model_Dominio {
        Livro
        Cliente
        Endereco
        CartaoCredito
        Pedido
        ItemCarrinho
        HistoricoStatus
        Troca
        Cupom
        SerieVendas
    }

    class View_Thymeleaf {
        catalogo
        livro
        carrinho
        checkout_endereco
        checkout_pagamento
        cliente_form
        cliente_perfil
        pedidos
        cupons
        admin_analise
    }

    CatalogoController ..> Model_Dominio : usa
    ClienteController ..> Model_Dominio : usa
    CompraController ..> Model_Dominio : usa
    PedidoController ..> Model_Dominio : usa
    CupomController ..> Model_Dominio : usa
    AdminController ..> Model_Dominio : usa

    CatalogoController ..> View_Thymeleaf : seleciona
    ClienteController ..> View_Thymeleaf : seleciona
    CompraController ..> View_Thymeleaf : seleciona
    PedidoController ..> View_Thymeleaf : seleciona
    CupomController ..> View_Thymeleaf : seleciona
    AdminController ..> View_Thymeleaf : seleciona
```

A seta tracejada (`..>`) representa **dependência**: o Controller usa as classes
de domínio e escolhe qual template será renderizado, mas não é dono de nenhum
dos dois.

---

## 4. Herança prevista

> **Situação atual:** o projeto não possui herança. Todas as classes do
> protótipo são independentes. As hierarquias abaixo são a evolução planejada
> para a segunda grande entrega, quando as regras de negócio forem implementadas.

### 4.1 Cupom

Hoje o tipo do cupom é um texto (`tipo = "Troca"` ou `"Promocional"`). Mas o DRS
trata os dois de formas diferentes:

- **RN0033** — apenas **um** cupom promocional por compra; de troca, vários
- **RF0045** — o cupom de troca é **gerado pelo sistema** após o recebimento da devolução
- **RN0036** — sobra de valor em cupons de troca gera um novo cupom de troca

Quando duas coisas compartilham dados mas seguem regras diferentes, a herança é
a resposta natural: o que é comum sobe para a superclasse, o que difere desce
para as subclasses.

```mermaid
classDiagram
    direction TB

    class Cupom {
        <<abstract>>
        #String codigo
        #double valor
        #LocalDate validade
        #String situacao
        +getCodigo() String
        +getValor() double
        +getValidade() LocalDate
        +getSituacao() String
        +ehValido()* boolean
        +podeAcumular()* boolean
    }

    class CupomTroca {
        -Long pedidoOrigemId
        +CupomTroca(String, double, LocalDate, Long)
        +ehValido() boolean
        +podeAcumular() boolean
        +getPedidoOrigemId() Long
    }

    class CupomPromocional {
        -String campanha
        +CupomPromocional(String, double, LocalDate, String)
        +ehValido() boolean
        +podeAcumular() boolean
        +getCampanha() String
    }

    Cupom <|-- CupomTroca
    Cupom <|-- CupomPromocional
```

`<<abstract>>` indica que `Cupom` não pode ser instanciada diretamente — só
existem cupons de troca ou promocionais. O `*` após `ehValido()` e
`podeAcumular()` marca métodos **abstratos**: a superclasse declara que toda
subclasse precisa respondê-los, e cada uma responde à sua maneira.
`podeAcumular()` devolve `true` em `CupomTroca` e `false` em `CupomPromocional`,
implementando a RN0033 pela estrutura, e não por um `if` no meio do código.

### 4.2 Pagamento

O **RF0037** exige pagamento combinando cartões e cupons, e a **RN0034** define
valor mínimo de R$ 10,00 por cartão — regra que não se aplica a cupons.

```mermaid
classDiagram
    direction TB

    class Pagamento {
        <<abstract>>
        #Long id
        #double valor
        #Pedido pedido
        +getValor() double
        +getPedido() Pedido
        +validar()* boolean
        +descricao()* String
    }

    class PagamentoCartao {
        -CartaoCredito cartao
        -int parcelas
        +validar() boolean
        +descricao() String
        +getCartao() CartaoCredito
    }

    class PagamentoCupom {
        -Cupom cupom
        +validar() boolean
        +descricao() String
        +getCupom() Cupom
    }

    Pagamento <|-- PagamentoCartao
    Pagamento <|-- PagamentoCupom
    Pagamento "1..*" --o "1" Pedido : compoe
```

`validar()` em `PagamentoCartao` verifica o mínimo de R$ 10,00 (RN0034) e simula
a autorização da operadora (RN0037 — o professor autorizou simulação). Em
`PagamentoCupom`, verifica validade e situação do cupom.

---

## 5. Classes ainda não implementadas

O DRS prevê dois módulos que ficaram fora do protótipo por decisão de escopo e
que precisarão existir nas próximas entregas:

| Classe prevista | Requisitos | Observação |
|---|---|---|
| `Categoria` | RN0012 | Hoje `categoria` é um texto em `Livro`; a RN0012 permite **várias** categorias por livro, o que exige `Livro 0..* — 0..* Categoria` |
| `GrupoPrecificacao` | RF0052, RN0013, RN0014 | Guarda a margem de lucro que calcula o preço de venda |
| `ItemEstoque` | RF0051, RN0050, RN0051 | Entrada em estoque com custo, fornecedor e data |
| `Fornecedor` | RN0050 | Referenciado pela entrada em estoque |
| `LogTransacao` | RNF0012 | Data, hora, usuário e dados alterados de toda escrita |

Também vale notar que `Pedido.status` e `Troca.status` são `String` hoje. Com a
padronização de status definida no projeto, o candidato natural é convertê-los
em `enum` — o que impede que um status inexistente seja atribuído por engano.
