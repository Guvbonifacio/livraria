package com.fatec.livraria.model;

public class CartaoCredito {
    private String numero;
    private String nomeImpresso;
    private String bandeira;
    private String codigoSeguranca;
    private boolean preferencial; // -- RF0027

    public CartaoCredito(String numero, String nomeImpresso, String bandeira, String codigoSeguranca, boolean preferencial) {
        this.numero = numero;
        this.nomeImpresso = nomeImpresso;
        this.bandeira = bandeira;
        this.codigoSeguranca = codigoSeguranca;
        this.preferencial = preferencial;
    }

    public String getNumero() {
        return numero;

    }

    public String getNomeImpresso() {
        return nomeImpresso;

    }

    public String getBandeira() {
        return bandeira;

    }

    public String getCodigoSeguranca() {
        return codigoSeguranca;

    }

    public boolean isPreferencial() {
        return preferencial;

    }
}