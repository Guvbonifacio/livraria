package com.fatec.livraria.model;

import java.time.LocalDateTime;

public record Notificacao(
    Long id,
    Long clienteId,
    String mensagem,
    LocalDateTime dataHora,
    boolean lida
) {}