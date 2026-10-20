package com.fatec.livraria.service;

import com.fatec.livraria.model.DadosAdministrativos.Item;
import com.fatec.livraria.repository.EstoqueRepository;
import com.fatec.livraria.repository.LivroRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class EstoquePedidoService {

    private final EstoqueRepository estoque;
    private final LivroRepository livros;
    private final AuditoriaService auditoria;

    public EstoquePedidoService(
            EstoqueRepository estoque,
            LivroRepository livros,
            AuditoriaService auditoria) {

        this.estoque = estoque;
        this.livros = livros;
        this.auditoria = auditoria;
    }

    private String chave(Long pedidoId) {
        return "pedido:" + pedidoId;
    }

    // Vincula somente reservas já existentes.
    // Não cria reservas nem modifica o prazo de expiração.
    public void vincularReservas(
            String sessaoId,
            Long pedidoId,
            Set<Long> livrosDoPedido) {

        if (sessaoId == null) {
            return;
        }

        for (var reserva : estoque.listarReservas(sessaoId)) {
            if (!livrosDoPedido.contains(reserva.livroId())) {
                continue;
            }

            int alterados = estoque.vincularReserva(
                reserva.id(),
                sessaoId,
                chave(pedidoId)
            );

            if (alterados != 1) {
                throw erro(
                    "A reserva mudou durante a finalização. "
                    + "Revise o carrinho."
                );
            }

            auditoria.registrar(
                "UPDATE",
                "reserva_estoque",
                reserva.id(),
                Map.of(
                    "pedidoId", pedidoId,
                    "livroId", reserva.livroId()
                )
            );
        }
    }

    public void baixar(
            Long pedidoId,
            List<Item> itens) {

        // Confere todos os itens antes de alterar o primeiro lote.
        for (var item : itens) {
            long disponivel = livros.buscarDisponibilidade(
                item.livro().id(),
                chave(pedidoId)
            );

            if (item.quantidade() > disponivel) {
                throw erro(
                    "Estoque insuficiente para aprovar. "
                    + "O pedido permanece EM PROCESSAMENTO."
                );
            }
        }

        for (var item : itens) {
            int restante = item.quantidade();

            // Consome os lotes por data de entrada e ID.
            for (var lote :
                    estoque.listarLotes(item.livro().id())) {

                int quantidade = Math.min(
                    restante,
                    lote.quantidade()
                );

                if (quantidade == 0) {
                    break;
                }

                int alterados = estoque.baixar(
                    lote.id(),
                    quantidade
                );

                if (alterados != 1) {
                    throw erro(
                        "O estoque mudou durante a baixa. "
                        + "Nenhuma alteração foi confirmada."
                    );
                }

                auditoria.registrar(
                    "UPDATE",
                    "item_estoque",
                    lote.id(),
                    Map.of(
                        "pedidoId", pedidoId,
                        "livroId", item.livro().id(),
                        "quantidadeBaixada", quantidade,
                        "saldoAnterior", lote.quantidade(),
                        "saldoAtual",
                            lote.quantidade() - quantidade
                    )
                );

                restante -= quantidade;
            }

            if (restante != 0) {
                throw erro(
                    "Estoque insuficiente. "
                    + "Nenhuma alteração foi confirmada."
                );
            }
        }
    }

    public void liberarReservas(Long pedidoId) {
        String identificador = chave(pedidoId);

        for (var reserva :
                estoque.listarReservas(identificador)) {

            int excluidos = estoque.excluirReserva(
                reserva.id(),
                identificador
            );

            if (excluidos != 1) {
                throw erro(
                    "A reserva mudou durante o processamento."
                );
            }

            auditoria.registrar(
                "DELETE",
                "reserva_estoque",
                reserva.id(),
                Map.of(
                    "pedidoId", pedidoId,
                    "livroId", reserva.livroId(),
                    "quantidade", reserva.quantidade()
                )
            );
        }
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(
            List.of(mensagem)
        );
    }
}