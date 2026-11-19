package com.fatec.livraria.controller;

import com.fatec.livraria.service.AcessoAdministrativoException;
import com.fatec.livraria.service.RecomendacaoService;
import com.fatec.livraria.service.ValidacaoException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/recomendacao")
public class RecomendacaoTreinoController {

    private final RecomendacaoService service;

    public RecomendacaoTreinoController(
            RecomendacaoService service) {

        this.service = service;
    }

    @PostMapping("/treinar")
    public Map<String, Object> treinar() {
        // A autorização e as regras permanecem no Service.
        var modelo = service.treinar();

        Map<String, Object> resultado = new LinkedHashMap<>();

        resultado.put("id", modelo.id());
        resultado.put("versao", modelo.versao());
        resultado.put("amostras", modelo.amostras());
        resultado.put("perda_inicial", modelo.perdaInicial());
        resultado.put("perda_final", modelo.perdaFinal());
        resultado.put(
            "metrica_validacao",
            modelo.metricaValidacao()
        );

        return resultado;
    }

    @PostMapping("/avaliar")
    public Map<String, Double> avaliar() {
        return Map.of(
            "metrica_validacao",
            service.avaliar()
        );
    }

    @ExceptionHandler(AcessoAdministrativoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public List<String> acessoNegado(
            AcessoAdministrativoException e) {

        return e.getErros();
    }

    @ExceptionHandler(ValidacaoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<String> erroDeValidacao(
            ValidacaoException e) {

        return e.getErros();
    }
}