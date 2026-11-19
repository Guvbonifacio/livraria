package com.fatec.livraria.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.CredentialResolutionException;
import com.anthropic.errors.NoCredentialsException;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlockParam;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ClaudeProvedor
        implements ProvedorIA, AutoCloseable {

    private final String modelo;
    private final long maxTokens;
    private final Duration timeout;
    private final Supplier<AnthropicClient> fabrica;

    private AnthropicClient cliente;

    public ClaudeProvedor(
            String modelo,
            long maxTokens,
            Duration timeout) {

        this(
            modelo,
            maxTokens,
            timeout,
            AnthropicOkHttpClient::fromEnv
        );
    }

    public ClaudeProvedor(
            String modelo,
            long maxTokens,
            Duration timeout,
            Supplier<AnthropicClient> fabrica) {

        this.modelo = modelo;
        this.maxTokens = maxTokens;
        this.timeout = timeout;
        this.fabrica = fabrica;
    }

    private synchronized AnthropicClient cliente() {
        if (cliente == null) {
            cliente = fabrica.get().withOptions(
                opcoes -> opcoes
                    .timeout(timeout)
                    .maxRetries(0)
            );
        }

        return cliente;
    }

    @Override
    public Resposta responder(
            List<Mensagem> conversa,
            List<Ferramenta> ferramentas) {

        var parametros = MessageCreateParams.builder()
            .model(modelo)
            .maxTokens(maxTokens);

        var sistema = new ArrayList<String>();
        var mensagens = new ArrayList<MessageParam>();
        var resultados = new ArrayList<ContentBlockParam>();

        for (var mensagem : conversa) {
            if (mensagem.papel() == Papel.SISTEMA) {
                sistema.add(mensagem.texto());
                continue;
            }

            if (mensagem.papel() == Papel.FERRAMENTA) {
                var resultado = mensagem.resultado();

                resultados.add(
                    ContentBlockParam.ofToolResult(
                        ToolResultBlockParam.builder()
                            .toolUseId(resultado.chamadaId())
                            .content(resultado.conteudo())
                            .isError(resultado.erro())
                            .build()
                    )
                );

                continue;
            }

            if (!resultados.isEmpty()) {
                mensagens.add(
                    MessageParam.ofUserOfContentBlockParams(
                        List.copyOf(resultados)
                    )
                );

                resultados.clear();
            }

            if (mensagem.papel() == Papel.CLIENTE) {
                mensagens.add(
                    MessageParam.ofUser(mensagem.texto())
                );
            } else {
                mensagens.add(
                    MessageParam.ofAssistantOfContentBlockParams(
                        blocos(mensagem)
                    )
                );
            }
        }

        if (!resultados.isEmpty()) {
            mensagens.add(
                MessageParam.ofUserOfContentBlockParams(
                    resultados
                )
            );
        }

        parametros.messages(mensagens);

        if (!sistema.isEmpty()) {
            parametros.system(String.join("\n", sistema));
        }

        for (var ferramenta : ferramentas) {
            var propriedades =
                Tool.InputSchema.Properties.builder();

            ferramenta.propriedades().forEach(
                (nome, esquema) ->
                    propriedades.putAdditionalProperty(
                        nome,
                        JsonValue.from(esquema)
                    )
            );

            var esquema = Tool.InputSchema.builder()
                .properties(propriedades.build())
                .required(ferramenta.obrigatorios())
                .putAdditionalProperty(
                    "additionalProperties",
                    JsonValue.from(false)
                )
                .build();

            parametros.addTool(
                Tool.builder()
                    .name(ferramenta.nome())
                    .description(ferramenta.descricao())
                    .inputSchema(esquema)
                    .build()
            );
        }

        try {
            var resposta = cliente()
                .messages()
                .create(parametros.build());

            var textos = new ArrayList<String>();
            var chamadas = new ArrayList<Chamada>();

            for (var bloco : resposta.content()) {
                if (bloco.isText()) {
                    textos.add(bloco.asText().text());
                }

                if (bloco.isToolUse()) {
                    var chamada = bloco.asToolUse();

                    Map<String, Object> argumentos =
                        chamada._input().convert(
                            new com.fasterxml.jackson.core.type.TypeReference<
                                Map<String, Object>
                            >() {}
                        );

                    chamadas.add(new Chamada(
                        chamada.id(),
                        chamada.name(),
                        argumentos
                    ));
                }
            }

            String motivo = resposta.stopReason()
                .map(valor -> valor.toString())
                .orElse("");

            Parada parada;

            if (motivo.equals("max_tokens")) {
                parada = Parada.LIMITE;

            } else if (!chamadas.isEmpty()) {
                parada = Parada.FERRAMENTAS;

            } else if (List.of(
                    "end_turn",
                    "stop_sequence",
                    "refusal"
                ).contains(motivo)) {

                parada = Parada.FIM;

            } else {
                throw new FalhaProvedor(
                    "CLAUDE",
                    FalhaProvedor.Tipo.PROTOCOLO,
                    "Claude devolveu um motivo de parada não suportado.",
                    null
                );
            }

            List<Map<String, Object>> nativos =
                JsonValue.from(resposta.content()).convert(
                    new com.fasterxml.jackson.core.type.TypeReference<
                        List<Map<String, Object>>
                    >() {}
                );

            return new Resposta(
                "CLAUDE",
                resposta.model().toString(),
                String.join("\n", textos),
                chamadas,
                parada,
                new ConteudoNativo("CLAUDE", nativos)
            );

        } catch (NoCredentialsException
                | CredentialResolutionException e) {

            throw new FalhaProvedor(
                "CLAUDE",
                FalhaProvedor.Tipo.CREDENCIAL,
                "Configure ANTHROPIC_API_KEY no ambiente do processo.",
                e
            );

        } catch (AnthropicServiceException e) {
            throw new FalhaProvedor(
                "CLAUDE",
                FalhaProvedor.tipoHttp(e.statusCode()),
                "A API Claude recusou a solicitação (HTTP "
                    + e.statusCode() + ").",
                e
            );

        } catch (AnthropicIoException e) {
            var tipo =
                Thread.currentThread().isInterrupted()
                    ? FalhaProvedor.Tipo.INTERROMPIDA
                    : FalhaProvedor.Tipo.CONEXAO;

            throw new FalhaProvedor(
                "CLAUDE",
                tipo,
                "Não foi possível conectar ao Claude.",
                e
            );

        } catch (AnthropicInvalidDataException e) {
            throw new FalhaProvedor(
                "CLAUDE",
                FalhaProvedor.Tipo.PROTOCOLO,
                "Claude devolveu uma resposta inválida.",
                e
            );
        }
    }

    private List<ContentBlockParam> blocos(
            Mensagem mensagem) {

        if (mensagem.nativo() != null
                && mensagem.nativo()
                    .provedor()
                    .equals("CLAUDE")) {

            return mensagem.nativo()
                .blocos()
                .stream()
                .map(bloco ->
                    JsonValue.from(bloco)
                        .convert(ContentBlockParam.class)
                )
                .toList();
        }

        var blocos = new ArrayList<ContentBlockParam>();

        if (!mensagem.texto().isEmpty()) {
            blocos.add(
                ContentBlockParam.ofText(mensagem.texto())
            );
        }

        for (var chamada : mensagem.chamadas()) {
            var argumentos =
                ToolUseBlockParam.Input.builder();

            chamada.argumentos().forEach(
                (nome, valor) ->
                    argumentos.putAdditionalProperty(
                        nome,
                        JsonValue.from(valor)
                    )
            );

            blocos.add(
                ContentBlockParam.ofToolUse(
                    ToolUseBlockParam.builder()
                        .id(chamada.id())
                        .name(chamada.nome())
                        .input(argumentos.build())
                        .build()
                )
            );
        }

        return blocos;
    }

    @Override
    public synchronized void close() {
        if (cliente != null) {
            cliente.close();
        }
    }
}