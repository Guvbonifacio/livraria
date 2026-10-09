package com.fatec.livraria.service;

import org.springframework.stereotype.Service;

@Service
public class UsuarioAtual {

    public String getIdentificador() {
        // Lacuna: ainda não há autenticação para identificar o responsável.
        // Substituir o valor fixo pelo identificador do usuário autenticado.
        return "usuario-teste";
    }
}