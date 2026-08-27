package com.fatec.livraria.model;

import java.time.LocalDateTime;

public class HistoricoStatus {

    private LocalDateTime data;
    private String status;

    public HistoricoStatus(LocalDateTime data, String status) {
        this.data = data;
        this.status = status;
    }

    public LocalDateTime getData() { return data; }

    public String getStatus() { return status; }
}