package com.fatec.livraria.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ModeloRecomendacao(
    Long id,
    String versao,
    LocalDateTime treinadoEm,
    LocalDate dataCorte,
    LocalDate fimObservacao,
    LocalDate fimValidacao,
    int amostras,
    int iteracoes,
    double taxaAprendizado,
    double perdaInicial,
    double perdaFinal,
    Double metricaValidacao,
    double intercepto,
    List<Coeficiente> coeficientes,
    Map<Long, Double> vieses
) {
    public static final List<String> FEATURES = List.of(
        "quantidade",
        "valor",
        "recencia",
        "peso_declarado",
        "votos_uteis",
        "votos_nao_uteis"
    );

    public record Coeficiente(
        String feature,
        double coeficiente,
        double media,
        double desvio
    ) {}

    public ModeloRecomendacao {
        coeficientes = List.copyOf(coeficientes);
        vieses = Map.copyOf(vieses);
    }
}