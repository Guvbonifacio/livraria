package com.fatec.livraria.model;

public class Livro {

    private Long id;
    private String titulo;
    private String autor;
    private String categoria;
    private double preco;

    public Livro(Long id, String titulo, String autor, String categoria, double preco){
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.categoria = categoria;
        this.preco = preco;
    }

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

    public double getPreco() {
        return preco;
    }
}