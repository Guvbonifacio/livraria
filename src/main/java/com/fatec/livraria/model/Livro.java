package com.fatec.livraria.model;

public class Livro {  

    private Long id;
    private String titulo;
    private String autor;
    private String categoria;
    private int ano;
    private String editora;
    private String edicao;
    private String isbn;
    private int paginas;
    private String sinopse;
    private String dimensoes; // Ex: "21 x 14 x 1.5 cm - 350g"
    private double preco;
    private int estoque;
    private String imagemUrl;
    // atributos com base no RN0011 

public Livro(Long id, String titulo, String autor, String categoria, int ano, String editora, String edicao, String isbn, int paginas,
    String sinopse, String dimensoes, double preco, int estoque, String imagemUrl) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.categoria = categoria;
        this.ano = ano;
        this.editora = editora;
        this.edicao = edicao;
        this.isbn = isbn;
        this.paginas = paginas;
        this.sinopse = sinopse;
        this.dimensoes = dimensoes;
        this.preco = preco;
        this.estoque = estoque;
        this.imagemUrl = imagemUrl;
    } //construtor completo.

    public Long getId() {
        return id; 
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getAno(){
        return ano; 
    }

    public String getEditora() {
        return editora;
    }
    public String getEdicao() {
        return edicao;
    }
    public String getIsbn() {
        return isbn;

    }
    public int getPaginas() {
        return paginas;

    }
    public String getSinopse() {
        return sinopse;

    }
    public String getDimensoes() {
        return dimensoes;

    }
    public double getPreco() {
        return preco;

    }
    public int getEstoque() {
        return estoque;

    }
    public String getImagemUrl() {
        return imagemUrl;

    }
}