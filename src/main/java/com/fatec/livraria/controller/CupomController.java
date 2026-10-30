package com.fatec.livraria.controller;

import com.fatec.livraria.service.CupomClienteService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CupomController {

    private final CupomClienteService cupons;

    public CupomController(CupomClienteService cupons) {
        this.cupons = cupons;
    }

    @GetMapping("/cupons")
    public String cupons(Model model) {
        model.addAttribute("cupons", cupons.listar());

        return "cupons";
    }
}