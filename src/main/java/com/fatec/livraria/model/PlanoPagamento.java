package com.fatec.livraria.model;

import java.math.BigDecimal;
import java.util.List;

public record PlanoPagamento(
        List<Parcela> parcelas,
        BigDecimal excedente) {

    public PlanoPagamento {
        parcelas = List.copyOf(parcelas);
    }

    // Nunca contém número completo do cartão nem CVV.
    public record Parcela(
        String forma,
        BigDecimal valor,
        Long cartaoId,
        String numeroMascarado,
        String bandeira,
        Long cupomId
    ) {}
}