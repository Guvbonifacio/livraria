package com.fatec.livraria.ia;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "livraria.ia.diagnostico-inicial",
    havingValue = "true"
)
public class DiagnosticoIA implements ApplicationRunner {

    private static final Logger LOG =
        LoggerFactory.getLogger(DiagnosticoIA.class);

    private final ProvedorIA provedor;

    public DiagnosticoIA(ProvedorIA provedor) {
        this.provedor = provedor;
    }

    @Override
    public void run(ApplicationArguments args) {
        var resposta = provedor.responder(
            List.of(
                ProvedorIA.Mensagem.cliente(
                    "Responda apenas: integração de IA ativa."
                )
            ),
            List.of()
        );

        if (resposta.parada() != ProvedorIA.Parada.FIM
                || resposta.texto().isBlank()) {

            throw new IllegalStateException(
                "O diagnóstico não recebeu uma resposta textual completa."
            );
        }

        LOG.info(
            "Diagnóstico IA: provedor={} modelo={} resposta={}",
            resposta.provedor(),
            resposta.modelo(),
            resposta.texto()
        );
    }
}