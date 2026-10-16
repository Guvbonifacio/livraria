package com.fatec.livraria.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class FormularioPagamento {

    // A chave é o ID do cartão.
    // Valor vazio significa que o cartão não será utilizado.
    private Map<Long, String> valoresCartoes = new LinkedHashMap<>();

    private List<Long> cuponsIds = new ArrayList<>();

    public List<Long> getCuponsIds() {
    return cuponsIds;
    }

    public void setCuponsIds(List<Long> cuponsIds) {
    this.cuponsIds = cuponsIds;
    }
    
    public Map<Long, String> getValoresCartoes() {
        return valoresCartoes;
    }

    public void setValoresCartoes(Map<Long, String> valoresCartoes) {
        this.valoresCartoes = valoresCartoes;
    }
}