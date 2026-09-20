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

    // Exibe o formulário de cadastro de novo cliente
    @GetMapping("/novo")
    public String novoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cliente-form";
    }

    // Processa o envio do formulário de cadastro
    @PostMapping("/salvar")
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

    // Exibe os detalhes/perfil do cliente cadastrado
    @GetMapping("/{id}")
    public String detalharCliente(@PathVariable Long id, Model model) {
        Cliente cliente = service.buscarPorId(id);
        model.addAttribute("cliente", cliente);
        return "cliente-perfil";
    }

    // Exibe a tela de edição do cliente
    @GetMapping("/{id}/editar")
    public String editarCliente(@PathVariable Long id, Model model) {
        Cliente cliente = service.buscarPorId(id);
        model.addAttribute("cliente", cliente);
        return "cliente-editar";
    }

    // Processa a atualização dos dados do cliente
    @PostMapping("/{id}/atualizar")
    public String atualizarCliente(@PathVariable Long id, @ModelAttribute Cliente cliente, Model model) {
        cliente.setId(id);
        try {
            service.alterar(cliente);
            return "redirect:/clientes/" + id;
        } catch (ValidacaoException e) {
            model.addAttribute("erros", e.getErros());
            return "cliente-editar";
        }
    }
}