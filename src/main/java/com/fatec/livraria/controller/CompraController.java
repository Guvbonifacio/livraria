package com.fatec.livraria.controller;

import java.util.List;
import com.fatec.livraria.model.ItemCarrinho;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fatec.livraria.model.DadosFalsos;

@Controller
@RequestMapping("/carrinho") //Isso define a rota base para toda a classe. Os próximos métodos apenas adicionaram um "subcaminho"

public class CompraController {

    @GetMapping
    public String exibirCarrinho(Model model){
    List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();
    List<ItemCarrinho> expirados = DadosFalsos.itensExpiradosExemplo();
    double totalGeral = itens.stream() //Lista de itens
        .mapToDouble(item -> item.getSubtotal()) // extrai apenas o valor, dos ites, retornado pelo método GetSubtotal()
        .sum();

    model.addAttribute("itens", itens);
    model.addAttribute("itensExpirados", expirados);
    model.addAttribute("totalGeral", totalGeral);
    return "carrinho";
}

    @PostMapping("/adicionar")
    public String adicionarItem(@RequestParam Long livroId, @RequestParam(defaultValue="1") int quantidade){
        return "redirect:/carrinho";
    }
}
