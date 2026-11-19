package com.fatec.livraria.ia;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@FunctionalInterface
public interface ProvedorIA {

    Resposta responder(
        List<Mensagem> conversa,
        List<Ferramenta> ferramentas
    );

    enum Papel {
        SISTEMA, CLIENTE, ASSISTENTE, FERRAMENTA
    }

    enum Parada {
        FIM, FERRAMENTAS, LIMITE
    }

    record Chamada(
        String id,
        String nome,
        Map<String, Object> argumentos
    ) {
        public Chamada {
            Objects.requireNonNull(id);
            Objects.requireNonNull(nome);
            argumentos = copiarMapa(argumentos);
        }
    }

    record Ferramenta(
        String nome,
        String descricao,
        Map<String, Object> propriedades,
        List<String> obrigatorios
    ) {
        public Ferramenta {
            Objects.requireNonNull(nome);
            Objects.requireNonNull(descricao);
            propriedades = copiarMapa(propriedades);
            obrigatorios = List.copyOf(obrigatorios);
        }

        public Map<String, Object> esquema() {
            return Map.of(
                "type", "object",
                "properties", propriedades,
                "required", obrigatorios,
                "additionalProperties", false
            );
        }
    }

    record ResultadoFerramenta(
        String chamadaId,
        String nome,
        String conteudo,
        boolean erro
    ) {}

    record ConteudoNativo(
        String provedor,
        List<Map<String, Object>> blocos
    ) {
        public ConteudoNativo {
            blocos = blocos.stream()
                .map(bloco -> copiarMapa(bloco))
                .toList();
        }
    }

    record Mensagem(
        Papel papel,
        String texto,
        List<Chamada> chamadas,
        ResultadoFerramenta resultado,
        ConteudoNativo nativo
    ) {
        public Mensagem {
            Objects.requireNonNull(papel);
            texto = texto == null ? "" : texto;
            chamadas = List.copyOf(chamadas);

            if (papel == Papel.FERRAMENTA && resultado == null) {
                throw new IllegalArgumentException(
                    "Resultado de ferramenta obrigatório."
                );
            }
        }

        public static Mensagem sistema(String texto) {
            return new Mensagem(
                Papel.SISTEMA, texto, List.of(), null, null
            );
        }

        public static Mensagem cliente(String texto) {
            return new Mensagem(
                Papel.CLIENTE, texto, List.of(), null, null
            );
        }

        public static Mensagem resultado(
                Chamada chamada,
                String conteudo,
                boolean erro) {

            return new Mensagem(
                Papel.FERRAMENTA,
                "",
                List.of(),
                new ResultadoFerramenta(
                    chamada.id(),
                    chamada.nome(),
                    conteudo,
                    erro
                ),
                null
            );
        }
    }

    record Resposta(
        String provedor,
        String modelo,
        String texto,
        List<Chamada> chamadas,
        Parada parada,
        ConteudoNativo nativo
    ) {
        public Resposta {
            Objects.requireNonNull(provedor);
            Objects.requireNonNull(modelo);
            Objects.requireNonNull(parada);
            texto = texto == null ? "" : texto;
            chamadas = List.copyOf(chamadas);
        }

        public Mensagem mensagem() {
            return new Mensagem(
                Papel.ASSISTENTE,
                texto,
                chamadas,
                null,
                nativo
            );
        }
    }

    private static Map<String, Object> copiarMapa(
            Map<String, Object> valores) {

        // Permite null, que é um valor JSON legítimo.
        return Collections.unmodifiableMap(
            new LinkedHashMap<>(valores)
        );
    }
}