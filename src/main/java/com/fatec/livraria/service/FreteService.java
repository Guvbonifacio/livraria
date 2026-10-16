package com.fatec.livraria.service;

import com.fatec.livraria.model.Endereco;
import com.fatec.livraria.model.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class FreteService {

    /**
     * Critério demonstrativo do projeto, sem cotação externa:
     *
     * Brasil/SP: base de R$ 15,00.
     * Demais estados brasileiros: base de R$ 25,00.
     * Outros países: base de R$ 50,00.
     *
     * Acrescenta R$ 2,00 por unidade do carrinho.
     *
     * Considera destino e itens, conforme RF0034.
     * Os valores não representam tarifas reais de transportadora.
     */
    public BigDecimal calcular(List<ItemCarrinho> itens,
                               Endereco destino) {
        if (itens == null || itens.isEmpty()) {
            throw erro("Seu carrinho está vazio.");
        }

        if (destino == null
                || vazio(destino.getPais())
                || vazio(destino.getEstado())) {
            throw erro(
                "Informe país e estado para calcular o frete."
            );
        }

        String pais = destino.getPais()
            .trim()
            .toUpperCase(Locale.ROOT);

        String estado = destino.getEstado()
            .trim()
            .toUpperCase(Locale.ROOT);

        boolean brasil = List.of(
            "BRASIL", "BR", "BRAZIL"
        ).contains(pais);

        BigDecimal base;

        if (!brasil) {
            base = new BigDecimal("50.00");
        } else if ("SP".equals(estado)) {
            base = new BigDecimal("15.00");
        } else {
            base = new BigDecimal("25.00");
        }

        long unidades = 0;

        for (ItemCarrinho item : itens) {
            if (item == null || item.getQuantidade() <= 0) {
                throw erro(
                    "Quantidade inválida para calcular o frete."
                );
            }

            unidades += item.getQuantidade();
        }

        BigDecimal adicional = new BigDecimal("2.00")
            .multiply(BigDecimal.valueOf(unidades));

        return base.add(adicional);
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }
}