package com.fatec.livraria.controller;

import com.fatec.livraria.service.UsuarioAtual;
import com.fatec.livraria.service.ValidacaoException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SessaoController {

    private final UsuarioAtual usuario;

    public SessaoController(UsuarioAtual usuario) {
        this.usuario = usuario;
    }

    @PostMapping("/sessao/perfil")
    public String alterarPerfil(
            @RequestParam String perfil) {

        try {
            usuario.simularPerfil(perfil);

        } catch (ValidacaoException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        }

        return "redirect:/";
    }
}