package com.fatec.livraria.service;

import org.springframework.stereotype.Service;

@Service
public class OperadoraSimulada {

    // Lacuna de integração: ainda não existe um gateway real.
    // O administrador escolhe a resposta da simulação.
    // Não recebemos nem persistimos número completo ou CVV.
    public boolean autorizar(
            Long pagamentoId,
            boolean aceiteSimulado) {

        return aceiteSimulado;
    }
}