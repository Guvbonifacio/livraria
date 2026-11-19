package com.fatec.livraria.ia;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProvedorComFallback implements ProvedorIA {

    private static final Logger LOG =
        LoggerFactory.getLogger(
            ProvedorComFallback.class
        );

    private final ProvedorIA principal;
    private final ProvedorIA reserva;

    public ProvedorComFallback(
            ProvedorIA principal,
            ProvedorIA reserva) {

        this.principal = principal;
        this.reserva = reserva;
    }

    @Override
    public Resposta responder(
            List<Mensagem> conversa,
            List<Ferramenta> ferramentas) {

        // O provedor que iniciou ferramentas conclui esse turno.
        if (reserva != null && continuaNoOllama(conversa)) {
            return registrar(
                reserva.responder(conversa, ferramentas)
            );
        }

        try {
            return registrar(
                principal.responder(conversa, ferramentas)
            );

        } catch (FalhaProvedor primeira) {
            LOG.warn(
                "IA falhou provedor={} motivo={}",
                primeira.provedor(),
                primeira.tipo()
            );

            if (reserva == null
                    || !primeira.permiteFallback()) {

                throw primeira;
            }

            try {
                return registrar(
                    reserva.responder(conversa, ferramentas)
                );

            } catch (FalhaProvedor segunda) {
                LOG.warn(
                    "IA reserva falhou provedor={} motivo={}",
                    segunda.provedor(),
                    segunda.tipo()
                );

                var falha = new FalhaProvedor(
                    "NENHUM",
                    FalhaProvedor.Tipo.INDISPONIVEL,
                    "Não foi possível obter resposta dos provedores de IA.",
                    segunda
                );

                falha.addSuppressed(primeira);
                throw falha;
            }
        }
    }

    private boolean continuaNoOllama(
            List<Mensagem> conversa) {

        if (conversa.isEmpty()
                || conversa.getLast().papel()
                    != Papel.FERRAMENTA) {

            return false;
        }

        for (int i = conversa.size() - 1; i >= 0; i--) {
            var mensagem = conversa.get(i);

            if (mensagem.papel() == Papel.ASSISTENTE) {
                return mensagem.nativo() != null
                    && mensagem.nativo()
                        .provedor()
                        .equals("OLLAMA");
            }
        }

        return false;
    }

    private Resposta registrar(Resposta resposta) {
        LOG.info(
            "IA atendida provedor={} modelo={} parada={}",
            resposta.provedor(),
            resposta.modelo(),
            resposta.parada()
        );

        return resposta;
    }
}