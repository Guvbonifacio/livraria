package com.fatec.livraria.service;

import java.util.List;

public class ValidacaoException extends RuntimeException {
    private final List<String> erros;
    public ValidacaoException(List<String> erros) {
        super(String.join("; ", erros));
        this.erros = erros;
    }
    
    public List<String> getErros() {
        return erros;
    }
}