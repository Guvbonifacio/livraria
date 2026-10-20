package com.fatec.livraria.service;

import com.fatec.livraria.repository.NotificacaoRepository;
import com.fatec.livraria.repository.PedidoRepository;
import com.fatec.livraria.repository.TrocaRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutorizacaoTrocaService {

    private final TrocaRepository trocas;
    private final PedidoRepository pedidos;
    private final NotificacaoRepository notificacoes;
    private final AuditoriaService auditoria;

    public AutorizacaoTrocaService(
            TrocaRepository trocas,
            PedidoRepository pedidos,
            NotificacaoRepository notificacoes,
            AuditoriaService auditoria) {

        this.trocas = trocas;
        this.pedidos = pedidos;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
    }

    @Transactional
    public void autorizar(
            Long trocaId,
            String decisao) {

        if (!"ACEITAR".equals(decisao)) {
            throw erro(
                "A decisão disponível nesta etapa "
                + "é autorizar a troca."
            );
        }

        var troca = trocas.buscarPorId(trocaId);

        if (troca == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Troca não encontrada."
            );
        }

        if (!"EM TROCA".equals(troca.status())) {
            throw erro(
                "Somente solicitações EM TROCA "
                + "podem ser autorizadas."
            );
        }

        var pedido = troca.pedido();
        var itens = trocas.listarItensDaTroca(trocaId);

        if (itens.isEmpty()) {
            throw erro(
                "A solicitação não possui itens para troca."
            );
        }

        var comprados = new HashMap<Long, Integer>();

        pedido.itens().forEach(
            item -> comprados.put(
                item.id(),
                item.quantidade()
            )
        );

        var quantidades = new HashMap<Long, Integer>();

        for (var item : itens) {
            Integer comprada = comprados.get(
                item.itemPedidoId()
            );

            if (comprada == null
                    || item.quantidade() <= 0
                    || item.quantidade() > comprada) {

                throw erro(
                    "A solicitação contém item ou quantidade "
                    + "incompatível com a compra."
                );
            }

            quantidades.put(
                item.itemPedidoId(),
                item.quantidade()
            );
        }

        // Compara os itens originais da compra e suas quantidades.
        boolean integral = pedido.itens().stream()
            .allMatch(item ->
                Objects.equals(
                    quantidades.get(item.id()),
                    item.quantidade()
                )
            );

        String esperado = integral
            ? "EM TROCA"
            : "ENTREGUE";

        if (!esperado.equals(pedido.status())) {
            throw erro(
                "O pedido não está no status esperado "
                + "para esta autorização: "
                + esperado + "."
            );
        }

        LocalDateTime agora = LocalDateTime.now();

        if (trocas.autorizar(trocaId, agora) != 1) {
            throw erro(
                "A solicitação já foi processada. "
                + "Atualize a página."
            );
        }

        auditoria.registrar(
            "UPDATE",
            "troca",
            trocaId,
            Map.of(
                "statusAnterior", "EM TROCA",
                "status", "TROCA AUTORIZADA"
            )
        );

        if (integral) {
            int alterados = pedidos.transicionarStatus(
                pedido.id(),
                "EM TROCA",
                "TROCA AUTORIZADA"
            );

            if (alterados != 1) {
                throw erro(
                    "O status do pedido mudou. "
                    + "Atualize a página."
                );
            }

            auditoria.registrar(
                "UPDATE",
                "pedido",
                pedido.id(),
                Map.of(
                    "statusAnterior", "EM TROCA",
                    "status", "TROCA AUTORIZADA"
                )
            );
        }

        String mensagem =
            "A troca #" + trocaId
            + " do pedido #" + pedido.id()
            + " foi autorizada.";

        Long notificacaoId = notificacoes.salvar(
            pedido.cliente().id(),
            mensagem,
            agora
        );

        auditoria.registrar(
            "INSERT",
            "notificacao",
            notificacaoId,
            Map.of(
                "clienteId", pedido.cliente().id(),
                "pedidoId", pedido.id(),
                "trocaId", trocaId,
                "mensagem", mensagem
            )
        );
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(
            List.of(mensagem)
        );
    }
}