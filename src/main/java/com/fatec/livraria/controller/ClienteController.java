package com.fatec.livraria.controller;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.DadosFalsos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @GetMapping("/novo")
    public String novoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cliente-form";
    }

    @PostMapping("/salvar")
    public String salvarCliente(Cliente cliente) {
        // Simulação do salvamento no protótipo
        return "redirect:/";
    }

    @GetMapping("/{id}")
    public String detalharCliente (@PathVariable Long id, Model model){
        Cliente cliente = DadosFalsos.clienteExemplo(id); //* Chamo o método clienteExemplo dos DadosFalsos e armazeno na variavel
        // cliente  */
        model.addAttribute("cliente", cliente); //o model passa a ter essa variável cliente
        return "cliente-perfil"; //retorno com o nome da página que o html terá que carregar
    }
}