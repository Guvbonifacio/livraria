package com.fatec.livraria.service;

import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.model.Livro;
import com.fatec.livraria.repository.LivroRepository;
import com.fatec.livraria.repository.ReservaCarrinhoRepository.Reserva;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class CarrinhoService {

    private final LivroRepository livroRepository;
    private final NotificacaoEstoqueService notificacoesEstoque;
    private final ReservaCarrinhoService reservas;

    private final String sessaoId = UUID.randomUUID().toString();

    private final Map<Long, ItemCarrinho> itens =
        new LinkedHashMap<>();

    private final Map<Long, ItemExpirado> retirados =
        new LinkedHashMap<>();

    private boolean expiracaoPendente;

    public record ItemExpirado(
        Livro livro,
        int quantidade,
        String horario,
        int minutos
    ) {}

    public record EstadoReserva(
        String versao,
        boolean expirou,
        String aviso
    ) {}

    public CarrinhoService(
            LivroRepository livroRepository,
            NotificacaoEstoqueService notificacoesEstoque,
            ReservaCarrinhoService reservas) {

        this.livroRepository = livroRepository;
        this.notificacoesEstoque = notificacoesEstoque;
        this.reservas = reservas;
    }

    public String getSessaoId() {
        return sessaoId;
    }

    public int getPrazoMinutos() {
        return reservas.getMinutos();
    }

    private List<Reserva> sincronizar() {
        var linhas = reservas.processar(sessaoId);

        for (var reserva : linhas) {
            if (!reserva.expirado()) {
                continue;
            }

            // Remove todas as unidades do produto da sessão.
            ItemCarrinho item = itens.remove(reserva.livroId());

            if (item != null) {
                String horario = DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm:ss")
                    .withZone(ZoneId.of("America/Sao_Paulo"))
                    .format(reserva.expiraEm());

                retirados.put(
                    reserva.livroId(),
                    new ItemExpirado(
                        item.getLivro(),
                        item.getQuantidade(),
                        horario,
                        reservas.getMinutos()
                    )
                );

                expiracaoPendente = true;
            }
        }

        return linhas;
    }

    public synchronized List<ItemExpirado> listarRetirados() {
        sincronizar();

        return List.copyOf(retirados.values());
    }

    public synchronized void confirmarLeituraExpiracao() {
        expiracaoPendente = false;
    }

    public synchronized void exigirPrazoValido() {
        sincronizar();

        if (expiracaoPendente) {
            throw new CarrinhoAtualizadoException(
                List.of(
                    "O prazo de reserva expirou. "
                    + "Revise os produtos removidos do carrinho."
                )
            );
        }
    }

    public synchronized EstadoReserva estadoReserva() {
        var ativas = sincronizar().stream()
            .filter(r -> !r.expirado())
            .toList();

        String aviso = ativas.stream().anyMatch(r -> r.avisado())
            ? "A reserva do seu carrinho está nos últimos "
                + "cinco minutos. Finalize antes do prazo."
            : "";

        String versao = ativas.stream()
            .map(r ->
                r.id() + ":" + r.expiraEm() + ":" + r.avisado()
            )
            .toList()
            + ":" + retirados.keySet();

        return new EstadoReserva(
            versao,
            expiracaoPendente,
            aviso
        );
    }

    public synchronized void adicionar(
            Long livroId,
            int quantidade) {

        sincronizar();
        validarQuantidade(quantidade);

        Livro livro = buscarLivroDisponivel(livroId);
        ItemCarrinho existente = itens.get(livroId);

        long quantidadeFinal =
            (existente == null ? 0L : existente.getQuantidade())
            + quantidade;

        validarEstoque(livro, quantidadeFinal);

        reservas.reservar(
            sessaoId,
            livroId,
            (int) quantidadeFinal,
            existente == null ? 0 : existente.getQuantidade(),
            true
        );

        retirados.remove(livroId);

        itens.put(
            livroId,
            new ItemCarrinho(livro, (int) quantidadeFinal)
        );
    }

    public synchronized void alterarQuantidade(
            Long livroId,
            int quantidade) {

        sincronizar();
        validarQuantidade(quantidade);

        if (!itens.containsKey(livroId)) {
            throw erro("O livro não está no carrinho.");
        }

        Livro livro = buscarLivroDisponivel(livroId);

        validarEstoque(livro, quantidade);

        // Alterar quantidade não reinicia o prazo.
        reservas.reservar(
            sessaoId,
            livroId,
            quantidade,
            itens.get(livroId).getQuantidade(),
            false
        );

        itens.put(
            livroId,
            new ItemCarrinho(livro, quantidade)
        );
    }

    public synchronized void remover(Long livroId) {
        sincronizar();

        if (!itens.containsKey(livroId)) {
            throw erro("O livro não está no carrinho.");
        }

        reservas.remover(sessaoId, livroId);
        itens.remove(livroId);
    }

    public synchronized List<ItemCarrinho> listar() {
        sincronizar();

        List<ItemCarrinho> resultado = new ArrayList<>();

        for (ItemCarrinho item : itens.values()) {
            Livro atual = livroRepository.buscarPorId(
                item.getLivro().getId(),
                sessaoId
            );

            if (atual == null) {
                atual = indisponivel(item.getLivro());
            }

            resultado.add(
                new ItemCarrinho(
                    atual,
                    item.getQuantidade()
                )
            );
        }

        return List.copyOf(resultado);
    }

    public synchronized List<String> revalidarEstoque() {
        exigirPrazoValido();

        Map<Long, ItemCarrinho> revisados = new LinkedHashMap<>();
        List<String> mensagens = new ArrayList<>();

        for (var entrada : itens.entrySet()) {
            Long livroId = entrada.getKey();
            ItemCarrinho anterior = entrada.getValue();

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

        if (!mensagens.isEmpty()) {
            Map<Long, Integer> quantidades = revisados
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                entrada -> entrada.getKey(),
                entrada -> entrada.getValue().getQuantidade()
            ));

            notificacoesEstoque.registrar(
                List.copyOf(mensagens),
                sessaoId,
                quantidades
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

    // Após a compra, a reserva já pertence ao pedido.
    // Limpa somente o estado do carrinho na sessão.
    public synchronized void limpar() {
        itens.clear();
        retirados.clear();
        expiracaoPendente = false;
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
            throw erro("A quantidade deve ser maior que zero.");
        }
    }

    private void validarEstoque(Livro livro, long quantidade) {
        if (quantidade > livro.getEstoque()) {
            throw erro(
                "Quantidade solicitada acima do estoque disponível. "
                + "Disponível: " + livro.getEstoque()
                + " unidade(s)."
            );
        }
    }

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
}