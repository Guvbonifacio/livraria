package com.fatec.livraria.controller;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.DadosFalsos;
import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.model.Pedido;
import com.fatec.livraria.model.Troca;
import com.fatec.livraria.service.ClienteService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ClienteService clienteService;

    public AdminController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // Painel principal do administrador
    @GetMapping
    public String admin() {
        return "admin";
    }

    // --- GERENCIAMENTO DE CLIENTES ---

    // Listagem e consulta de clientes por filtros opcionais
    @GetMapping("/clientes")
    public String listarClientes(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String email,
            Model model) {
        List<Cliente> clientes = clienteService.consultar(nome, cpf, email);
        model.addAttribute("clientes", clientes);
        return "admin-clientes";
    }

    // Exibe os detalhes do cliente selecionado
    @GetMapping("/clientes/{id}")
    public String detalheCliente(@PathVariable Long id, Model model) {
        Cliente cliente = clienteService.buscarPorId(id);
        model.addAttribute("cliente", cliente);
        return "admin-cliente-detalhe";
    }

    // Inativa o cadastro do cliente
    @PostMapping("/clientes/{id}/inativar")
    public String inativarCliente(@PathVariable Long id) {
        clienteService.inativar(id);
        return "redirect:/admin/clientes/" + id;
    }

    // --- GERENCIAMENTO DE PEDIDOS ---

    // Listagem geral de pedidos
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

    // Detalhamento do pedido
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

    // Atualização do status do pedido
    @PostMapping("/pedidos/{id}/status")
    public String alterarStatusPedido(@PathVariable Long id, @RequestParam String novoStatus) {
        return "redirect:/admin/pedidos/" + id;
    }

    // --- GERENCIAMENTO DE TROCAS ---

    // Listagem geral de solicitações de troca
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

    // Detalhes da solicitação de troca
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

    // Aceita ou nega a solicitação de troca
    @PostMapping("/trocas/{id}/analisar")
    public String analisarTroca(@PathVariable Long id, @RequestParam String decisao) {
        return "redirect:/admin/trocas/" + id;
    }

    // Confirmação de recebimento do item e opção de retorno ao estoque
    @PostMapping("/trocas/{id}/receber")
    public String receberItemTroca(@PathVariable Long id, @RequestParam Boolean retornaEstoque) {
        return "redirect:/admin/trocas/" + id;
    }

    // Conclusão da troca e geração do cupom de troca
    @PostMapping("/trocas/{id}/finalizar")
    public String finalizarTroca(@PathVariable Long id) {
        return "redirect:/admin/trocas/" + id;
    }
    
    @GetMapping("/analise") // Exibe os gráficos e estatísticas de vendas
    public String analise(
            Model model,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(required = false) List<String> categorias) {
        model.addAttribute("etiquetas", DadosFalsos.mesesVendas());
        model.addAttribute("series", DadosFalsos.seriesVendas());
        model.addAttribute("categoriasDisponiveis", DadosFalsos.categorias());

        return "admin-analise";
    }
}