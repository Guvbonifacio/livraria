package com.fatec.livraria.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record LivroChat(Long id, String titulo, String autor, String categoria,
        BigDecimal preco, int estoque, String sinopse, String isbn,
        int ano, String editora, String edicao, int paginas, String dimensoes) {
    public static LivroChat de(Livro livro) {
        return new LivroChat(livro.getId(), livro.getTitulo(), livro.getAutor(), livro.getCategoria(),
            BigDecimal.valueOf(livro.getPreco()).setScale(2, RoundingMode.HALF_UP),
            livro.getEstoque(), livro.getSinopse(), livro.getIsbn(), livro.getAno(),
            livro.getEditora(), livro.getEdicao(), livro.getPaginas(), livro.getDimensoes());
    }
}