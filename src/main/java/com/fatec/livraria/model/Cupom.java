package com.fatec.livraria.model;

import java.time.LocalDate;

public class Cupom {

    private String codigo;
    private String tipo;
    private double valor;
    private LocalDate validade;
    private String situacao;

    public Cupom(String codigo, String tipo, double valor, LocalDate validade, String situacao) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.valor = valor;
        this.validade = validade;
        this.situacao = situacao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipo() {
        return tipo;
    }

    public double getValor() {
        return valor;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public String getSituacao() {
        return situacao;
    }
}