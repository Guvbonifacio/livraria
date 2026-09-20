package com.fatec.livraria.controller;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.repository.ClienteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteRepository repository;

    public ClienteController(ClienteRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/novo")
    public String novoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cliente-form";
    }

    @PostMapping("/salvar")
    public String salvarCliente(@ModelAttribute Cliente cliente) {
        Long id = repository.salvar(cliente);

        for (var endereco : cliente.getEnderecos()) {
            repository.salvarEndereco(id, endereco);
        }

        for (var cartao : cliente.getCartoes()) {
            repository.salvarCartao(id, cartao);
        }

        return "redirect:/clientes/" + id;
    }

    @GetMapping("/{id}")
    public String detalharCliente(@PathVariable Long id, Model model) {
        Cliente cliente = repository.buscarPorId(id);
        model.addAttribute("cliente", cliente);
        return "cliente-perfil";
    }
}