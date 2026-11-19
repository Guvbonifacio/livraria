package com.fatec.livraria.service;

import com.fatec.livraria.model.Livro;
import com.fatec.livraria.model.ModeloRecomendacao;
import com.fatec.livraria.model.ModeloRecomendacao.Coeficiente;
import com.fatec.livraria.model.PeriodoRecomendacao;
import com.fatec.livraria.repository.LivroRepository;
import com.fatec.livraria.repository.RecomendacaoRepository;
import com.fatec.livraria.repository.RecomendacaoRepository.Linha;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecomendacaoService {

    public record Amostra(
        long clienteId,
        long categoriaId,
        List<Double> features,
        int rotulo
    ) {
        public Amostra {
            features = List.copyOf(features);
        }
    }

    private static final int ITERACOES = 2000;
    private static final double TAXA = 0.05;
    private static final int LIMITE = 3;

    private final RecomendacaoRepository repository;
    private final LivroRepository livros;
    private final UsuarioAtual usuario;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public RecomendacaoService(
            RecomendacaoRepository repository,
            LivroRepository livros,
            UsuarioAtual usuario,
            AuditoriaService auditoria,
            Clock relogio) {

        this.repository = repository;
        this.livros = livros;
        this.usuario = usuario;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public List<Amostra> extrairAmostras() {
        usuario.exigirAdministrador();

        var p = PeriodoRecomendacao.PADRAO;

        var linhas = repository.extrair(
            p.corte(),
            p.observacaoExclusiva()
        );

        var parametros = normalizacao(linhas);

        return linhas.stream()
            .map(l -> new Amostra(
                l.clienteId(),
                l.categoriaId(),
                normalizar(l, parametros),
                l.rotulo()
            ))
            .toList();
    }

    @Transactional
    public ModeloRecomendacao treinar() {
        return treinar(PeriodoRecomendacao.PADRAO);
    }

    /**
     * Permite avançar as janelas nos próximos treinamentos,
     * preservando as versões já gravadas.
     */
    @Transactional
    public ModeloRecomendacao treinar(
            PeriodoRecomendacao periodo) {

        usuario.exigirAdministrador();

        if (periodo == null
                || periodo.dataCorte() == null
                || periodo.fimObservacao() == null
                || periodo.fimValidacao() == null
                || periodo.fimObservacao()
                    .isBefore(periodo.dataCorte())
                || !periodo.fimValidacao()
                    .isAfter(periodo.fimObservacao())) {

            throw erro(
                "Informe janelas de treinamento "
                + "e validação consecutivas e válidas."
            );
        }

        var agora = agora();

        if (agora.isBefore(periodo.observacaoExclusiva())) {
            throw erro(
                "A janela dos rótulos ainda não terminou."
            );
        }

        var linhas = repository.extrair(
            periodo.corte(),
            periodo.observacaoExclusiva()
        );

        if (linhas.isEmpty()
                || linhas.stream()
                    .map(linha -> linha.rotulo())
                    .distinct()
                    .count() != 2) {

            throw erro(
                "O treinamento exige amostras "
                + "positivas e negativas."
            );
        }

        var parametros = normalizacao(linhas);

        double[][] x = linhas.stream()
            .map(l -> normalizar(l, parametros)
                .stream()
                .mapToDouble(valor -> valor.doubleValue())
                .toArray())
            .toArray(double[][]::new);

        double[] pesos =
            new double[ModeloRecomendacao.FEATURES.size()];

        Map<Long, Double> vieses = new LinkedHashMap<>();

        linhas.forEach(
            l -> vieses.putIfAbsent(l.categoriaId(), 0.0)
        );

        double intercepto = 0;

        double inicial = perda(
            linhas, x, pesos, vieses, intercepto
        );

        for (int iteracao = 0;
                iteracao < ITERACOES;
                iteracao++) {

            double[] gradiente = new double[pesos.length];

            Map<Long, Double> gradienteVies =
                new LinkedHashMap<>();

            vieses.keySet().forEach(
                id -> gradienteVies.put(id, 0.0)
            );

            double gradienteIntercepto = 0;

            for (int i = 0; i < linhas.size(); i++) {
                var linha = linhas.get(i);

                double z =
                    intercepto + vieses.get(linha.categoriaId());

                for (int j = 0; j < pesos.length; j++) {
                    z += pesos[j] * x[i][j];
                }

                double erro = sigmoid(z) - linha.rotulo();

                gradienteIntercepto += erro;

                gradienteVies.merge(
                    linha.categoriaId(),
                    erro,
                    (acumulado, valor) -> acumulado + valor
                );

                for (int j = 0; j < pesos.length; j++) {
                    gradiente[j] += erro * x[i][j];
                }
            }

            double passo = TAXA / linhas.size();

            intercepto -= passo * gradienteIntercepto;

            for (int j = 0; j < pesos.length; j++) {
                pesos[j] -= passo * gradiente[j];
            }

            for (long id : vieses.keySet()) {
                vieses.put(
                    id,
                    vieses.get(id)
                        - passo * gradienteVies.get(id)
                );
            }

            // Remove a redundância entre intercepto e vieses.
            // A transformação preserva as previsões.
            double centro = vieses.values()
                .stream()
                .mapToDouble(valor -> valor.doubleValue())
                .average()
                .orElse(0);

            for (long id : vieses.keySet()) {
                vieses.put(id, vieses.get(id) - centro);
            }

            intercepto += centro;
        }

        var coeficientes = new ArrayList<Coeficiente>();

        for (int j = 0; j < pesos.length; j++) {
            var parametro = parametros.get(j);

            coeficientes.add(new Coeficiente(
                parametro.feature(),
                pesos[j],
                parametro.media(),
                parametro.desvio()
            ));
        }

        double fim = perda(
            linhas, x, pesos, vieses, intercepto
        );

        if (!Double.isFinite(fim)) {
            throw erro(
                "O treinamento produziu perda inválida."
            );
        }

        var modelo = new ModeloRecomendacao(
            null,
            UUID.randomUUID().toString(),
            agora,
            periodo.dataCorte(),
            periodo.fimObservacao(),
            periodo.fimValidacao(),
            linhas.size(),
            ITERACOES,
            TAXA,
            inicial,
            fim,
            null,
            intercepto,
            coeficientes,
            vieses
        );

        Double metrica =
            agora.isBefore(periodo.validacaoExclusiva())
                ? null
                : avaliarModelo(modelo);

        modelo = new ModeloRecomendacao(
            null,
            modelo.versao(),
            agora,
            modelo.dataCorte(),
            modelo.fimObservacao(),
            modelo.fimValidacao(),
            modelo.amostras(),
            ITERACOES,
            TAXA,
            inicial,
            fim,
            metrica,
            intercepto,
            coeficientes,
            vieses
        );

        Long id = repository.salvar(modelo);

        auditoria.registrar(
            "INSERT",
            "modelo_recomendacao",
            id,
            Map.of(
                "versao", modelo.versao(),
                "amostras", modelo.amostras(),
                "perdaFinal", fim
            )
        );

        for (var c : coeficientes) {
            repository.salvarCoeficiente(id, c);

            auditoria.registrar(
                "INSERT",
                "coeficiente_modelo",
                id,
                Map.of(
                    "modeloId", id,
                    "feature", c.feature(),
                    "coeficiente", c.coeficiente()
                )
            );
        }

        for (var entrada : vieses.entrySet()) {
            repository.salvarVies(
                id,
                entrada.getKey(),
                entrada.getValue()
            );

            auditoria.registrar(
                "INSERT",
                "vies_categoria",
                id,
                Map.of(
                    "modeloId", id,
                    "categoriaId", entrada.getKey(),
                    "vies", entrada.getValue()
                )
            );
        }

        return repository.buscar(id);
    }

    /**
     * Brier: média de (probabilidade - rótulo)².
     * Menor é melhor. Não altera pesos nem normalização.
     */
    public double avaliar() {
        usuario.exigirAdministrador();

        var modelo = exigirModelo();

        if (agora().isBefore(
                modelo.fimValidacao()
                    .plusDays(1)
                    .atStartOfDay())) {

            throw erro(
                "A janela de validação ainda não terminou."
            );
        }

        return avaliarModelo(modelo);
    }

    private double avaliarModelo(
            ModeloRecomendacao modelo) {

        var inicio = modelo.fimObservacao()
            .plusDays(1)
            .atStartOfDay();

        var fim = modelo.fimValidacao()
            .plusDays(1)
            .atStartOfDay();

        // Contexto conhecido no início da validação.
        // Os parâmetros continuam sendo os aprendidos no treino.
        var linhas = repository.extrair(inicio, fim);

        return linhas.stream()
            .mapToDouble(l -> Math.pow(
                pontuar(modelo, l) - l.rotulo(),
                2
            ))
            .average()
            .orElseThrow(
                () -> erro("Não há amostras para avaliar.")
            );
    }

    public Map<Long, Double> pontuarCategorias(
            Long clienteId) {

        var modelo = exigirModelo();
        var referencia = agora();

        var resultado = new LinkedHashMap<Long, Double>();

        repository.extrair(referencia, referencia)
            .stream()
            .filter(l ->
                clienteId != null
                && l.clienteId() == clienteId
            )
            .forEach(l -> resultado.put(
                l.categoriaId(),
                pontuar(modelo, l)
            ));

        if (resultado.isEmpty()) {
            throw erro(
                "Cliente não encontrado para recomendação."
            );
        }

        return Map.copyOf(resultado);
    }

    public List<Livro> recomendar(Long clienteId) {
        var scores = pontuarCategorias(clienteId);
        var categorias = repository.categoriasDosLivros();

        // Usa o maior score entre as categorias do livro.
        // Um livro em duas categorias não ganha bônus por duplicidade.
        java.util.function.ToDoubleFunction<Livro> score =
            livro -> categorias
                .getOrDefault(livro.getId(), List.of())
                .stream()
                .mapToDouble(
                    id -> scores.getOrDefault(id, 0.0)
                )
                .max()
                .orElse(0);

        return livros.consultar(null, null)
            .stream()
            .filter(l -> l.getEstoque() > 0)
            .sorted(
                Comparator.comparingDouble(score)
                    .reversed()
                    .thenComparing(livro -> livro.getId())
            )
            .limit(LIMITE)
            .toList();
    }

    private ModeloRecomendacao exigirModelo() {
        var modelo = repository.buscarAtual();

        if (modelo == null) {
            throw erro(
                "Treine uma versão do recomendador "
                + "antes de recomendar."
            );
        }

        return modelo;
    }

    private static double[] valores(Linha linha) {
        return new double[] {
            linha.quantidade(),
            linha.valor(),

            linha.dias() == null
                ? 0
                : 1.0 / (1.0 + Math.max(0, linha.dias())),

            linha.pesoDeclarado(),
            linha.votosUteis(),
            linha.votosNaoUteis()
        };
    }

    private List<Coeficiente> normalizacao(
            List<Linha> linhas) {

        if (linhas.isEmpty()) {
            throw erro(
                "Não há amostras para normalizar."
            );
        }

        var resultado = new ArrayList<Coeficiente>();

        for (int j = 0;
                j < ModeloRecomendacao.FEATURES.size();
                j++) {

            final int indice = j;

            double media = linhas.stream()
                .mapToDouble(l -> valores(l)[indice])
                .average()
                .orElseThrow();

            double variancia = linhas.stream()
                .mapToDouble(l ->
                    Math.pow(valores(l)[indice] - media, 2)
                )
                .average()
                .orElseThrow();

            double desvio = Math.sqrt(variancia);

            resultado.add(new Coeficiente(
                ModeloRecomendacao.FEATURES.get(j),
                0,
                media,
                desvio < 1e-12 ? 1 : desvio
            ));
        }

        return List.copyOf(resultado);
    }

    private static List<Double> normalizar(
            Linha linha,
            List<Coeficiente> parametros) {

        double[] valores = valores(linha);

        var resultado = new ArrayList<Double>();

        for (var parametro : parametros) {
            int indice = ModeloRecomendacao.FEATURES
                .indexOf(parametro.feature());

            resultado.add(
                (valores[indice] - parametro.media())
                    / parametro.desvio()
            );
        }

        return List.copyOf(resultado);
    }

    private static double pontuar(
            ModeloRecomendacao modelo,
            Linha linha) {

        var x = normalizar(
            linha,
            modelo.coeficientes()
        );

        double z = modelo.intercepto()
            + modelo.vieses()
                .getOrDefault(linha.categoriaId(), 0.0);

        for (int j = 0; j < x.size(); j++) {
            z += x.get(j)
                * modelo.coeficientes()
                    .get(j)
                    .coeficiente();
        }

        return sigmoid(z);
    }

    private static double sigmoid(double z) {
        if (z >= 0) {
            return 1.0 / (1 + Math.exp(-z));
        }

        return Math.exp(z) / (1 + Math.exp(z));
    }

    private static double perda(
            List<Linha> linhas,
            double[][] x,
            double[] pesos,
            Map<Long, Double> vieses,
            double intercepto) {

        double soma = 0;

        for (int i = 0; i < linhas.size(); i++) {
            double z =
                intercepto
                + vieses.get(linhas.get(i).categoriaId());

            for (int j = 0; j < pesos.length; j++) {
                z += pesos[j] * x[i][j];
            }

            // Entropia cruzada em forma numericamente estável.
            soma += Math.max(z, 0)
                - linhas.get(i).rotulo() * z
                + Math.log1p(Math.exp(-Math.abs(z)));
        }

        return soma / linhas.size();
    }

    private LocalDateTime agora() {
        // Mesmo calendário local usado por data_pedido e auditoria.
        return LocalDateTime.ofInstant(
            relogio.instant(),
            ZoneId.systemDefault()
        );
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }
}