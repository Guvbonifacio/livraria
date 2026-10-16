package com.fatec.livraria.controller;

import com.fatec.livraria.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ConfirmacaoPedidoController {

    private final PedidoService pedidos;

    public ConfirmacaoPedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping("/pedidos/{id}/confirmacao")
    public String exibir(
            @PathVariable Long id,
            Model model) {

        var pedido = pedidos.buscarConfirmacao(id);

        if (pedido == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pedido não encontrado."
            );
        }

        model.addAttribute("pedido", pedido);

        return "pedido-confirmado";
    }
}