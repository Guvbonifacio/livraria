package com.fatec.livraria.model;

    public class Endereco {
    private String nomeCurto; // Ex: "Casa", "Trabalho" --RF0026
    private String tipoResidencia;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cep;
    private String cidade;
    private String estado;

    public Endereco(String nomeCurto, String tipoResidencia, String logradouro, String numero, String bairro, String cep, String cidade, String estado) {
        this.nomeCurto = nomeCurto;
        this.tipoResidencia = tipoResidencia;
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cep = cep;
        this.cidade = cidade;
        this.estado = estado;
    }

    public String getNomeCurto() {
        return nomeCurto;
    }

    public String getTipoResidencia() {
        return tipoResidencia;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCep() {
        return cep;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }
}