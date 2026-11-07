package com.fatec.livraria.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

abstract class TesteComSessao {

    @Autowired
    private WebApplicationContext contextoDaSessao;

    @Autowired
    private UsuarioAtual usuarioDaSessao;

    protected MockHttpSession sessao;

    private ServletRequestAttributes atributos;

    protected String perfilInicial() {
        return "CLIENTE";
    }

    @BeforeEach
    void abrirSessaoDeTeste() {
        var request = new MockHttpServletRequest(
            contextoDaSessao.getServletContext()
        );

        sessao = new MockHttpSession(
            contextoDaSessao.getServletContext()
        );

        request.setSession(sessao);

        atributos = new ServletRequestAttributes(request);

        RequestContextHolder.setRequestAttributes(atributos);

        usuarioDaSessao.simularPerfil(perfilInicial());
    }

    @AfterEach
    void fecharSessaoDeTeste() {
        if (atributos != null) {
            atributos.requestCompleted();
        }

        if (sessao != null) {
            sessao.invalidate();
        }

        RequestContextHolder.resetRequestAttributes();
    }
}