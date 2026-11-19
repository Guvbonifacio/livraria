package com.fatec.livraria.service;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class ChatSessao {
    private Long conversaId;
    private Long clienteId;
    public synchronized <T> T executar(Supplier<T> trabalho) { return trabalho.get(); }
    public synchronized Long conversa(Long cliente) {
        return cliente.equals(clienteId)?conversaId:null;
    }
    public synchronized void definir(Long cliente,Long conversa) {
        clienteId=cliente;
        conversaId=conversa;
    }
}