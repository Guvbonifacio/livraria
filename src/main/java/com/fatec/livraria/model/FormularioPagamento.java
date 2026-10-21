package com.fatec.livraria.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FormularioPagamento {

    private Map<Long, String> valoresCartoes = new LinkedHashMap<>();
    private List<Long> cuponsIds = new ArrayList<>();

    private boolean usarNovoCartao;
    private CartaoCredito novoCartao = new CartaoCredito();
    private String valorNovoCartao;
    private boolean salvarCartaoPerfil;

    public Map<Long, String> getValoresCartoes() {
        return valoresCartoes;
    }

    public void setValoresCartoes(Map<Long, String> valoresCartoes) {
        this.valoresCartoes = valoresCartoes;
    }

    public List<Long> getCuponsIds() {
        return cuponsIds;
    }

    public void setCuponsIds(List<Long> cuponsIds) {
        this.cuponsIds = cuponsIds;
    }

    public boolean isUsarNovoCartao() {
        return usarNovoCartao;
    }

    public void setUsarNovoCartao(boolean usarNovoCartao) {
        this.usarNovoCartao = usarNovoCartao;
    }

    public CartaoCredito getNovoCartao() {
        return novoCartao;
    }

    public void setNovoCartao(CartaoCredito novoCartao) {
        this.novoCartao = novoCartao;
    }

    public String getValorNovoCartao() {
        return valorNovoCartao;
    }

    public void setValorNovoCartao(String valorNovoCartao) {
        this.valorNovoCartao = valorNovoCartao;
    }

    public boolean isSalvarCartaoPerfil() {
        return salvarCartaoPerfil;
    }

    public void setSalvarCartaoPerfil(boolean salvarCartaoPerfil) {
        this.salvarCartaoPerfil = salvarCartaoPerfil;
    }
}