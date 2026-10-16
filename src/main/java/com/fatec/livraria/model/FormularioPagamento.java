package com.fatec.livraria.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class FormularioPagamento {

    // A chave é o ID do cartão.
    // Valor vazio significa que o cartão não será utilizado.
    private Map<Long, String> valoresCartoes = new LinkedHashMap<>();

    public Map<Long, String> getValoresCartoes() {
        return valoresCartoes;
    }

    public void setValoresCartoes(Map<Long, String> valoresCartoes) {
        this.valoresCartoes = valoresCartoes;
    }
}