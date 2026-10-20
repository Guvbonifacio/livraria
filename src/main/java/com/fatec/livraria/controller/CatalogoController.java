package com.fatec.livraria.controller;

import com.fatec.livraria.model.Livro;
import com.fatec.livraria.repository.LivroRepository;
import com.fatec.livraria.service.CarrinhoService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CatalogoController {

    private final LivroRepository repository;
    private final CarrinhoService carrinho;

    public CatalogoController(LivroRepository repository, CarrinhoService carrinho) {
        this.repository = repository;
        this.carrinho = carrinho;
    }


    @GetMapping("/")
    public String catalogo(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String categoria,
            Model model) {

        model.addAttribute(
            "livros",
            repository.consultar(busca, categoria, carrinho.getSessaoId())
        );

        model.addAttribute(
            "categorias",
            repository.listarCategorias()
        );

        model.addAttribute("busca", busca);
        model.addAttribute("categoriaSelecionada", categoria);

        return "catalogo";
    }

    @GetMapping("/livro/{id}")
    public String detalharLivro(
            @PathVariable Long id,
            Model model) {

        Livro livro = repository.buscarPorId(id, carrinho.getSessaoId());

        if (livro == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Livro não encontrado."
            );
        }

        model.addAttribute("livro", livro);

        return "livro";
    }
}