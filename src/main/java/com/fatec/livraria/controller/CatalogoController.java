package com.fatec.livraria.controller;

import com.fatec.livraria.model.DadosFalsos;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CatalogoController {

    @GetMapping("/")
    public String catalogo(Model model) {
        model.addAttribute("livros", DadosFalsos.listaDeLivros());
        model.addAttribute("categorias", DadosFalsos.categorias());
        return "catalogo";
    }
}