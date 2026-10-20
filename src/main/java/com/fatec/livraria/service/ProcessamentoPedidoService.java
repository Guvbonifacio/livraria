package com.fatec.livraria.service;

import com.fatec.livraria.repository.PedidoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessamentoPedidoService {

    private final PedidoRepository pedidos;
    private final ValidacaoPagamentoPedidoService validacao;
    private final EstoquePedidoService estoque;
    private final AuditoriaService auditoria;
    private final int validadeDias;

    public ProcessamentoPedidoService(
            PedidoRepository pedidos,
            ValidacaoPagamentoPedidoService validacao,
            EstoquePedidoService estoque,
            AuditoriaService auditoria,
            @Value("${livraria.cupom-troca.validade-dias:0}")
            int validadeDias) {

        this.pedidos = pedidos;
        this.validacao = validacao;
        this.estoque = estoque;
        this.auditoria = auditoria;
        this.validadeDias = validadeDias;
    }

    @Transactional
    public String processar(
            Long pedidoId,
            String retornoSimulado) {

        // Representa a resposta simulada da operadora.
        // A aprovação ainda depende da validação dos pagamentos.
        if (!List.of("APROVADA", "REPROVADA")
                .contains(retornoSimulado)) {

            throw erro(
                "Nesta etapa, simule apenas aprovação "
                + "ou recusa do pagamento."
            );
        }

        var pedido = pedidos.buscarParaProcessar(pedidoId);

        if (pedido == null) {
            throw erro("Pedido não encontrado.");
        }

        if (!"EM PROCESSAMENTO".equals(pedido.status())) {
            throw erro("Este pedido já foi processado.");
        }

        var pagamentos = pedidos.listarPagamentosParaProcessar(
            pedidoId
        );

        if (pagamentos.stream().anyMatch(
                pagamento ->
                    !"PENDENTE".equals(pagamento.status()))) {

            throw erro(
                "Há pagamento já processado neste pedido."
            );
        }

        var detalhe = pedidos.buscarAdministrativo(pedidoId);

        if (detalhe.itens().isEmpty()) {
            throw erro("Pedido sem itens.");
        }

        boolean aprovada = validacao.validar(
            pedido,
            pagamentos,
            "APROVADA".equals(retornoSimulado)
        );

        var excedentes = pedidos.listarExcedentes(pedidoId);

        if (aprovada
                && !excedentes.isEmpty()
                && validadeDias <= 0) {

            throw erro(
                "Configure a validade do cupom de troca "
                + "antes de liberar o excedente."
            );
        }

        // RF0053 / RN0028: somente a aprovação realiza a baixa.
        if (aprovada) {
            estoque.baixar(
                pedidoId,
                detalhe.itens()
            );
        }

        for (var pagamento : pagamentos) {
            String statusPagamento = aprovada
                ? "APROVADO"
                : "RECUSADO";

            int alterados = pedidos.atualizarStatusPagamento(
                pagamento.id(),
                statusPagamento
            );

            if (alterados != 1) {
                throw erro(
                    "O pagamento mudou durante o processamento."
                );
            }

            auditoria.registrar(
                "UPDATE",
                "pagamento",
                pagamento.id(),
                Map.of(
                    "pedidoId", pedidoId,
                    "statusAnterior", "PENDENTE",
                    "status", statusPagamento
                )
            );

            if (!aprovada
                    && "CUPOM_TROCA".equals(pagamento.forma())) {

                int restaurados = pedidos.restaurarTroca(
                    pagamento.cupomId(),
                    pedido.clienteId(),
                    pagamento.valor(),
                    pedidoId
                );

                if (restaurados == 1) {
                    auditoria.registrar(
                        "UPDATE",
                        "cupom",
                        pagamento.cupomId(),
                        Map.of(
                            "pedidoId", pedidoId,
                            "ativo", true,
                            "motivo", "pagamento reprovado"
                        )
                    );
                }
            }
        }

        for (Long cupomId : excedentes) {
            if (aprovada) {
                LocalDate validade = LocalDate.now()
                    .plusDays(validadeDias);

                int ativados = pedidos.ativarExcedente(
                    cupomId,
                    validade
                );

                if (ativados == 1) {
                    auditoria.registrar(
                        "UPDATE",
                        "cupom",
                        cupomId,
                        Map.of(
                            "pedidoId", pedidoId,
                            "ativo", true,
                            "validade", validade.toString()
                        )
                    );
                }
            } else {
                int desativados = pedidos.desativarExcedente(
                    cupomId
                );

                if (desativados == 1) {
                    auditoria.registrar(
                        "UPDATE",
                        "cupom",
                        cupomId,
                        Map.of(
                            "pedidoId", pedidoId,
                            "ativo", false
                        )
                    );
                }
            }
        }

        estoque.liberarReservas(pedidoId);

        String statusPedido = aprovada
            ? "APROVADA"
            : "REPROVADA";

        int alterados = pedidos.atualizarStatusPedido(
            pedidoId,
            statusPedido
        );

        if (alterados != 1) {
            throw erro(
                "O pedido mudou durante o processamento."
            );
        }

        auditoria.registrar(
            "UPDATE",
            "pedido",
            pedidoId,
            Map.of(
                "statusAnterior", "EM PROCESSAMENTO",
                "status", statusPedido
            )
        );

        return statusPedido;
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(
            List.of(mensagem)
        );
    }
}