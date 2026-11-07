package com.fatec.livraria.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class UsuarioAtual {

    private String perfil = "CLIENTE";

    public synchronized String getPerfil() {
        return perfil;
    }

    public synchronized boolean isAdministrador() {
        return "ADMIN".equals(perfil);
    }

    public synchronized void simularPerfil(String perfil) {
        if (!List.of("CLIENTE", "ADMIN")
                .contains(perfil == null ? "" : perfil)) {

            throw new ValidacaoException(
                List.of(
                    "Perfil inválido. Selecione CLIENTE ou ADMIN."
                )
            );
        }

        // Lacuna: não há autenticação.
        // Esta troca é uma simulação explícita para demonstração,
        // assim como o retorno da OperadoraSimulada.
        // Não altera o perfil de nenhum cliente no banco.
        this.perfil = perfil;
    }

    public synchronized void exigirAdministrador() {
        if (!"ADMIN".equals(perfil)) {
            throw new AcessoAdministrativoException();
        }
    }

    public synchronized String getIdentificador() {
        return "usuario-teste (" + perfil + ")";
    }

    public Long getClienteId() {
        // Lacuna: sem autenticação, o contexto comercial continua
        // sendo o cliente de demonstração 1.
        // A simulação troca somente o perfil da sessão.
        return 1L;
    }
}