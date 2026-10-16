package com.fatec.livraria.controller;

import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.service.CarrinhoService;
import com.fatec.livraria.service.ValidacaoException;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService service;

    public CarrinhoController(CarrinhoService service) {
        this.service = service;
    }

    @GetMapping
    public String exibir(Model model) {
        List<ItemCarrinho> itens = service.listar();

        model.addAttribute("itens", itens);
        model.addAttribute(
            "totalGeral",
            service.calcularTotal(itens)
        );

        return "carrinho";
    }

    @PostMapping("/adicionar")
    public String adicionar(
            @RequestParam Long livroId,
            @RequestParam(defaultValue = "1") int quantidade,
            RedirectAttributes redirect) {

        return executar(
            redirect,
            () -> service.adicionar(livroId, quantidade),
            "Livro adicionado ao carrinho."
        );
    }

    @PostMapping("/atualizar")
    public String atualizar(
            @RequestParam Long livroId,
            @RequestParam int quantidade,
            RedirectAttributes redirect) {

        return executar(
            redirect,
            () -> service.alterarQuantidade(livroId, quantidade),
            "Quantidade atualizada."
        );
    }

    @PostMapping("/remover")
    public String remover(
            @RequestParam Long livroId,
            RedirectAttributes redirect) {

        return executar(
            redirect,
            () -> service.remover(livroId),
            "Livro removido do carrinho."
        );
    }

    private String executar(
            RedirectAttributes redirect,
            Runnable operacao,
            String sucesso) {
        try {
            operacao.run();

            redirect.addFlashAttribute(
                "sucessoCarrinho",
                sucesso
            );
        } catch (ValidacaoException e) {
            redirect.addFlashAttribute(
                "errosCarrinho",
                e.getErros()
            );
        }

        return "redirect:/carrinho";
    }
}