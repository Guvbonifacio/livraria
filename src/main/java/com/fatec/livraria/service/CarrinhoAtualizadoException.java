package com.fatec.livraria.service;

import java.util.List;

public class CarrinhoAtualizadoException
        extends ValidacaoException {

    public CarrinhoAtualizadoException(
            List<String> mensagens) {

        super(mensagens);
    }
}