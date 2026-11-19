package com.fatec.livraria.service;

import com.fatec.livraria.ia.ProvedorIA.Chamada;
import com.fatec.livraria.ia.ProvedorIA.Ferramenta;
import com.fatec.livraria.model.LivroChat;
import com.fatec.livraria.repository.LivroRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class FerramentasChat {
    public record Resultado(String json,boolean erro,List<LivroChat> recomendados) {}
    private final LivroRepository livros;
    private final RecomendacaoService recomendacao;
    private final UsuarioAtual usuario;
    private final ObjectMapper json;
    public FerramentasChat(LivroRepository livros,RecomendacaoService recomendacao,
            UsuarioAtual usuario,ObjectMapper json) {
        this.livros=livros;this.recomendacao=recomendacao;this.usuario=usuario;this.json=json;
    }
    public List<Ferramenta> disponiveis() {
        return List.of(
            new Ferramenta("recomendar_livros","Obtém recomendações do modelo treinado para o cliente da sessão. Use sempre ao recomendar.",Map.of(),List.of()),
            new Ferramenta("buscar_livros","Busca título, autor ou ISBN no catálogo real, inclusive livros sem estoque.",
                Map.of("termo",Map.of("type","string"),"categoria",Map.of("type","string","enum",livros.listarCategorias())),List.of()),
            new Ferramenta("detalhar_livro","Consulta dados completos e estoque atual de um livro do catálogo.",
                Map.of("livroId",Map.of("type","integer","minimum",1)),List.of("livroId")));
    }
    public Resultado executar(Chamada chamada) {
        try {
            return switch(chamada.nome()) {
                case "recomendar_livros" -> {
                    validarChaves(chamada,Set.of());
                    var indicados=recomendacao.recomendar(usuario.getClienteId()).stream()
                        .map(l->LivroChat.de(l)).toList();
                    yield new Resultado(json.writeValueAsString(Map.of("livros",indicados)),false,indicados);
                }
                case "buscar_livros" -> {
                    validarChaves(chamada,Set.of("termo","categoria"));
                    String termo=texto(chamada.argumentos().get("termo"));
                    String categoria=texto(chamada.argumentos().get("categoria"));
                    if(categoria!=null&&!livros.listarCategorias().contains(categoria))throw erro("Categoria não cadastrada.");
                    var achados=livros.consultar(termo,categoria).stream().limit(10).map(l->LivroChat.de(l)).toList();
                    yield new Resultado(json.writeValueAsString(Map.of("livros",achados)),false,List.of());
                }
                case "detalhar_livro" -> {
                    validarChaves(chamada,Set.of("livroId"));
                    Object valor=chamada.argumentos().get("livroId");
                    if(!(valor instanceof Number))throw erro("Informe um identificador numérico de livro.");
                    long id;
                    try {id=new BigDecimal(valor.toString()).longValueExact();}
                    catch(ArithmeticException|NumberFormatException e){throw erro("Identificador de livro inválido.");}
                    var livro=id>0?livros.buscarPorId(id):null;
                    if(livro==null)throw erro("Livro não encontrado no catálogo.");
                    yield new Resultado(json.writeValueAsString(Map.of("livro",LivroChat.de(livro))),false,List.of());
                }
                default -> throw erro("Ferramenta não permitida.");
            };
        }catch(ValidacaoException e){
            return new Resultado(json.writeValueAsString(Map.of("erro",String.join(" ",e.getErros()))),true,List.of());
        }
    }
    private void validarChaves(Chamada c,Set<String> permitidas) {
        if(!permitidas.containsAll(c.argumentos().keySet()))throw erro("Argumentos não permitidos.");
    }
    private String texto(Object valor) {
        if(valor==null)return null;
        if(!(valor instanceof String s)||s.length()>200)throw erro("Informe um filtro de texto de até 200 caracteres.");
        return s.isBlank()?null:s.trim();
    }
    private ValidacaoException erro(String texto){return new ValidacaoException(List.of(texto));}
}