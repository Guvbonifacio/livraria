package com.fatec.livraria.controller;

import com.fatec.livraria.service.AcessoAdministrativoException;
import com.fatec.livraria.service.RecomendacaoService;
import com.fatec.livraria.service.RecomendacaoAdminService;
import com.fatec.livraria.service.ValidacaoException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/recomendacao")
public class RecomendacaoTreinoController {

    private final RecomendacaoService service;
    private final RecomendacaoAdminService admin;

    public RecomendacaoTreinoController(
            RecomendacaoService service, RecomendacaoAdminService admin) {

        this.service = service;
        this.admin = admin;
    }

    @GetMapping
    public String painel(Model model) {
        model.addAttribute("painel", admin.painel());
        return "admin-recomendacao";
    }

    @PostMapping(value = "/treinar", params = "origem=tela")
    public String treinarNaTela(
            @RequestParam(required = false) String dataCorte,
            @RequestParam(required = false) String fimObservacao,
            @RequestParam(required = false) String fimValidacao,
            RedirectAttributes redirect) {
        try {
            var modelo = admin.treinar(dataCorte, fimObservacao, fimValidacao);
            redirect.addFlashAttribute("sucessoTreino", "Nova versão criada: " + modelo.versao());
        } catch (AcessoAdministrativoException e) {
            throw e;
        } catch (ValidacaoException e) {
            redirect.addFlashAttribute("errosTreino", e.getErros());
            redirect.addFlashAttribute("corteInformado", dataCorte);
            redirect.addFlashAttribute("observacaoInformada", fimObservacao);
            redirect.addFlashAttribute("validacaoInformada", fimValidacao);
        }
        return "redirect:/admin/recomendacao";
    }

    @PostMapping("/treinar")
    @ResponseBody
    public Map<String, Object> treinar(
            @RequestParam(required = false) String dataCorte,
            @RequestParam(required = false) String fimObservacao,
            @RequestParam(required = false) String fimValidacao) {
        // A autorização e as regras permanecem no Service.
        var modelo = admin.treinar(dataCorte, fimObservacao, fimValidacao);

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
    @ResponseBody
    public Map<String, Double> avaliar() {
        return Map.of(
            "metrica_validacao",
            service.avaliar()
        );
    }

    @ExceptionHandler(AcessoAdministrativoException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public List<String> acessoNegado(
            AcessoAdministrativoException e) {

        return e.getErros();
    }

    @ExceptionHandler(ValidacaoException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<String> erroDeValidacao(
            ValidacaoException e) {

        return e.getErros();
    }
}