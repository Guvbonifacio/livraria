package com.fatec.livraria.controller;

import com.fatec.livraria.model.DadosFalsos;
import com.fatec.livraria.model.Livro;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CatalogoController {

    @GetMapping("/")
    public String catalogo(Model model) {
        model.addAttribute("livros", DadosFalsos.listaDeLivros());
        model.addAttribute("categorias", DadosFalsos.categorias());
        return "catalogo";
    }

    @GetMapping("/livro/{id}")
    public String detalharLivro(@PathVariable Long id, Model model){
        Livro livroEcontrado = DadosFalsos.buscarLivroPorId(id); //Busca o livro pelo id
        model.addAttribute("livro", livroEcontrado); //atribui o objeto livro encontrado
        return "livro";
    }
}