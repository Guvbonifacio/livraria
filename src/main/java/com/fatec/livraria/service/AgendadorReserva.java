package com.fatec.livraria.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "livraria.carrinho.agendamento-ativo",
    havingValue = "true",
    matchIfMissing = true
)
public class AgendadorReserva {

    private static final Logger LOG =
        LoggerFactory.getLogger(AgendadorReserva.class);

    private final ReservaCarrinhoService reservas;

    public AgendadorReserva(ReservaCarrinhoService reservas) {
        this.reservas = reservas;
    }

    @Scheduled(
        fixedDelayString =
            "${livraria.carrinho.verificacao-ms:1000}"
    )
    public void verificar() {
        for (String sessao : reservas.listarSessoes()) {
            try {
                reservas.processar(sessao);

            } catch (RuntimeException e) {
                LOG.warn(
                    "Falha ao verificar reserva; "
                    + "haverá nova tentativa.",
                    e
                );
            }
        }
    }
}