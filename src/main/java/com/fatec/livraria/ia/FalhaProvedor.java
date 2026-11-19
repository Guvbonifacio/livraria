package com.fatec.livraria.ia;

public class FalhaProvedor extends RuntimeException {

    public enum Tipo {
        CREDENCIAL,
        CONEXAO,
        INDISPONIVEL,
        CONFIGURACAO,
        PROTOCOLO,
        INTERROMPIDA
    }

    private final String provedor;
    private final Tipo tipo;

    public FalhaProvedor(
            String provedor,
            Tipo tipo,
            String mensagem,
            Throwable causa) {

        super(mensagem, causa);
        this.provedor = provedor;
        this.tipo = tipo;
    }

    public String provedor() {
        return provedor;
    }

    public Tipo tipo() {
        return tipo;
    }

    public boolean permiteFallback() {
        return tipo == Tipo.CREDENCIAL
            || tipo == Tipo.CONEXAO
            || tipo == Tipo.INDISPONIVEL;
    }

    public static Tipo tipoHttp(int status) {
        if (status == 401 || status == 403) {
            return Tipo.CREDENCIAL;
        }

        if (status == 404) {
            return Tipo.CONFIGURACAO;
        }

        if (status == 429 || status >= 500) {
            return Tipo.INDISPONIVEL;
        }

        return Tipo.PROTOCOLO;
    }
}