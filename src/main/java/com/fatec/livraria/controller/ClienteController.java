package com.fatec.livraria.controller;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.DadosFalsos;

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

        // TEMPORÁRIO — passa pelo Service no Passo D
        Long id = repository.salvar(cliente);

        System.out.println("Cliente salvo com ID: " + id);

        return "redirect:/clientes/" + id;  ///retorna para o cliente recem criado
    }

    @GetMapping("/{id}")
    public String detalharCliente (@PathVariable Long id, Model model){
        Cliente cliente = DadosFalsos.clienteExemplo(id); //* Chamo o método clienteExemplo dos DadosFalsos e armazeno na variavel
        // cliente  */
        model.addAttribute("cliente", cliente); //o model passa a ter essa variável cliente
        return "cliente-perfil"; //retorno com o nome da página que o html terá que carregar
    }

}