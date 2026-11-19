package com.fatec.livraria.controller;

import com.fatec.livraria.service.ChatbotService;
import com.fatec.livraria.service.UsuarioAtual;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ChatbotAdvice {
    private final ChatbotService chat;
    private final UsuarioAtual usuario;
    private final String modo;
    public ChatbotAdvice(ChatbotService chat,UsuarioAtual usuario,
            @Value("${livraria.ia.principal:claude}") String modo) {
        this.chat=chat;this.usuario=usuario;this.modo=modo;
    }
    @ModelAttribute("chatHistorico")
    public List<ChatbotService.MensagemExibida> historico() {return chat.historico();}
    @ModelAttribute("chatRetorno")
    public String retorno(HttpServletRequest request) {
        return request.getRequestURI()+(request.getQueryString()==null?"":"?"+request.getQueryString());
    }
    @ModelAttribute("chatModo")
    public String modo() {return modo;}
    @ModelAttribute("clienteAtualId")
    public Long cliente() {return usuario.getClienteId();}
}