package com.fatec.livraria.ia;

import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Exclusivo de demonstrações e testes sem rede. As ferramentas continuam consultando dados reais. */
public class ProvedorIAFalso implements ProvedorIA {
    private final ObjectMapper json;

    public ProvedorIAFalso(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public Resposta responder(List<Mensagem> conversa, List<Ferramenta> ferramentas) {
        Mensagem ultima = conversa.get(conversa.size() - 1);
        String pergunta = conversa.stream()
            .filter(mensagem -> mensagem.papel() == Papel.CLIENTE)
            .reduce((anterior, atual) -> atual)
            .map(mensagem -> Objects.requireNonNull(mensagem).texto())
            .orElse("");

        String minuscula = pergunta.toLowerCase(Locale.ROOT);

        if (ultima.papel() == Papel.FERRAMENTA) {
            JsonNode resultado = json.readTree(ultima.resultado().conteudo());
            if (resultado.has("erro")) {
                return fim(resultado.path("erro").asString());
            }

            JsonNode livros = resultado.path("livros");
            if (livros.isArray()) {
                if (livros.isEmpty()) {
                    return fim("Nenhum livro encontrado no catálogo.");
                }
                if (duvida(minuscula) && "buscar_livros".equals(ultima.resultado().nome())) {
                    return chamar("detalhar_livro", Map.of("livroId", livros.get(0).path("id").asLong()));
                }

                List<String> linhas = new ArrayList<>();
                for (JsonNode livro : livros) {
                    linhas.add(resumo(livro));
                }
                return fim(String.join("\n", linhas));
            }

            JsonNode livro = resultado.path("livro");
            return fim(resumo(livro) + "\nSinopse: " + livro.path("sinopse").asString()
                + "\nISBN: " + livro.path("isbn").asString()
                + "; páginas: " + livro.path("paginas").asInt());
        }

        if (minuscula.contains("recomend") || minuscula.contains("sugir")) {
            return chamar("recomendar_livros", Map.of());
        }

        String termo = pergunta.replaceFirst("(?iu)^(buscar|busque|procurar|procure)\\s+", "")
            .replaceFirst("(?iu)^.*?(autor|sinopse|estoque|disponibilidade)\\s+(de|do livro)\\s+", "")
            .replaceAll("[?!.]+$", "")
            .trim();

        if (duvida(minuscula) || !termo.equals(pergunta)) {
            return chamar("buscar_livros", Map.of("termo", termo));
        }

        return fim("Olá! Posso recomendar livros, buscar títulos e consultar autores, sinopses e disponibilidade.");
    }

    private boolean duvida(String texto) {
        return texto.contains("autor") || texto.contains("sinopse")
            || texto.contains("estoque") || texto.contains("disponibilidade");
    }

    private String resumo(JsonNode livro) {
        return livro.path("titulo").asString() + ", de " + livro.path("autor").asString()
            + " — R$ " + livro.path("preco").asString().replace('.', ',')
            + (livro.path("estoque").asInt() > 0 ? "; disponível." : "; indisponível em estoque.");
    }

    private Resposta chamar(String nome, Map<String, Object> argumentos) {
        return new Resposta("FALSO", "teste", "",
            List.of(new Chamada(UUID.randomUUID().toString(), nome, argumentos)),
            Parada.FERRAMENTAS, new ConteudoNativo("FALSO", List.of()));
    }

    private Resposta fim(String texto) {
        return new Resposta("FALSO", "teste", texto, List.of(),
            Parada.FIM, new ConteudoNativo("FALSO", List.of()));
    }
}