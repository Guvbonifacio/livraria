package com.fatec.livraria.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FormularioTroca {

    private List<Long> itensIds = new ArrayList<>();
    private Map<Long, String> quantidades = new LinkedHashMap<>();
    private String motivo;
    private String observacao;

    public List<Long> getItensIds() {
        return itensIds;
    }

    public void setItensIds(List<Long> itensIds) {
        this.itensIds = itensIds;
    }

    public Map<Long, String> getQuantidades() {
        return quantidades;
    }

    public void setQuantidades(Map<Long, String> quantidades) {
        this.quantidades = quantidades;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}