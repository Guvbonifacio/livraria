package com.fatec.livraria.service;

import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.model.Livro;
import com.fatec.livraria.repository.LivroRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class CarrinhoService {

    private final LivroRepository livroRepository;
    private final NotificacaoEstoqueService notificacoesEstoque;

    // Identifica este carrinho durante toda a sessão.
    private final String sessaoId = UUID.randomUUID().toString();

    private final Map<Long, ItemCarrinho> itens =
        new LinkedHashMap<>();

    public CarrinhoService(
        LivroRepository livroRepository,
        NotificacaoEstoqueService notificacoesEstoque) {

        this.livroRepository = livroRepository;
        this.notificacoesEstoque = notificacoesEstoque;
    }

    public String getSessaoId() {
        return sessaoId;
    }

    public synchronized void adicionar(
            Long livroId,
            int quantidade) {

        validarQuantidade(quantidade);

        Livro livro = buscarLivroDisponivel(livroId);
        ItemCarrinho existente = itens.get(livroId);

        // Usa long para evitar estouro ao somar quantidades.
        long quantidadeFinal =
            (existente == null ? 0L : existente.getQuantidade())
            + quantidade;

        validarEstoque(livro, quantidadeFinal);

        // Só modifica o carrinho depois das validações.
        itens.put(
            livroId,
            new ItemCarrinho(livro, (int) quantidadeFinal)
        );
    }

    public synchronized void alterarQuantidade(
            Long livroId,
            int quantidade) {

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
                item.getLivro().getId(),
                sessaoId
            );

            if (atual == null) {
                atual = indisponivel(item.getLivro());
            }

            // Devolve cópias, preservando os itens da sessão.
            resultado.add(
                new ItemCarrinho(
                    atual,
                    item.getQuantidade()
                )
            );
        }

        return List.copyOf(resultado);
    }

    // RN0032: listar() apenas exibe.
    // Este método efetiva a correção dos itens guardados na sessão.
    public synchronized List<String> revalidarEstoque() {
        Map<Long, ItemCarrinho> revisados = new LinkedHashMap<>();
        List<String> mensagens = new ArrayList<>();

        for (var entrada : itens.entrySet()) {
            Long livroId = entrada.getKey();
            ItemCarrinho anterior = entrada.getValue();

            // Desconta reservas de outros carrinhos,
            // preservando a identidade deste carrinho.
            Livro atual = livroRepository.buscarPorId(
                livroId,
                sessaoId
            );

            String titulo = anterior.getLivro().getTitulo();

            if (atual == null || atual.getEstoque() <= 0) {
                mensagens.add(
                    "O livro '" + titulo
                    + "' foi removido do carrinho "
                    + "porque ficou indisponível."
                );

                continue;
            }

            // Nunca aumenta a quantidade escolhida pelo cliente.
            int quantidade = Math.min(
                anterior.getQuantidade(),
                atual.getEstoque()
            );

            revisados.put(
                livroId,
                new ItemCarrinho(atual, quantidade)
            );

            if (quantidade != anterior.getQuantidade()) {
                mensagens.add(
                    "A quantidade do livro '" + titulo
                    + "' foi ajustada de "
                    + anterior.getQuantidade()
                    + " para " + quantidade
                    + " devido à disponibilidade atual."
                );

            } else if (
                    atual.getEstoque()
                    != anterior.getLivro().getEstoque()) {

                mensagens.add(
                    "A disponibilidade do livro '" + titulo
                    + "' mudou de "
                    + anterior.getLivro().getEstoque()
                    + " para " + atual.getEstoque()
                    + " unidade(s). "
                    + "A quantidade escolhida foi mantida."
                );
            }
        }

        // Confirma notificações e logs antes de alterar a sessão.
        // Se a gravação falhar, o carrinho original permanece intacto.
        if (!mensagens.isEmpty()) {
            notificacoesEstoque.registrar(
                List.copyOf(mensagens)
            );
        }

        itens.clear();
        itens.putAll(revisados);

        return List.copyOf(mensagens);
    }

    public BigDecimal calcularTotal(
            List<ItemCarrinho> itensExibidos) {

        return itensExibidos.stream()
            .map(item -> item.getSubtotalExato())
            .reduce(
                BigDecimal.ZERO,
                (acumulado, valor) -> acumulado.add(valor)
            );
    }

    public synchronized void limpar() {
        itens.clear();
    }

    private Livro buscarLivroDisponivel(Long id) {
        Livro livro = id == null
            ? null
            : livroRepository.buscarPorId(id, sessaoId);

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
            throw erro(
                "A quantidade deve ser maior que zero."
            );
        }
    }

    private void validarEstoque(
            Livro livro,
            long quantidade) {

        if (quantidade > livro.getEstoque()) {
            throw erro(
                "Quantidade solicitada acima do estoque disponível. "
                + "Disponível: "
                + livro.getEstoque()
                + " unidade(s)."
            );
        }
    }

    // Mantém o item visível para remoção caso o livro
    // seja excluído ou inativado.
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
        return new ValidacaoException(
            List.of(mensagem)
        );
    }
}