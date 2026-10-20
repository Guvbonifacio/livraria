package com.fatec.livraria.service;

import com.fatec.livraria.model.FormularioPagamento;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class FinalizacaoCheckoutService {

    private final PedidoService pedidos;
    private final CarrinhoService carrinho;
    private final CheckoutService checkout;

    public FinalizacaoCheckoutService(
            PedidoService pedidos,
            CarrinhoService carrinho,
            CheckoutService checkout) {

        this.pedidos = pedidos;
        this.carrinho = carrinho;
        this.checkout = checkout;
    }

    // Impede finalizações simultâneas pela mesma sessão.
    public synchronized Long finalizar(
            FormularioPagamento formulario) {

        var itens = carrinho.listar();

        if (itens.isEmpty()) {
            throw new ValidacaoException(
                List.of("Seu carrinho está vazio.")
            );
        }

        var entrega = checkout.obterEntrega();

        if (entrega == null) {
            throw new ValidacaoException(
                List.of("Selecione o endereço antes de continuar.")
            );
        }

        Long id = pedidos.finalizar(
            itens,
            entrega.getEndereco(),
            formulario,carrinho.getSessaoId()
        );

        // Executa somente após o COMMIT.
        carrinho.limpar();
        checkout.limparEntrega();

        return id;
    }
}