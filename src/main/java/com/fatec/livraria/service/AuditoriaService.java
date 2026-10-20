package com.fatec.livraria.service;

import com.fatec.livraria.model.LogTransacao;
import com.fatec.livraria.repository.LogRepository;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class AuditoriaService {

    private final LogRepository logs;
    private final UsuarioAtual usuario;
    private final ObjectMapper json;

    public AuditoriaService(
            LogRepository logs,
            UsuarioAtual usuario,
            ObjectMapper json) {

        this.logs = logs;
        this.usuario = usuario;
        this.json = json;
    }

    public void registrar(
            String operacao,
            String tabela,
            Long id,
            Map<String, Object> dados) {

        logs.registrar(
            new LogTransacao(
                null,
                LocalDateTime.now(),
                usuario.getIdentificador(),
                operacao,
                tabela,
                id,
                json.writeValueAsString(dados)
            )
        );
    }
}