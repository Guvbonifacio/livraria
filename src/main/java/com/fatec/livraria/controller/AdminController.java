package com.fatec.livraria.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.DadosFalsos;
import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.model.Pedido;
import com.fatec.livraria.model.Troca;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping
public String admin() {
    return "admin";
}

    @GetMapping
    public String admin() {
        return "admin";
    }

    @GetMapping("/clientes")
    public String listarClientes(Model model) {

        List<Cliente> clientes = List.of(
            DadosFalsos.clienteExemplo(1L)
        );

        model.addAttribute("clientes", clientes);

        return "admin-clientes";
    }

    @GetMapping("/clientes/{id}")
    public String detalheCliente(@PathVariable Long id, Model model) {

        Cliente cliente = DadosFalsos.clienteExemplo(id);

        model.addAttribute("cliente", cliente);

        return "admin-cliente-detalhe";
    }

    @GetMapping("/pedidos")
public String listarPedidos(Model model) {

    Cliente cliente = DadosFalsos.clienteExemplo(1L);

    List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

    Pedido pedido = new Pedido(
        1001L,
        cliente,
        itens,
        10.00,
        171.80
    );

    pedido.setDataPedido(
        LocalDateTime.of(2026, 8, 20, 14, 30)
    );

    pedido.setStatus("EM PROCESSAMENTO");

    model.addAttribute("pedidos", List.of(pedido));

    return "admin-pedidos";
}

@GetMapping("/pedidos/{id}")
public String detalhePedidoAdmin(@PathVariable Long id, Model model) {

    Cliente cliente = DadosFalsos.clienteExemplo(1L);
    List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

    Pedido pedido = new Pedido(
        id,
        cliente,
        itens,
        10.00,
        171.80
    );

    pedido.setDataPedido(
        LocalDateTime.of(2026, 8, 20, 14, 30)
    );

    pedido.setStatus("EM PROCESSAMENTO");

    model.addAttribute("pedido", pedido);

    return "admin-pedido-detalhe";
}

@GetMapping("/trocas")
public String listarTrocas(Model model) {

    Cliente cliente = DadosFalsos.clienteExemplo(1L);
    List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

    Pedido pedido = new Pedido(
        1001L,
        cliente,
        itens,
        10.00,
        171.80
    );

    pedido.setStatus("ENTREGUE");

    Troca troca = new Troca(
        1L,
        pedido,
        List.of(itens.get(0)),
        "Produto danificado"
    );

    model.addAttribute("trocas", List.of(troca));

    return "admin-trocas";
}
@GetMapping("/trocas/{id}")
public String detalheTroca(@PathVariable Long id, Model model) {

    Cliente cliente = DadosFalsos.clienteExemplo(1L);
    List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

    Pedido pedido = new Pedido(
        1001L,
        cliente,
        itens,
        10.00,
        171.80
    );

    pedido.setStatus("ENTREGUE");

    Troca troca = new Troca(
        id,
        pedido,
        List.of(itens.get(0)),
        "Produto danificado"
    );

    model.addAttribute("troca", troca);

    return "admin-troca-detalhe";
}
@GetMapping("/analise")
public String analise() {
    return "admin-analise";
}

}