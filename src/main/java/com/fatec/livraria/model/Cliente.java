package com.fatec.livraria.model;

import java.util.ArrayList;
import java.util.List;

public class Cliente {
    private Long id;
    private String nome;
    private String genero;
    private String dataNascimento;
    private String cpf;
    private String telefone;
    private String email;
    private String senha;
    private List<Endereco> enderecos = new ArrayList<>();
    private List<CartaoCredito> cartoes = new ArrayList<>();

    public Cliente() {}

    public Cliente(Long id, String nome, String genero, String dataNascimento, String cpf, String telefone, String email, String senha) {
        this.id = id;
        this.nome = nome;
        this.genero = genero;
        this.dataNascimento = dataNascimento;
        this.cpf = cpf;
        this.telefone = telefone;
        this.email = email;
        this.senha = senha;
    }

    public Long getId() {
        return id;        
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(String dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }
    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public List<Endereco> getEnderecos() {
        return enderecos;
    }

    public void setEnderecos(List<Endereco> enderecos) {
        this.enderecos = enderecos;
    }

    public List<CartaoCredito> getCartoes() {
        return cartoes;
    }

    public void setCartoes(List<CartaoCredito> cartoes) {
        this.cartoes = cartoes;
    }
}