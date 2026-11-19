package com.fatec.livraria.service;

import com.fatec.livraria.model.ModeloRecomendacao;
import com.fatec.livraria.model.PeriodoRecomendacao;
import com.fatec.livraria.repository.RecomendacaoRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecomendacaoAdminService {
    public record Painel(List<ModeloRecomendacao> versoes,
                        ModeloRecomendacao vigente, PeriodoRecomendacao periodo) {}

    private final UsuarioAtual usuario;
    private final RecomendacaoRepository repository;
    private final RecomendacaoService recomendacao;

    public RecomendacaoAdminService(UsuarioAtual usuario,
            RecomendacaoRepository repository, RecomendacaoService recomendacao) {
        this.usuario = usuario;
        this.repository = repository;
        this.recomendacao = recomendacao;
    }

    public Painel painel() {
        usuario.exigirAdministrador();
        var versoes = repository.listarVersoes();
        var vigente = versoes.isEmpty() ? null : versoes.get(0);
        var periodo = vigente == null ? PeriodoRecomendacao.PADRAO
            : new PeriodoRecomendacao(vigente.dataCorte(), vigente.fimObservacao(), vigente.fimValidacao());
        return new Painel(versoes, vigente, periodo);
    }

    @Transactional
    public ModeloRecomendacao treinar(String corte, String observacao, String validacao) {
        usuario.exigirAdministrador();
        // Compatibilidade com os testes e chamadas anteriores sem parâmetros.
        if (corte == null && observacao == null && validacao == null)
            return recomendacao.treinar();
        return recomendacao.treinar(new PeriodoRecomendacao(
            data(corte), data(observacao), data(validacao)));
    }

    private LocalDate data(String valor) {
        try {
            if (valor != null && !valor.isBlank()) return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw erro();
        }
        throw erro();
    }

    private ValidacaoException erro() {
        return new ValidacaoException(List.of("Informe as três datas válidas para o treinamento."));
    }
}