package com.fatec.livraria.controller;

import com.fatec.livraria.service.ChatbotService;
import com.fatec.livraria.service.PreferenciaService;
import com.fatec.livraria.service.ValidacaoException;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChatbotController {
    private final ChatbotService chat;
    private final PreferenciaService preferencias;
    public ChatbotController(ChatbotService chat,PreferenciaService preferencias) {
        this.chat=chat;this.preferencias=preferencias;
    }
    @PostMapping("/chat")
    public String enviar(@RequestParam(defaultValue="") String texto,
            @RequestParam(defaultValue="/") String retorno,RedirectAttributes redirect) {
        try {chat.enviar(texto);}
        catch(ValidacaoException e) {redirect.addFlashAttribute("errosChat",e.getErros());}
        redirect.addFlashAttribute("chatAberto",true);
        return "redirect:"+retornoLocal(retorno)+"#painelChatbot";
    }
    @PostMapping("/chat/{mensagemId}/feedback")
    public String avaliar(@PathVariable Long mensagemId,@RequestParam(required=false) Boolean util,
            @RequestParam(defaultValue="/") String retorno,RedirectAttributes redirect) {
        try {chat.avaliar(mensagemId,util);}
        catch(ValidacaoException e) {redirect.addFlashAttribute("errosChat",e.getErros());}
        redirect.addFlashAttribute("chatAberto",true);
        return "redirect:"+retornoLocal(retorno)+"#painelChatbot";
    }
    @PostMapping("/clientes/{id}/preferencias")
    public String salvarPreferencias(@PathVariable Long id,@RequestParam(required=false) List<Long> categorias,
            RedirectAttributes redirect) {
        try {
            preferencias.salvar(id,categorias);
            redirect.addFlashAttribute("sucessoPreferencias","Preferências atualizadas.");
        }catch(ValidacaoException e) {redirect.addFlashAttribute("errosPreferencias",e.getErros());}
        return "redirect:/clientes/"+id;
    }
    // Impede redirecionamento externo a partir de um campo enviado pelo navegador.
    private String retornoLocal(String retorno) {
        try {
            if(retorno.length()>500 || retorno.chars().anyMatch(c -> c<32 || c=='\\')) return "/";
            URI uri=URI.create(retorno);
            return !uri.isAbsolute() && uri.getRawAuthority()==null && retorno.startsWith("/")
                && !retorno.startsWith("//") && uri.getRawFragment()==null ? retorno : "/";
        }catch(IllegalArgumentException e) {return "/";}
    }
}