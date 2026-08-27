package com.fatec.livraria.controller;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.DadosFalsos;
import com.fatec.livraria.model.HistoricoStatus;
import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PedidoController {

    @GetMapping("/pedidos") // Página geral que exibe os pedidos do cliente
    public String pedidos(Model model) {
        Cliente cliente = DadosFalsos.clienteExemplo(1L);
        List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

        Pedido pedido = new Pedido(1001L, cliente, itens, 10.00, 151.80);

        model.addAttribute("pedidos", List.of(pedido));

        return "pedidos";
    }

    @GetMapping("/pedidos/{id}") // Página específica de detalhes com base no id do pedido
    public String detalhesPedido(@PathVariable Long id, Model model) {
        Cliente cliente = DadosFalsos.clienteExemplo(1L);
        List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

        Pedido pedido = new Pedido(id, cliente, itens, 10.00, 171.80);
        pedido.setDataPedido(LocalDateTime.of(2026, 8, 20, 14, 30));

        // Povoando o histórico dentro do método
        pedido.getHistorico().add(
            new HistoricoStatus(LocalDateTime.of(2026,8,20,14,30),"PEDIDO REALIZADO")
        );
        pedido.getHistorico().add(
            new HistoricoStatus(LocalDateTime.of(2026,8,20,14,35),"PAGAMENTO APROVADO")
        );
        pedido.getHistorico().add(
            new HistoricoStatus(LocalDateTime.of(2026,8,21,9,0),"EM PROCESSAMENTO")
        );

        model.addAttribute("pedido", pedido);

        return "detalhes-pedido";
    }

    @GetMapping("/pedidos/{id}/troca")
    public String solicitarTroca(@PathVariable Long id, Model model) {

        Cliente cliente = DadosFalsos.clienteExemplo(1L);
        List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

        Pedido pedido = new Pedido(id,cliente,itens,10.00,171.80);

    pedido.setDataPedido(
        LocalDateTime.of(2026, 8, 20, 14, 30)
    );

    pedido.setStatus("ENTREGUE");

    model.addAttribute("pedido", pedido);

    return "solicitar-troca";
}

    @GetMapping("/pedidos/{id}/troca/despacho")
    public String informarDespacho(@PathVariable Long id, Model model) {

    model.addAttribute("pedidoId", id);

    return "informar-despacho";
}
}