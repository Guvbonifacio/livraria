package com.fatec.livraria.model;

import java.time.LocalDate;
import java.util.List;

public class DadosFalsos {

    public static List<Livro> listaDeLivros() {
        return List.of(
            new Livro(1L, "O Hobbit", "J.R.R. Tolkien", "Fantasia", 1937, "HarperCollins", "1ª Edição", "978-8533613379", 336, 
                      "Bilbo Bolseiro é um hobbit de vida confortável. Sua pacatez é interrompida quando Gandalf e 13 anões surgem em sua porta para uma jornada perigosa.", 
                      "20.8 x 13.6 x 2.2 cm", 49.90, 15, "https://via.placeholder.com/300x400"),

            new Livro(2L, "Duna", "Frank Herbert", "Ficção", 1965, "Editora Aleph", "2ª Edição", "978-8576572008", 680, 
                      "Uma efervescente mistura de aventura, misticismo e ecologia ambientada no planeta desértico de Arrakis.", 
                      "23.0 x 16.0 x 4.0 cm", 62.00, 8, "https://via.placeholder.com/300x400"),

            new Livro(3L, "Dom Casmurro", "Machado de Assis", "Literatura Clássica", 1899, "Garnier", "1ª Edição", "978-8535902778", 256, 
                      "A célebre história de Bentinho e Capitu, marcada pelo enigma e pela dúvida perturbadora do ciúme.", 
                      "21.0 x 14.0 x 1.5 cm", 34.50, 20, "https://via.placeholder.com/300x400"),

            new Livro(4L, "O Poder do Hábito", "Charles Duhigg", "Autoajuda", 2012, "Objetiva", "1ª Edição", "978-8539004119", 408, 
                      "Como nossos hábitos funcionam e como podemos alterá-los para transformar nossas vidas, negócios e comunidades.", 
                      "23.0 x 15.6 x 2.4 cm", 54.90, 12, "https://via.placeholder.com/300x400"),

            new Livro(5L, "1984", "George Orwell", "Ficção", 1949, "Companhia das Letras", "1ª Edição", "978-8535914849", 416, 
                      "Um dos romances mais influentes do século XX, sobre um futuro distópico sob vigilância constante do Grande Irmão.", 
                      "21.0 x 14.0 x 2.2 cm", 45.00, 18, "https://via.placeholder.com/300x400"),

            new Livro(6L, "O Senhor dos Anéis", "J.R.R. Tolkien", "Fantasia", 1954, "HarperCollins", "2ª Edição", "978-8595084773", 1200, 
                      "A jornada épica para destruir o Um Anel e salvar a Terra-média da tirania de Sauron.", 
                      "23.0 x 16.0 x 5.0 cm", 89.90, 5, "https://via.placeholder.com/300x400"),

            new Livro(7L, "Memórias Póstumas de Brás Cubas", "Machado de Assis", "Literatura Clássica", 1881, "Tipografia Nacional", "1ª Edição", "978-8535910667", 224, 
                      "Um defunto autor narra sua vida com tom irônico, inovando o romance brasileiro e satirizando a sociedade do século XIX.", 
                      "21.0 x 14.0 x 1.3 cm", 29.90, 10, "https://via.placeholder.com/300x400"),

            new Livro(8L, "Ansiedade: Como Enfrentar o Mal do Século", "Augusto Cury", "Autoajuda", 2013, "Benvirá", "1ª Edição", "978-8582400784", 160, 
                      "Apresenta a Síndrome do Pensamento Acelerado (SPA) e traz estratégias para desacelerar a mente e gerenciar o estresse.", 
                      "20.8 x 13.6 x 1.0 cm", 39.90, 14, "https://via.placeholder.com/300x400"),

            new Livro(9L, "O Nome do Vento", "Patrick Rothfuss", "Fantasia", 2007, "Editora Arqueiro", "1ª Edição", "978-8580410051", 656, 
                      "A trajetória de Kvothe: de uma infância em uma trupe de artistas mímicos a um famoso mago e figura lendária.", 
                      "23.0 x 16.0 x 3.5 cm", 74.90, 9, "https://via.placeholder.com/300x400"),

            new Livro(10L, "Fahrenheit 451", "Ray Bradbury", "Ficção", 1953, "Biblioteca Azul", "1ª Edição", "978-8525052247", 216, 
                      "Em um futuro onde os livros são proibidos e queimados por bombeiros, um deles começa a questionar o sistema.", 
                      "21.0 x 14.0 x 1.8 cm", 42.00, 11, "https://via.placeholder.com/300x400")
        );
    }

    public static Livro buscarLivroPorId(Long id) {
        return listaDeLivros().stream()
                .filter(livro -> livro.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static List<String> categorias() {
        return List.of("Autoajuda", "Fantasia", "Ficção", "Literatura Clássica");
    }

    public static String nomeClienteAtual() {
        return "Maria Silva";
    }

    public static Cliente clienteExemplo(Long id) {
        Cliente cliente = new Cliente(id, "Ana Maria", "Feminino", "1995-05-20", "123.456.789-00", "(11) 98765-4321", "anamaria@email.com", "Senha@123");

        cliente.getEnderecos().add(new Endereco("Casa", "Casa", "Rua das Flores", "100", "Centro", "08700-000", "Mogi das Cruzes", "SP"));
        cliente.getEnderecos().add(new Endereco("Trabalho", "Apartamento", "Av. Paulista", "1500", "Bela Vista", "01310-100", "São Paulo", "SP"));
        cliente.getCartoes().add(new CartaoCredito(1L, "**** **** **** 1234", "ANA MARIA SILVA", "Mastercard", "123", true));
        cliente.getCartoes().add(new CartaoCredito(2L, "**** **** **** 5678", "ANA MARIA SILVA", "Visa", "456", false));

        return cliente;
    }

    // Método que simula os itens atualmente no carrinho de compras
    public static List<ItemCarrinho> carrinhoExemplo() {
        List<Livro> livros = listaDeLivros();

        // Adiciona O Hobbit (2 unidades) e Duna (1 unidade)
        return List.of(
            new ItemCarrinho(livros.get(0), 2),
            new ItemCarrinho(livros.get(1), 1)
        );
    }

    // Método que simula itens removidos por expiração de tempo no estoque (RNF0042)
    public static List<ItemCarrinho> itensExpiradosExemplo() {
        List<Livro> livros = listaDeLivros();

        // Simula que 1984 expirou e foi removido do carrinho
        return List.of(
            new ItemCarrinho(livros.get(4), 1)
        );
    }

    public static List<Cupom> cuponsExemplo() {
        return List.of(
            new Cupom("TROCA25", "Troca", 25.00, LocalDate.of(2026, 12, 31), "DISPONÍVEL"),
            new Cupom("PROMO10", "Promocional", 10.00, LocalDate.of(2026, 9, 30), "DISPONÍVEL"),
            new Cupom("TROCA40", "Troca", 40.00, LocalDate.of(2026, 7, 15), "EXPIRADO"),
            new Cupom("LIVROS15", "Promocional", 15.00, LocalDate.of(2026, 11, 20), "UTILIZADO")
        );
    }
}