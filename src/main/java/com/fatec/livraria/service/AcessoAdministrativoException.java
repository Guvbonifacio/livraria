package com.fatec.livraria.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AcessoAdministrativoException
        extends ValidacaoException {

    public AcessoAdministrativoException() {
        super(
            List.of(
                "Acesso permitido somente ao perfil ADMIN."
            )
        );
    }
}