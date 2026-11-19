package com.fatec.livraria.ia;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class ConfiguracaoIA {

    @Bean("claudeProvedor")
    public ClaudeProvedor claude(
            @Value("${livraria.ia.claude.modelo:claude-opus-5-5}")
            String modelo,

            @Value("${livraria.ia.max-tokens:512}")
            int tokens,

            @Value("${livraria.ia.claude.timeout-segundos:20}")
            int segundos) {

        validar(modelo, tokens, segundos);

        return new ClaudeProvedor(
            modelo,
            tokens,
            Duration.ofSeconds(segundos)
        );
    }

    @Bean("ollamaProvedor")
    public OllamaProvedor ollama(
            @Value("${livraria.ia.ollama.url:http://localhost:11434}")
            String url,

            @Value("${livraria.ia.ollama.modelo:qwen2.5:7b}")
            String modelo,

            @Value("${livraria.ia.max-tokens:512}")
            int tokens,

            @Value("${livraria.ia.ollama.timeout-segundos:120}")
            int segundos,

            ObjectMapper json) {

        validar(modelo, tokens, segundos);

        return new OllamaProvedor(
            url,
            modelo,
            tokens,
            Duration.ofSeconds(segundos),
            json
        );
    }

    @Bean
    @Primary
    public ProvedorIA provedorIA(
            @Value("${livraria.ia.principal:claude}")
            String principal,

            @Qualifier("claudeProvedor")
            ProvedorIA claude,

            @Qualifier("ollamaProvedor")
            ProvedorIA ollama,
            ObjectMapper json) {

        return switch (principal) {
            case "falso" -> new ProvedorIAFalso(json);
            case "claude" ->
                new ProvedorComFallback(claude, ollama);

            case "ollama" ->
                new ProvedorComFallback(ollama, null);

            default ->
                throw new IllegalArgumentException(
                    "Provedor de IA inválido: use claude, ollama ou falso."
                );
        };
    }

    private void validar(
            String modelo,
            int tokens,
            int segundos) {

        if (modelo.isBlank()
                || tokens <= 0
                || segundos <= 0) {

            throw new IllegalArgumentException(
                "Informe modelo, limite de tokens e timeout válidos."
            );
        }
    }
}