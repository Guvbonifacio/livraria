package com.fatec.livraria.controller;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.service.ClienteService;
import com.fatec.livraria.service.ValidacaoException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping("/novo")  // Exibe o formulário de cadastro de novo cliente
    public String novoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cliente-form";
    }

    @PostMapping("/salvar")   // Processa o envio do formulário de cadastro
    public String salvarCliente(@ModelAttribute Cliente cliente, @RequestParam String confirmacaoSenha, Model model) {
        try {
            Long id = service.cadastrar(cliente, confirmacaoSenha);
            return "redirect:/clientes/" + id;
        } catch (ValidacaoException e) {
            // Retorna ao formulário mantendo os dados preenchidos e exibindo os erros
            model.addAttribute("erros", e.getErros());
            model.addAttribute("cliente", cliente);
            return "cliente-form";
        }
    }

    @GetMapping("/{id}")   // Exibe os detalhes/perfil do cliente cadastrado
    public String detalharCliente(@PathVariable Long id, Model model) {
        Cliente cliente = service.buscarPorId(id);
        model.addAttribute("cliente", cliente);
        return "cliente-perfil";
    }
}