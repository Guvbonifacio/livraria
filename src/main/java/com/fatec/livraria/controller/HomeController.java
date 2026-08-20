package com.fatec.livraria.controller;
import com.fatec.livraria.model.Livro;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller //Essa anotação informa ao Spring que essa classe atende requisições da web
public class HomeController {

    List<Livro> livros = List.of(
        new Livro("O Hobbit", "J.R.R. Tolkien", "Fantasia", 49.90),
        new Livro("O Senhor dos Anéis", "J.R.R. Tolkien", "Fantasia", 59.90),
        new Livro("O Pequeno Príncipe", "Antoine de Saint-Exupéry", "Literatura Clássica", 29.90),
        new Livro("Dom Casmurro", "Machado de Assis", "Literatura Clássica", 24.90),
        new Livro("O Poder do Hábito", "Charles Duhigg", "Autoajuda", 44.90),
        new Livro("1984", "George Orwell", "Ficção", 39.90)
    );

    @GetMapping("/") //vai dizer qual endereço esse método atende
    public String home(Model model){
        model.addAttribute("titulo", "Livraria Estante do Saber");
        model.addAttribute("categorias", List.of("Autoajuda", "Fantasia", "Ficção", "Literatura Clássica"));
        model.addAttribute("livros", livros);
        return "home";        
    }

    @GetMapping("/carrinho")
    public String carrinho() {
        return "carrinho";
    }
}