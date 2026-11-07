package com.fatec.livraria.controller;

import com.fatec.livraria.service.UsuarioAtual;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PerfilDaSessaoAdvice {

    private final UsuarioAtual usuario;

    public PerfilDaSessaoAdvice(UsuarioAtual usuario) {
        this.usuario = usuario;
    }

    @ModelAttribute("perfilAtual")
    public String perfilAtual() {
        return usuario.getPerfil();
    }
}