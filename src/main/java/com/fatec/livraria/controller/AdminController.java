package com.fatec.livraria.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.DadosFalsos;
import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.model.Pedido;
import com.fatec.livraria.model.Troca;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // Painel Geral
    @GetMapping
    public String admin() {
        return "admin";
    }

    // --- CLIENTES ---

    @GetMapping("/clientes")
    public String listarClientes(Model model) {
        List<Cliente> clientes = List.of(DadosFalsos.clienteExemplo(1L));
        model.addAttribute("clientes", clientes);
        return "admin-clientes";
    }

    @GetMapping("/clientes/{id}")
    public String detalheCliente(@PathVariable Long id, Model model) {
        Cliente cliente = DadosFalsos.clienteExemplo(id);
        model.addAttribute("cliente", cliente);
        return "admin-cliente-detalhe";
    }

    @PostMapping("/clientes/{id}/inativar")
    public String inativarCliente(@PathVariable Long id) {
        // Simulação de inativação de cliente
        return "redirect:/admin/clientes";
    }

    @GetMapping("/pedidos")
    public String listarPedidos(Model model) {
        Cliente cliente = DadosFalsos.clienteExemplo(1L);
        List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

        Pedido pedido = new Pedido(1001L, cliente, itens, 10.00, 171.80);
        pedido.setDataPedido(LocalDateTime.of(2026, 8, 20, 14, 30));
        pedido.setStatus("EM PROCESSAMENTO");

        model.addAttribute("pedidos", List.of(pedido));
        return "admin-pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String detalhePedidoAdmin(@PathVariable Long id, Model model) {
        Cliente cliente = DadosFalsos.clienteExemplo(1L);
        List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

        Pedido pedido = new Pedido(id, cliente, itens, 10.00, 171.80);
        pedido.setDataPedido(LocalDateTime.of(2026, 8, 20, 14, 30));
        pedido.setStatus("EM PROCESSAMENTO");

        model.addAttribute("pedido", pedido);
        return "admin-pedido-detalhe";
    }

    @PostMapping("/pedidos/{id}/status")
    public String alterarStatusPedido(@PathVariable Long id, @RequestParam String novoStatus) {
        // Simulação de alteração do status do pedido
        return "redirect:/admin/pedidos/" + id;
    }

    @GetMapping("/trocas")
    public String listarTrocas(Model model) {
        Cliente cliente = DadosFalsos.clienteExemplo(1L);
        List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

        Pedido pedido = new Pedido(1001L, cliente, itens, 10.00, 171.80);
        pedido.setStatus("ENTREGUE");

        Troca troca = new Troca(1L, pedido, List.of(itens.get(0)), "Produto danificado");

        model.addAttribute("trocas", List.of(troca));
        return "admin-trocas";
    }

    @GetMapping("/trocas/{id}")
    public String detalheTroca(@PathVariable Long id, Model model) {
        Cliente cliente = DadosFalsos.clienteExemplo(1L);
        List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

        Pedido pedido = new Pedido(1001L, cliente, itens, 10.00, 171.80);
        pedido.setStatus("ENTREGUE");

        Troca troca = new Troca(id, pedido, List.of(itens.get(0)), "Produto danificado");

        model.addAttribute("troca", troca);
        return "admin-troca-detalhe";
    }

    @PostMapping("/trocas/{id}/analisar")
    public String analisarTroca(@PathVariable Long id, @RequestParam String decisao) {
        // Simulação de aceite ou negação da troca
        return "redirect:/admin/trocas/" + id;
    }

    @PostMapping("/trocas/{id}/receber")
    public String receberItemTroca(@PathVariable Long id, @RequestParam Boolean retornaEstoque) {
        // Simulação da confirmação de recebimento e retorno ao estoque
        return "redirect:/admin/trocas/" + id;
    }

    @PostMapping("/trocas/{id}/finalizar")
    public String finalizarTroca(@PathVariable Long id) {
        // Simulação de geração de cupom de troca e encerramento
        return "redirect:/admin/trocas/" + id;
    }

    // --- ANÁLISE ---

    @GetMapping("/analise")
    public String analise(
        Model model,
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim,
        @RequestParam(required = false) List<String> categorias){
        model.addAttribute("rotulos", DadosFalsos.mesesVendas());
        model.addAttribute("series", DadosFalsos.seriesVendas());
        model.addAttribute("categoriasDisponiveis", DadosFalsos.categorias());

        return "admin-analise";
    }
}
