package com.fatec.livraria.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller //Essa anotação informa ao Spring que essa classe atende requisições da web
public class HomeController {

    @GetMapping("/") //vai dizer qual endereço esse método atende
    public String home(Model model){
        model.addAttribute("titulo", "Livraria Estante do Saber");
        model.addAttribute("categorias", List.of("Autoajuda", "Fantasia", "Ficção", "Literatura Clássica"));
        return "home";        
    }
}
