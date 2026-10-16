package com.fatec.livraria.service;

import org.springframework.stereotype.Service;

@Service
public class UsuarioAtual {

    public String getIdentificador() {
        // Lacuna: ainda não há autenticação para identificar o responsável.
        // Substituir o valor fixo pelo identificador do usuário autenticado.
        return "usuario-teste";
    }

    public Long getClienteId() {
    // Lacuna: ainda não há autenticação.
    // Usa provisoriamente o cliente de demonstração criado pelo data.sql.
    // Substituir pelo ID do cliente autenticado futuramente.
    return 1L;
}
}