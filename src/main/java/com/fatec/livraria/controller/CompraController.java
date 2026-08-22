package com.fatec.livraria.controller;

import java.util.List;
import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.model.Pedido;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.DadosFalsos;

@Controller
@RequestMapping("/carrinho") //Isso define a rota base para toda a classe. Os próximos métodos apenas adicionaram um "subcaminho"

public class CompraController {

    @GetMapping
    public String exibirCarrinho(Model model){
    List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();
    List<ItemCarrinho> expirados = DadosFalsos.itensExpiradosExemplo();
    double totalGeral = itens.stream() //Lista de itens
        .mapToDouble(item -> item.getSubtotal()) // extrai apenas o valor, dos ites, retornado pelo método GetSubtotal()
        .sum();

    model.addAttribute("itens", itens);
    model.addAttribute("itensExpirados", expirados);
    model.addAttribute("totalGeral", totalGeral);
    return "carrinho";
}

    @PostMapping("/adicionar")
    public String adicionarItem(@RequestParam Long livroId, @RequestParam(defaultValue="1") int quantidade){
        return "redirect:/carrinho";
    }

    @GetMapping("/checkout/endereco")
    public String checkoutEndereco(Model model){
    Cliente cliente = DadosFalsos.clienteExemplo(1L);

    double freteEstimado = 15.00;

    model.addAttribute("enderecos", cliente.getEnderecos());
    model.addAttribute("frete", freteEstimado);
        return "checkout-endereco";
}
    @PostMapping("/checkout/endereco")
    public String processarEndereco(
        @RequestParam(required = false) Long enderecoId,
        @RequestParam(required = false) Boolean salvarPerfil) {
        // Lógica de seleção simulada
        return "redirect:/carrinho/checkout/pagamento";
    }

    @GetMapping("/checkout/pagamento")
    public String checkoutPagamento(Model model){
    Cliente cliente = DadosFalsos.clienteExemplo(1L);
    List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();
    
    double subtotalItens = itens.stream().mapToDouble(item -> item.getSubtotal()).sum();
    double freteEstimado = 15.00;
    double totalGeral = subtotalItens + freteEstimado;
    model.addAttribute("cartoes", cliente.getCartoes());
    model.addAttribute("subtotal", subtotalItens);
    model.addAttribute("frete", freteEstimado);
    model.addAttribute("totalGeral", totalGeral);

    return "checkout-pagamento";
    }
    
    @PostMapping("/checkout/pagamento")
    public String processarPagamento(@RequestParam(required = false) Long cartaoId1,
                                     @RequestParam(required = false) Long cartaoId2,
                                     @RequestParam(required = false) String cupomDesconto) {
        // Simulação do salvamento do pedido no banco
        Long idPedidoGerado = 1001L;

        return "redirect:/carrinho/checkout/confirmacao?idPedido=" + idPedidoGerado;
    }

    @GetMapping("/checkout/confirmacao")
    public String checkoutConfirmacao(@RequestParam(name = "idPedido", defaultValue = "1001") Long idPedido, Model model) {
        Cliente cliente = DadosFalsos.clienteExemplo(1L);
        List<ItemCarrinho> itens = DadosFalsos.carrinhoExemplo();

        double subtotalItens = itens.stream().mapToDouble(item -> item.getSubtotal()).sum();
        double freteEstimado = 15.00;
        double totalGeral = subtotalItens + freteEstimado;

        // Monta o objeto Pedido simulado
        Pedido pedido = new Pedido(idPedido, cliente, itens, freteEstimado, totalGeral);

        model.addAttribute("pedido", pedido);
        model.addAttribute("subtotal", subtotalItens);

        return "checkout-confirmacao";
    }
}