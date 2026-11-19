package com.fatec.livraria.ia;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.NoCredentialsException;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static com.fatec.livraria.ia.ProvedorIA.*;

class ProvedoresIATest {

    private final ObjectMapper json = new ObjectMapper();

    private final AtomicReference<JsonNode> pedido =
        new AtomicReference<>();

    private final AtomicReference<String> corpo =
        new AtomicReference<>();

    private final AtomicInteger status =
        new AtomicInteger(200);

    private final AtomicInteger chamadas =
        new AtomicInteger();

    private HttpServer servidor;
    private String url;

    private final Ferramenta ferramenta =
        new Ferramenta(
            "buscar_livros",
            "Busca o catálogo.",
            Map.of(
                "termo", Map.of("type", "string"),
                "categoria", Map.of(
                    "type",
                    List.of("string", "null")
                )
            ),
            List.of("termo")
        );

    private final List<Mensagem> conversa = List.of(
        Mensagem.sistema("Use apenas o catálogo."),
        Mensagem.cliente("Quero fantasia.")
    );

    @BeforeEach
    void iniciar() throws Exception {
        servidor = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0),
            0
        );

        servidor.createContext("/", exchange -> {
            chamadas.incrementAndGet();

            pedido.set(
                json.readTree(
                    exchange.getRequestBody().readAllBytes()
                )
            );

            byte[] bytes = corpo.get()
                .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add(
                "Content-Type",
                "application/json"
            );

            exchange.sendResponseHeaders(
                status.get(),
                bytes.length
            );

            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        servidor.start();

        url = "http://127.0.0.1:"
            + servidor.getAddress().getPort();
    }

    @AfterEach
    void encerrar() {
        servidor.stop(0);
    }

    private ClaudeProvedor claude() {
        return new ClaudeProvedor(
            "claude-opus-5-5",
            512,
            Duration.ofSeconds(3),
            () -> AnthropicOkHttpClient.builder()
                .apiKey("chave-falsa-de-teste")
                .baseUrl(url)
                .build()
        );
    }

    private OllamaProvedor ollama() {
        return new OllamaProvedor(
            url,
            "qwen2.5:7b",
            512,
            Duration.ofSeconds(3),
            json
        );
    }

    private String respostaClaude(
            String conteudo,
            String motivo) {

        return """
            {
              "id":"msg_teste",
              "type":"message",
              "role":"assistant",
              "model":"claude-opus-5-5",
              "content":%s,
              "stop_reason":"%s",
              "stop_sequence":null,
              "usage":{
                "input_tokens":10,
                "output_tokens":5
              }
            }
            """.formatted(conteudo, motivo);
    }

    @Test
    void claudePreservaFerramentasAssinaturasEIdentificadores() {
        corpo.set(respostaClaude("""
            [
              {
                "type":"thinking",
                "thinking":"Raciocínio de teste.",
                "signature":"assinatura-teste"
              },
              {
                "type":"tool_use",
                "id":"toolu_1",
                "name":"buscar_livros",
                "input":{"termo":"fantasia"}
              },
              {
                "type":"tool_use",
                "id":"toolu_2",
                "name":"buscar_livros",
                "input":{"termo":"clássico"}
              }
            ]
            """, "tool_use"));

        try (var provedor = claude()) {
            var resposta = provedor.responder(
                conversa, List.of(ferramenta)
            );

            assertEquals("CLAUDE", resposta.provedor());
            assertEquals(
                Parada.FERRAMENTAS,
                resposta.parada()
            );
            assertEquals(2, resposta.chamadas().size());

            assertEquals(
                "fantasia",
                resposta.chamadas().get(0)
                    .argumentos().get("termo")
            );

            assertEquals(
                "Use apenas o catálogo.",
                pedido.get().path("system").asString()
            );

            var esquema = pedido.get()
                .path("tools").get(0)
                .path("input_schema");

            assertEquals(
                "object",
                esquema.path("type").asString()
            );

            assertEquals(
                "termo",
                esquema.path("required").get(0).asString()
            );

            var continuacao =
                new ArrayList<>(conversa);

            continuacao.add(resposta.mensagem());

            continuacao.add(
                Mensagem.resultado(
                    resposta.chamadas().get(0),
                    "[{\"id\":1}]",
                    false
                )
            );

            continuacao.add(
                Mensagem.resultado(
                    resposta.chamadas().get(1),
                    "Nenhum livro encontrado.",
                    true
                )
            );

            corpo.set(respostaClaude("""
                [
                  {
                    "type":"text",
                    "text":"O Hobbit."
                  }
                ]
                """, "end_turn"));

            var fim = provedor.responder(
                continuacao, List.of(ferramenta)
            );

            assertEquals(Parada.FIM, fim.parada());
            assertEquals("O Hobbit.", fim.texto());

            var mensagens =
                pedido.get().path("messages");

            assertEquals(3, mensagens.size());

            assertEquals(
                "assinatura-teste",
                mensagens.get(1)
                    .path("content").get(0)
                    .path("signature").asString()
            );

            var resultados =
                mensagens.get(2).path("content");

            assertEquals(2, resultados.size());

            assertEquals(
                "toolu_1",
                resultados.get(0)
                    .path("tool_use_id").asString()
            );

            assertFalse(
                resultados.get(0)
                    .path("is_error").asBoolean()
            );

            assertEquals(
                "toolu_2",
                resultados.get(1)
                    .path("tool_use_id").asString()
            );

            assertTrue(
                resultados.get(1)
                    .path("is_error").asBoolean()
            );
        }
    }

    @Test
    void ollamaUsaOMesmoContratoDeFerramentas() {
        corpo.set("""
            {
              "model":"qwen2.5:7b",
              "done":true,
              "done_reason":"stop",
              "message":{
                "role":"assistant",
                "content":"",
                "tool_calls":[{
                  "function":{
                    "name":"buscar_livros",
                    "arguments":{
                      "termo":"fantasia",
                      "categoria":null
                    }
                  }
                }]
              }
            }
            """);

        try (var provedor = ollama()) {
            var resposta = provedor.responder(
                conversa, List.of(ferramenta)
            );

            assertEquals("OLLAMA", resposta.provedor());
            assertEquals(
                Parada.FERRAMENTAS,
                resposta.parada()
            );

            assertFalse(
                pedido.get().path("stream").asBoolean()
            );

            assertEquals(
                512,
                pedido.get().path("options")
                    .path("num_predict").asInt()
            );

            assertEquals(
                "buscar_livros",
                pedido.get().path("tools").get(0)
                    .path("function").path("name").asString()
            );

            var chamada = resposta.chamadas().get(0);

            assertFalse(chamada.id().isBlank());

            assertTrue(
                chamada.argumentos().containsKey("categoria")
            );

            assertNull(
                chamada.argumentos().get("categoria")
            );

            var continuacao =
                new ArrayList<>(conversa);

            continuacao.add(resposta.mensagem());

            continuacao.add(
                Mensagem.resultado(
                    chamada,
                    "[{\"id\":1}]",
                    false
                )
            );

            corpo.set("""
                {
                  "model":"qwen2.5:7b",
                  "done":true,
                  "done_reason":"stop",
                  "message":{
                    "role":"assistant",
                    "content":"O Hobbit."
                  }
                }
                """);

            assertEquals(
                "O Hobbit.",
                provedor.responder(
                    continuacao,
                    List.of(ferramenta)
                ).texto()
            );

            var mensagens =
                pedido.get().path("messages");

            assertEquals(
                "assistant",
                mensagens.get(2).path("role").asString()
            );

            assertEquals(
                "buscar_livros",
                mensagens.get(2)
                    .path("tool_calls").get(0)
                    .path("function").path("name").asString()
            );

            assertEquals(
                "tool",
                mensagens.get(3).path("role").asString()
            );

            assertEquals(
                "buscar_livros",
                mensagens.get(3)
                    .path("tool_name").asString()
            );

            assertEquals(
                "[{\"id\":1}]",
                mensagens.get(3).path("content").asString()
            );
        }
    }

    @Test
    void limiteDeTokensNaoEhFimNormal() {
        corpo.set(respostaClaude("""
            [{"type":"text","text":"Parcial"}]
            """, "max_tokens"));

        try (var provedor = claude()) {
            assertEquals(
                Parada.LIMITE,
                provedor.responder(
                    conversa, List.of()
                ).parada()
            );
        }

        corpo.set("""
            {
              "model":"qwen2.5:7b",
              "done":true,
              "done_reason":"length",
              "message":{
                "role":"assistant",
                "content":"Parcial"
              }
            }
            """);

        try (var provedor = ollama()) {
            assertEquals(
                Parada.LIMITE,
                provedor.responder(
                    conversa, List.of()
                ).parada()
            );
        }
    }

    @Test
    void credencialRecusadaPermiteFallbackSemRetries() {
        status.set(401);

        corpo.set("""
            {
              "type":"error",
              "error":{
                "type":"authentication_error",
                "message":"Chave inválida."
              }
            }
            """);

        try (var provedor = claude()) {
            var erro = assertThrows(
                FalhaProvedor.class,
                () -> provedor.responder(
                    conversa, List.of()
                )
            );

            assertEquals(
                FalhaProvedor.Tipo.CREDENCIAL,
                erro.tipo()
            );

            assertTrue(erro.permiteFallback());
            assertEquals(1, chamadas.get());
        }
    }

    @Test
    void modeloAusenteNaoEhMascarado() {
        status.set(404);

        corpo.set("""
            {
              "type":"error",
              "error":{
                "type":"not_found_error",
                "message":"Modelo ausente."
              }
            }
            """);

        try (var provedor = claude()) {
            var erro = assertThrows(
                FalhaProvedor.class,
                () -> provedor.responder(
                    conversa, List.of()
                )
            );

            assertEquals(
                FalhaProvedor.Tipo.CONFIGURACAO,
                erro.tipo()
            );

            assertFalse(erro.permiteFallback());
        }

        try (var provedor = ollama()) {
            var erro = assertThrows(
                FalhaProvedor.class,
                () -> provedor.responder(
                    conversa, List.of()
                )
            );

            assertEquals(
                FalhaProvedor.Tipo.CONFIGURACAO,
                erro.tipo()
            );

            assertFalse(erro.permiteFallback());
        }
    }

    @Test
    void jsonInvalidoProduzErroDeProtocolo() {
        corpo.set("isto não é JSON");

        try (var provedor = ollama()) {
            var erro = assertThrows(
                FalhaProvedor.class,
                () -> provedor.responder(
                    conversa, List.of()
                )
            );

            assertEquals(
                FalhaProvedor.Tipo.PROTOCOLO,
                erro.tipo()
            );
        }
    }

    @Test
    void ollamaDesligadoProduzErroDeConexao() {
        servidor.stop(0);

        try (var provedor = ollama()) {
            var erro = assertThrows(
                FalhaProvedor.class,
                () -> provedor.responder(
                    conversa, List.of()
                )
            );

            assertEquals(
                FalhaProvedor.Tipo.CONEXAO,
                erro.tipo()
            );
        }
    }

    @Test
    void credencialSoEhConsultadaNaPrimeiraSolicitacao() {
        var acessos = new AtomicInteger();

        try (var provedor = new ClaudeProvedor(
                "claude-opus-5-5",
                512,
                Duration.ofSeconds(3),
                () -> {
                    acessos.incrementAndGet();
                    throw new NoCredentialsException(
                        List.of()
                    );
                })) {

            assertEquals(0, acessos.get());

            var erro = assertThrows(
                FalhaProvedor.class,
                () -> provedor.responder(
                    conversa, List.of()
                )
            );

            assertEquals(
                FalhaProvedor.Tipo.CREDENCIAL,
                erro.tipo()
            );

            assertEquals(1, acessos.get());
        }
    }
}