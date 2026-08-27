package com.fatec.livraria.controller;

import com.fatec.livraria.model.DadosFalsos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CupomController {

    @GetMapping("/cupons")
    public String cupons(Model model) {

        model.addAttribute(
            "cupons",
            DadosFalsos.cuponsExemplo()
        );

        return "cupons";
    }
}