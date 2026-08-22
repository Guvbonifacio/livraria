package com.fatec.livraria.model;

import java.util.List;

public class DadosFalsos {

    public static List<Livro> listaDeLivros() {
        return List.of(
            new Livro(1L,"O Hobbit","J.R.R. Tolkien","Fantasia",49.90),
            new Livro(2L,"Duna","Frank Herbert","Ficção",62.00),
            new Livro(3L,"Dom Casmurro","Machado de Assis", "Literatura Clássica",34.50),
            new Livro(4L,"O Poder do Hábito","Charles Duhigg",   "Autoajuda",54.90),
            new Livro(5L,"1984","George Orwell","Ficção",45.00),
            new Livro(6L,"O Senhor dos Anéis","J.R.R. Tolkien","Fantasia",89.90)
        );
    }

    public static Livro buscarLivroPorId(Long id) {
        return listaDeLivros().stream()//percorre a lista e devolver o primeiro que tiver o id procurado
                .filter(livro -> livro.getId().equals(id)) // filtra a lista e mantem apenas o com id correspondente
                .findFirst() //retorno o primeiro elemento encontrado caso exista
                .orElse(null); //retorno caso nada seja encontrado
    }

    public static List<String> categorias() {
        return List.of("Autoajuda", "Fantasia", "Ficção", "Literatura Clássica");
    }

    public static String nomeClienteAtual() {
        return "Maria Silva";
    }
}