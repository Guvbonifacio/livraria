package com.fatec.livraria.controller;

import com.fatec.livraria.model.Endereco;
import com.fatec.livraria.model.EntregaCheckout;
import com.fatec.livraria.model.FormularioEnderecoCheckout;
import com.fatec.livraria.service.CarrinhoService;
import com.fatec.livraria.service.CheckoutService;
import com.fatec.livraria.service.ValidacaoException;
import com.fatec.livraria.model.FormularioPagamento;
import com.fatec.livraria.service.PagamentoService;
import com.fatec.livraria.service.FinalizacaoCheckoutService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/carrinho")
public class CompraController {

    private final CarrinhoService carrinho;
    private final CheckoutService checkout;
    private final PagamentoService pagamentos;
    private final FinalizacaoCheckoutService finalizacao;

    public CompraController(
        CarrinhoService carrinho,
        CheckoutService checkout,
        PagamentoService pagamentos,
        FinalizacaoCheckoutService finalizacao) {

    this.carrinho = carrinho;
    this.checkout = checkout;
    this.pagamentos = pagamentos;
    this.finalizacao = finalizacao;
}

    @GetMapping("/checkout/endereco")
    public String checkoutEndereco(
            Model model,
            RedirectAttributes redirect) {
        if (carrinho.listar().isEmpty()) {
            return carrinhoVazio(redirect);
        }

        List<Endereco> enderecos = checkout.listarEnderecos();

        FormularioEnderecoCheckout formulario =
            new FormularioEnderecoCheckout();

        EntregaCheckout anterior = checkout.obterEntrega();

        if (anterior != null) {
            Endereco endereco = anterior.getEndereco();

            boolean cadastrado = endereco.getId() != null
                && enderecos.stream().anyMatch(
                    e -> e.getId().equals(endereco.getId())
                );

            if (cadastrado) {
                formulario.setEnderecoId(endereco.getId());
            } else {
                formulario.setOrigem("NOVO");
                formulario.setNovoEndereco(endereco);
            }

        } else if (enderecos.isEmpty()) {
            formulario.setOrigem("NOVO");

        } else {
            formulario.setEnderecoId(
                enderecos.get(0).getId()
            );
        }

        preencher(model, formulario, null);

        return "checkout-endereco";
    }

    @PostMapping("/checkout/endereco")
    public String processarEndereco(
            @ModelAttribute("formulario")
            FormularioEnderecoCheckout formulario,
            @RequestParam(defaultValue = "calcular") String acao,
            Model model,
            RedirectAttributes redirect) {

        if (carrinho.listar().isEmpty()) {
            return carrinhoVazio(redirect);
        }

        try {
            if ("continuar".equals(acao)) {
                checkout.confirmarEndereco(formulario);

                return "redirect:/carrinho/checkout/pagamento";
            }

            if (!"calcular".equals(acao)) {
                throw new ValidacaoException(
                    List.of("Ação inválida.")
                );
            }

            preencher(
                model,
                formulario,
                checkout.cotar(formulario)
            );

        } catch (ValidacaoException e) {
            model.addAttribute(
                "errosEntrega",
                e.getErros()
            );

            preencher(model, formulario, null);
        }

        return "checkout-endereco";
    }

    @GetMapping("/checkout/pagamento")
public String checkoutPagamento(
        Model model,
        RedirectAttributes redirect) {

    if (carrinho.listar().isEmpty()) {
        return carrinhoVazio(redirect);
    }

    var entrega = checkout.obterEntrega();

    if (entrega == null) {
        redirect.addFlashAttribute(
            "errosEntrega",
            List.of("Selecione o endereço antes de continuar.")
        );

        return "redirect:/carrinho/checkout/endereco";
    }

    preencherPagamento(
        model,
        new FormularioPagamento(),
        entrega
    );

    return "checkout-pagamento";
}

    @PostMapping("/checkout/pagamento")
public String validarPagamento(
        @ModelAttribute("pagamento")
        FormularioPagamento formulario,
        BindingResult binding,
        @RequestParam(defaultValue = "validar") String acao,
        Model model,
        RedirectAttributes redirect) {

    if (carrinho.listar().isEmpty()) {
        return carrinhoVazio(redirect);
    }

    EntregaCheckout entrega = checkout.obterEntrega();

    if (entrega == null) {
        redirect.addFlashAttribute(
            "errosEntrega",
            List.of("Selecione o endereço antes de continuar.")
        );

        return "redirect:/carrinho/checkout/endereco";
    }

    if (binding.hasErrors()) {
        model.addAttribute(
            "errosPagamento",
            List.of("Dados de pagamento inválidos.")
        );
    } else {
        try {
            if ("finalizar".equals(acao)) {
                Long pedidoId = finalizacao.finalizar(formulario);

                return "redirect:/pedidos/"
                    + pedidoId
                    + "/confirmacao";
            }

            if (!"validar".equals(acao)) {
                throw new org.springframework.web.server
                    .ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Ação inválida."
                    );
            }

            pagamentos.preparar(
                formulario,
                entrega.getTotal()
            );

            model.addAttribute("pagamentoValido", true);

        } catch (ValidacaoException e) {
            model.addAttribute(
                "errosPagamento",
                e.getErros()
            );

        } catch (org.springframework.dao.DataAccessException e) {
            model.addAttribute(
                "errosPagamento",
                List.of(
                    "Não foi possível registrar o pedido. "
                    + "Nenhuma alteração foi confirmada."
                )
            );
        }
    }

    preencherPagamento(model, formulario, entrega);

    return "checkout-pagamento";
}

private void preencherPagamento(
        Model model,
        FormularioPagamento formulario,
        EntregaCheckout entrega) {

    model.addAttribute("pagamento", formulario);
    model.addAttribute("cartoes", pagamentos.listarCartoes());
    model.addAttribute("entrega", entrega);
    model.addAttribute("subtotal", entrega.getSubtotal());
    model.addAttribute("frete", entrega.getFrete());
    model.addAttribute("totalGeral", entrega.getTotal());
    model.addAttribute("cupons", pagamentos.listarCupons());
}

    private void preencher(
            Model model,
            FormularioEnderecoCheckout formulario,
            EntregaCheckout cotacao) {
        if (formulario.getNovoEndereco() == null) {
            formulario.setNovoEndereco(new Endereco());
        }

        model.addAttribute("formulario", formulario);
        model.addAttribute(
            "enderecos",
            checkout.listarEnderecos()
        );

        model.addAttribute(
            "subtotal",
            cotacao == null
                ? carrinho.calcularTotal(carrinho.listar())
                : cotacao.getSubtotal()
        );

        model.addAttribute(
            "frete",
            cotacao == null ? null : cotacao.getFrete()
        );

        model.addAttribute(
            "totalGeral",
            cotacao == null ? null : cotacao.getTotal()
        );
    }

    private String carrinhoVazio(RedirectAttributes redirect) {
        redirect.addFlashAttribute(
            "errosCarrinho",
            List.of("Seu carrinho está vazio.")
        );

        return "redirect:/carrinho";
    }
}