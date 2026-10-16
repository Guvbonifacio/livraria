package com.fatec.livraria.model;

import java.math.BigDecimal;

public class EntregaCheckout {

    private final Endereco endereco;
    private final BigDecimal subtotal;
    private final BigDecimal frete;

    public EntregaCheckout(Endereco endereco,
                           BigDecimal subtotal,
                           BigDecimal frete) {
        this.endereco = endereco;
        this.subtotal = subtotal;
        this.frete = frete;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getFrete() {
        return frete;
    }

    public BigDecimal getTotal() {
        return subtotal.add(frete);
    }
}