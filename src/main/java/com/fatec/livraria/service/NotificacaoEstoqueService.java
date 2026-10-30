package com.fatec.livraria.service;

import com.fatec.livraria.repository.NotificacaoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoEstoqueService {

    private final NotificacaoRepository notificacoes;
    private final UsuarioAtual usuario;
    private final AuditoriaService auditoria;

    public NotificacaoEstoqueService(
            NotificacaoRepository notificacoes,
            UsuarioAtual usuario,
            AuditoriaService auditoria) {

        this.notificacoes = notificacoes;
        this.usuario = usuario;
        this.auditoria = auditoria;
    }

    // O aviso deve permanecer registrado quando a finalização
    // for interrompida para o cliente revisar o carrinho.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(List<String> mensagens) {
        Long clienteId = usuario.getClienteId();
        LocalDateTime agora = LocalDateTime.now();

        for (String mensagem : mensagens) {
            Long id = notificacoes.salvar(
                clienteId,
                mensagem,
                agora
            );

            auditoria.registrar(
                "INSERT",
                "notificacao",
                id,
                Map.of(
                    "clienteId", clienteId,
                    "origem", "RN0032",
                    "mensagem", mensagem
                )
            );
        }
    }
}