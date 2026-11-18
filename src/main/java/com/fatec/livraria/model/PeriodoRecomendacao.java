package com.fatec.livraria.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Datas finais inclusivas. Os limites SQL são exclusivos. */
public record PeriodoRecomendacao(
    LocalDate dataCorte,
    LocalDate fimObservacao,
    LocalDate fimValidacao
) {
    public static final PeriodoRecomendacao PADRAO =
        new PeriodoRecomendacao(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 17)
        );

    public LocalDateTime corte() {
        return dataCorte.atStartOfDay();
    }

    public LocalDateTime observacaoExclusiva() {
        return fimObservacao.plusDays(1).atStartOfDay();
    }

    public LocalDateTime validacaoExclusiva() {
        return fimValidacao.plusDays(1).atStartOfDay();
    }
}