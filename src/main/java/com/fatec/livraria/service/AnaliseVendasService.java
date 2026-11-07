package com.fatec.livraria.service;

import com.fatec.livraria.repository.AnaliseVendasRepository;
import com.fatec.livraria.repository.LivroRepository;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnaliseVendasService {

    public record Serie(
        String categoria,
        List<BigDecimal> valores
    ) {}

    public record Resultado(
        List<String> etiquetas,
        List<Serie> series
    ) {}

    private static final BigDecimal ZERO =
        new BigDecimal("0.00");

    private static final List<String> MESES = List.of(
        "jan", "fev", "mar", "abr", "mai", "jun",
        "jul", "ago", "set", "out", "nov", "dez"
    );

    private final AnaliseVendasRepository repository;
    private final LivroRepository livros;
    private final UsuarioAtual usuario;

    public AnaliseVendasService(
            AnaliseVendasRepository repository,
            LivroRepository livros,
            UsuarioAtual usuario) {

        this.repository = repository;
        this.livros = livros;
        this.usuario = usuario;
    }

    public Resultado analisar(
            String dataInicio,
            String dataFim,
            List<String> categoriasSelecionadas) {

        usuario.exigirAdministrador();

        LocalDate inicio = data(dataInicio, "data inicial");
        LocalDate fim = data(dataFim, "data final");

        // RF0056
        if (fim.isBefore(inicio)) {
            throw erro(
                "A data final não pode ser anterior à data inicial."
            );
        }

        LocalDate fimExclusivo;
        LocalDate minimo;
        LocalDate maximo;

        try {
            // Datas da tela são inclusivas.
            // Ex.: 01/02 a 28/02 equivale a [01/02, 01/03).
            fimExclusivo = fim.plusDays(1);
            minimo = inicio.plusMonths(1);
            maximo = inicio.plusMonths(24);

        } catch (DateTimeException e) {
            throw erro(
                "O período informado está fora do intervalo suportado."
            );
        }

        // RN0072
        if (fimExclusivo.isBefore(minimo)
                || fimExclusivo.isAfter(maximo)) {

            throw erro(
                "O período deve ser de no mínimo 1 mês "
                + "e no máximo 24 meses."
            );
        }

        List<String> disponiveis = livros.listarCategorias();

        // Ausência do filtro significa todas as categorias.
        List<String> selecionadas =
            categoriasSelecionadas == null
                ? disponiveis
                : categoriasSelecionadas;

        if (selecionadas.isEmpty()) {
            throw erro("Selecione ao menos uma categoria.");
        }

        if (selecionadas.stream().anyMatch(
                categoria -> categoria == null
                    || !disponiveis.contains(categoria))) {

            throw erro(
                "Selecione somente categorias cadastradas no sistema."
            );
        }

        // RN0073: todos os meses tocados pelo intervalo.
        List<YearMonth> meses = new ArrayList<>();

        YearMonth ultimo = YearMonth.from(fim);

        for (
            YearMonth mes = YearMonth.from(inicio);
            !mes.isAfter(ultimo);
            mes = mes.plusMonths(1)
        ) {
            meses.add(mes);
        }

        Map<String, List<BigDecimal>> valoresPorCategoria =
            new LinkedHashMap<>();

        // Inclui categorias mesmo quando não possuem nenhuma venda.
        // O filtro e a eliminação de duplicatas acontecem em Java.
        for (String categoria : disponiveis) {
            if (selecionadas.contains(categoria)) {
                valoresPorCategoria.put(
                    categoria,
                    new ArrayList<>(
                        Collections.nCopies(meses.size(), ZERO)
                    )
                );
            }
        }

        // Uma consulta de totais, com limite superior exclusivo.
        for (var linha :
                repository.consultar(inicio, fimExclusivo)) {

            List<BigDecimal> valores =
                valoresPorCategoria.get(linha.categoria());

            int indice = meses.indexOf(linha.mes());

            if (valores != null && indice >= 0) {
                BigDecimal reais = BigDecimal.valueOf(
                    linha.totalCentavos(),
                    2
                );

                valores.set(
                    indice,
                    valores.get(indice).add(reais)
                );
            }
        }

        List<String> etiquetas = meses.stream()
            .map(mes ->
                MESES.get(mes.getMonthValue() - 1)
                + "/"
                + String.format(
                    Locale.ROOT,
                    "%04d",
                    mes.getYear()
                )
            )
            .toList();

        List<Serie> series = valoresPorCategoria
            .entrySet()
            .stream()
            .map(entrada -> new Serie(
                entrada.getKey(),
                List.copyOf(entrada.getValue())
            ))
            .toList();

        return new Resultado(etiquetas, series);
    }

    private LocalDate data(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw erro("Informe a " + campo + ".");
        }

        try {
            return LocalDate.parse(valor);

        } catch (DateTimeParseException e) {
            throw erro(
                "Informe uma " + campo + " válida."
            );
        }
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }
}