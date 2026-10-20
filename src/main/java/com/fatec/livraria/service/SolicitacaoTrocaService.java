package com.fatec.livraria.service;

import com.fatec.livraria.model.DadosAdministrativos.Item;
import com.fatec.livraria.model.DadosAdministrativos.Troca;
import com.fatec.livraria.model.FormularioTroca;
import com.fatec.livraria.repository.PedidoRepository;
import com.fatec.livraria.repository.TrocaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitacaoTrocaService {

    private final PedidoClienteService consulta;
    private final PedidoRepository pedidos;
    private final TrocaRepository trocas;
    private final AuditoriaService auditoria;

    public SolicitacaoTrocaService(
            PedidoClienteService consulta,
            PedidoRepository pedidos,
            TrocaRepository trocas,
            AuditoriaService auditoria) {

        this.consulta = consulta;
        this.pedidos = pedidos;
        this.trocas = trocas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Troca> listar(Long pedidoId) {
        consulta.exigir(pedidoId);

        return trocas.listarDoPedido(pedidoId);
    }

    @Transactional(readOnly = true)
    public List<Long> itensSolicitados(Long pedidoId) {
        consulta.exigir(pedidoId);

        return trocas.listarItensSolicitados(pedidoId);
    }

    @Transactional
    public Long solicitar(
            Long pedidoId,
            FormularioTroca formulario) {

        // Confere propriedade do pedido e status ENTREGUE.
        var pedido = consulta.exigirEntregue(pedidoId);

        if (formulario == null
                || formulario.getItensIds() == null
                || formulario.getItensIds().isEmpty()) {

            throw erro("Selecione ao menos um item para troca.");
        }

        var erros = new ArrayList<String>();

        var itensComprados = new HashMap<Long, Item>();

        pedido.itens().forEach(
            item -> itensComprados.put(item.id(), item)
        );

        var anteriores = new HashSet<>(
            trocas.listarItensSolicitados(pedidoId)
        );

        var selecionados = new HashSet<Long>();
        var quantidades = new LinkedHashMap<Long, Integer>();

        for (Long itemId : formulario.getItensIds()) {
            Item item = itensComprados.get(itemId);

            if (item == null) {
                erros.add(
                    "O item selecionado não pertence a este pedido."
                );
                continue;
            }

            if (!selecionados.add(itemId)) {
                erros.add(
                    "O mesmo item foi selecionado mais de uma vez."
                );
                continue;
            }

            if (anteriores.contains(itemId)) {
                erros.add(
                    "O item já possui uma solicitação de troca: "
                    + item.livro().titulo() + "."
                );
                continue;
            }

            try {
                String valor = formulario.getQuantidades() == null
                    ? null
                    : formulario.getQuantidades().get(itemId);

                int quantidade = Integer.parseInt(
                    valor == null ? "" : valor.trim()
                );

                if (quantidade <= 0
                        || quantidade > item.quantidade()) {

                    erros.add(
                        "A quantidade para troca de "
                        + item.livro().titulo()
                        + " deve estar entre 1 e "
                        + item.quantidade() + "."
                    );

                } else {
                    quantidades.put(itemId, quantidade);
                }

            } catch (NumberFormatException e) {
                erros.add(
                    "Informe uma quantidade inteira para "
                    + item.livro().titulo() + "."
                );
            }
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        // RF0041 não exige motivo.
        // Texto vazio mantém compatibilidade com bancos existentes
        // em que a coluna motivo ainda está como NOT NULL.
        String motivo = texto(formulario.getMotivo());
        String observacao = texto(formulario.getObservacao());

        if (!observacao.isEmpty()) {
            motivo = motivo.isEmpty()
                ? observacao
                : motivo + " — " + observacao;
        }

        Long trocaId = trocas.salvar(pedidoId, motivo);

        auditoria.registrar(
            "INSERT",
            "troca",
            trocaId,
            Map.of(
                "pedidoId", pedidoId,
                "status", "EM TROCA"
            )
        );

        for (var entrada : quantidades.entrySet()) {
            Long itemTrocaId = trocas.salvarItem(
                trocaId,
                entrada.getKey(),
                entrada.getValue()
            );

            auditoria.registrar(
                "INSERT",
                "item_troca",
                itemTrocaId,
                Map.of(
                    "trocaId", trocaId,
                    "itemPedidoId", entrada.getKey(),
                    "quantidade", entrada.getValue()
                )
            );
        }

        // Uma solicitação integral contém todos os itens
        // e todas as unidades da compra na mesma solicitação.
        boolean integral = pedido.itens().stream()
            .allMatch(item ->
                Objects.equals(
                    quantidades.get(item.id()),
                    item.quantidade()
                )
            );

        if (integral) {
            int alterados = pedidos.transicionarStatus(
                pedidoId,
                "ENTREGUE",
                "EM TROCA"
            );

            if (alterados != 1) {
                throw erro(
                    "O status do pedido mudou. "
                    + "Consulte o pedido novamente."
                );
            }

            auditoria.registrar(
                "UPDATE",
                "pedido",
                pedidoId,
                Map.of(
                    "statusAnterior", "ENTREGUE",
                    "status", "EM TROCA"
                )
            );
        }

        return trocaId;
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(
            List.of(mensagem)
        );
    }
}