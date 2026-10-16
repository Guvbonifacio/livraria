package com.fatec.livraria.model;

public class FormularioEnderecoCheckout {

    private String origem = "CADASTRADO";
    private Long enderecoId;
    private Endereco novoEndereco = new Endereco();
    private boolean salvarPerfil;

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public Long getEnderecoId() {
        return enderecoId;
    }

    public void setEnderecoId(Long enderecoId) {
        this.enderecoId = enderecoId;
    }

    public Endereco getNovoEndereco() {
        return novoEndereco;
    }

    public void setNovoEndereco(Endereco novoEndereco) {
        this.novoEndereco = novoEndereco;
    }

    public boolean isSalvarPerfil() {
        return salvarPerfil;
    }

    public void setSalvarPerfil(boolean salvarPerfil) {
        this.salvarPerfil = salvarPerfil;
    }
}