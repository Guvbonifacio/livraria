package com.fatec.livraria.service;

import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.model.Livro;
import com.fatec.livraria.repository.LivroRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class CarrinhoService {

    private final LivroRepository livroRepository;

    private final Map<Long, ItemCarrinho> itens = new LinkedHashMap<>();

    public CarrinhoService(LivroRepository livroRepository) {
        this.livroRepository = livroRepository;
    }

    public synchronized void adicionar(Long livroId, int quantidade) {
        validarQuantidade(quantidade);

        Livro livro = buscarLivroDisponivel(livroId);
        ItemCarrinho existente = itens.get(livroId);

        // Usa long para evitar estouro ao somar quantidades.
        long quantidadeFinal =
            (existente == null ? 0L : existente.getQuantidade())
            + quantidade;

        validarEstoque(livro, quantidadeFinal);

        // Só modifica o carrinho depois de todas as validações.
        itens.put(
            livroId,
            new ItemCarrinho(livro, (int) quantidadeFinal)
        );
    }

    public synchronized void alterarQuantidade(
            Long livroId, int quantidade) {
        validarQuantidade(quantidade);

        if (!itens.containsKey(livroId)) {
            throw erro("O livro não está no carrinho.");
        }

        Livro livro = buscarLivroDisponivel(livroId);

        validarEstoque(livro, quantidade);

        itens.put(
            livroId,
            new ItemCarrinho(livro, quantidade)
        );
    }

    public synchronized void remover(Long livroId) {
        if (itens.remove(livroId) == null) {
            throw erro("O livro não está no carrinho.");
        }
    }

    public synchronized List<ItemCarrinho> listar() {
        List<ItemCarrinho> resultado = new ArrayList<>();

        for (ItemCarrinho item : itens.values()) {
            Livro atual = livroRepository.buscarPorId(
                item.getLivro().getId()
            );

            if (atual == null) {
                atual = indisponivel(item.getLivro());
            }

            // Devolve uma cópia, sem expor o item guardado na sessão.
            resultado.add(
                new ItemCarrinho(atual, item.getQuantidade())
            );
        }

        return List.copyOf(resultado);
    }

    public BigDecimal calcularTotal(List<ItemCarrinho> itensExibidos) {
        return itensExibidos.stream()
            .map(item -> item.getSubtotalExato())
            .reduce(
                BigDecimal.ZERO,
            (acumulado, valor) -> acumulado.add(valor)
            );
    }

    private Livro buscarLivroDisponivel(Long id) {
        Livro livro = id == null
            ? null
            : livroRepository.buscarPorId(id);

        if (livro == null) {
            throw erro(
                "Livro não encontrado ou indisponível para venda."
            );
        }

        if (livro.getEstoque() <= 0) {
            throw erro("Livro indisponível em estoque.");
        }

        return livro;
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw erro("A quantidade deve ser maior que zero.");
        }
    }

    private void validarEstoque(Livro livro, long quantidade) {
        if (quantidade > livro.getEstoque()) {
            throw erro(
                "Quantidade solicitada acima do estoque disponível. "
                + "Disponível: "
                + livro.getEstoque()
                + " unidade(s)."
            );
        }
    }

    // Se o livro desaparecer ou for inativado, mantém o item
    // visível no carrinho para que o usuário possa removê-lo.
    private Livro indisponivel(Livro livro) {
        return new Livro(
            livro.getId(),
            livro.getTitulo(),
            livro.getAutor(),
            livro.getCategoria(),
            livro.getAno(),
            livro.getEditora(),
            livro.getEdicao(),
            livro.getIsbn(),
            livro.getPaginas(),
            livro.getSinopse(),
            livro.getDimensoes(),
            livro.getPreco(),
            0,
            livro.getImagemUrl()
        );
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }

    public synchronized void limpar() {
    itens.clear();
}
}