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
            registrarFalha(
                "IA falhou provedor={} motivo={}",
                primeira
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
                registrarFalha(
                    "IA reserva falhou provedor={} motivo={}",
                    segunda
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

    // A causa é essencial para diagnosticar rede e TLS, mas uma falha de
    // credencial pode carregar a chave na mensagem: dessa só o tipo é logado.
    private void registrarFalha(
            String formato,
            FalhaProvedor falha) {

        if (falha.tipo() == FalhaProvedor.Tipo.CREDENCIAL) {
            LOG.warn(
                formato,
                falha.provedor(),
                falha.tipo()
            );

            return;
        }

        LOG.warn(
            formato,
            falha.provedor(),
            falha.tipo(),
            falha
        );
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