package com.fatec.livraria.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.DadosFalsos;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // Página inicial da administração
    @GetMapping
    public String admin() {
        return "admin";
    }

    // Lista de clientes
    @GetMapping("/clientes")
    public String listarClientes(Model model) {

        List<Cliente> clientes = List.of(
            DadosFalsos.clienteExemplo(1L)
        );

        model.addAttribute("clientes", clientes);

        return "admin-clientes";
    }

    // Detalhe de um cliente
    @GetMapping("/clientes/{id}")
    public String detalheCliente(@PathVariable Long id, Model model) {

        Cliente cliente = DadosFalsos.clienteExemplo(id);

        model.addAttribute("cliente", cliente);

        return "admin-cliente-detalhe";
    }
}