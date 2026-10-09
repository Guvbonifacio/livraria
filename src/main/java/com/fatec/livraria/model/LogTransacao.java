package com.fatec.livraria.model;

import java.time.LocalDateTime;

public class LogTransacao {

    private Long id;
    private LocalDateTime dataHora;
    private String usuario;
    private String operacao;
    private String tabela;
    private Long registroId;
    private String dados;

    public LogTransacao(Long id, LocalDateTime dataHora, String usuario, String operacao, String tabela, Long registroId, String dados) {
        this.id = id;
        this.dataHora = dataHora;
        this.usuario = usuario;
        this.operacao = operacao;
        this.tabela = tabela;
        this.registroId = registroId;
        this.dados = dados;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getOperacao() {
        return operacao;
    }

    public String getTabela() {
        return tabela;
    }

    public Long getRegistroId() {
        return registroId;
    }

    public String getDados() {
        return dados;
    }
}