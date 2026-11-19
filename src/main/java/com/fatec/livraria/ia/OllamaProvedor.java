package com.fatec.livraria.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class OllamaProvedor
        implements ProvedorIA, AutoCloseable {

    private final URI endpoint;
    private final String modelo;
    private final int maxTokens;
    private final Duration timeout;
    private final ObjectMapper json;
    private final HttpClient http;

    public OllamaProvedor(
            String url,
            String modelo,
            int maxTokens,
            Duration timeout,
            ObjectMapper json) {

        URI base = URI.create(url);

        if (base.getHost() == null
                || !List.of("http", "https")
                    .contains(base.getScheme())) {

            throw new IllegalArgumentException(
                "Informe uma URL HTTP válida para o Ollama."
            );
        }

        this.endpoint = base.resolve("/api/chat");
        this.modelo = modelo;
        this.maxTokens = maxTokens;
        this.timeout = timeout;
        this.json = json;

        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public Resposta responder(
            List<Mensagem> conversa,
            List<Ferramenta> ferramentas) {

        var mensagens =
            new ArrayList<Map<String, Object>>();

        for (var mensagem : conversa) {
            Map<String, Object> item =
                new LinkedHashMap<>();

            item.put(
                "role",
                switch (mensagem.papel()) {
                    case SISTEMA -> "system";
                    case CLIENTE -> "user";
                    case ASSISTENTE -> "assistant";
                    case FERRAMENTA -> "tool";
                }
            );

            if (mensagem.papel() == Papel.FERRAMENTA) {
                var resultado = mensagem.resultado();

                item.put(
                    "content",
                    resultado.erro()
                        ? "Erro na ferramenta: "
                            + resultado.conteudo()
                        : resultado.conteudo()
                );

                item.put(
                    "tool_name",
                    resultado.nome()
                );
            } else {
                item.put("content", mensagem.texto());
            }

            if (!mensagem.chamadas().isEmpty()) {
                item.put(
                    "tool_calls",
                    mensagem.chamadas()
                        .stream()
                        .map(chamada -> Map.of(
                            "function", Map.of(
                                "name", chamada.nome(),
                                "arguments", chamada.argumentos()
                            )
                        ))
                        .toList()
                );
            }

            mensagens.add(item);
        }

        Map<String, Object> corpo = new LinkedHashMap<>();

        corpo.put("model", modelo);
        corpo.put("stream", false);
        corpo.put("messages", mensagens);
        corpo.put(
            "options",
            Map.of("num_predict", maxTokens)
        );

        if (!ferramentas.isEmpty()) {
            corpo.put(
                "tools",
                ferramentas.stream()
                    .map(ferramenta -> Map.of(
                        "type", "function",
                        "function", Map.of(
                            "name", ferramenta.nome(),
                            "description", ferramenta.descricao(),
                            "parameters", ferramenta.esquema()
                        )
                    ))
                    .toList()
            );
        }

        try {
            var pedido = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        json.writeValueAsString(corpo),
                        StandardCharsets.UTF_8
                    )
                )
                .build();

            var resposta = http.send(
                pedido,
                HttpResponse.BodyHandlers.ofString(
                    StandardCharsets.UTF_8
                )
            );

            if (resposta.statusCode() != 200) {
                throw new FalhaProvedor(
                    "OLLAMA",
                    FalhaProvedor.tipoHttp(
                        resposta.statusCode()
                    ),
                    "Ollama recusou a solicitação (HTTP "
                        + resposta.statusCode() + ").",
                    null
                );
            }

            var raiz = json.readTree(resposta.body());
            var mensagem = raiz.path("message");

            if (!raiz.path("done").asBoolean()
                    || !mensagem.isObject()
                    || !mensagem.path("role")
                        .asString()
                        .equals("assistant")) {

                throw new FalhaProvedor(
                    "OLLAMA",
                    FalhaProvedor.Tipo.PROTOCOLO,
                    "Ollama devolveu uma resposta incompleta ou inválida.",
                    null
                );
            }

            var chamadas = new ArrayList<Chamada>();
            var ferramentasChamadas =
                mensagem.path("tool_calls");

            if (!ferramentasChamadas.isMissingNode()
                    && !ferramentasChamadas.isNull()
                    && !ferramentasChamadas.isArray()) {

                throw new FalhaProvedor(
                    "OLLAMA",
                    FalhaProvedor.Tipo.PROTOCOLO,
                    "Ollama devolveu chamadas de ferramentas inválidas.",
                    null
                );
            }

            for (var chamada : ferramentasChamadas) {
                var funcao = chamada.path("function");

                if (!funcao.path("name").isString()
                        || funcao.path("name")
                            .asString()
                            .isBlank()
                        || !funcao.path("arguments")
                            .isObject()) {

                    throw new FalhaProvedor(
                        "OLLAMA",
                        FalhaProvedor.Tipo.PROTOCOLO,
                        "Ollama devolveu argumentos de ferramenta inválidos.",
                        null
                    );
                }

                Map<String, Object> argumentos =
                    json.convertValue(
                        funcao.path("arguments"),
                        new TypeReference<
                            Map<String, Object>
                        >() {}
                    );

                String id = chamada.path("id")
                    .asString("");

                chamadas.add(new Chamada(
                    id.isBlank()
                        ? "toolu_"
                            + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                        : id,
                    funcao.path("name").asString(),
                    argumentos
                ));
            }

            Parada parada;

            if (raiz.path("done_reason")
                    .asString("")
                    .equals("length")) {

                parada = Parada.LIMITE;
            } else {
                parada = chamadas.isEmpty()
                    ? Parada.FIM
                    : Parada.FERRAMENTAS;
            }

            return new Resposta(
                "OLLAMA",
                raiz.path("model").asString(modelo),
                mensagem.path("content").asString(""),
                chamadas,
                parada,
                new ConteudoNativo(
                    "OLLAMA",
                    List.of()
                )
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new FalhaProvedor(
                "OLLAMA",
                FalhaProvedor.Tipo.INTERROMPIDA,
                "A chamada ao Ollama foi interrompida.",
                e
            );

        } catch (IOException e) {
            throw new FalhaProvedor(
                "OLLAMA",
                FalhaProvedor.Tipo.CONEXAO,
                "Não foi possível conectar ao Ollama.",
                e
            );

        } catch (JacksonException e) {
            throw new FalhaProvedor(
                "OLLAMA",
                FalhaProvedor.Tipo.PROTOCOLO,
                "Ollama devolveu JSON inválido.",
                e
            );
        }
    }

    @Override
    public void close() {
        http.close();
    }
}