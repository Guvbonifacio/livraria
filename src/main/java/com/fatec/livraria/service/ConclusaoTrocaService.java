package com.fatec.livraria.service;

import com.fatec.livraria.model.DadosAdministrativos.Troca;
import com.fatec.livraria.repository.PedidoRepository;
import com.fatec.livraria.repository.TrocaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConclusaoTrocaService {

    private final TrocaRepository trocas;
    private final PedidoRepository pedidos;
    private final AuditoriaService auditoria;
    private final UsuarioAtual usuario;

    public ConclusaoTrocaService(
            TrocaRepository trocas,
            PedidoRepository pedidos,
            AuditoriaService auditoria, UsuarioAtual usuario) {

        this.trocas = trocas;
        this.pedidos = pedidos;
        this.auditoria = auditoria;
        this.usuario = usuario;
    }

    @Transactional
    public void receber(Long id, boolean retornaEstoque) {
        usuario.exigirAdministrador();
        Troca troca = exigir(id);

        if (!"TROCA AUTORIZADA".equals(troca.status())) {
            throw erro(
                "Somente uma troca autorizada pode ser recebida."
            );
        }

        if (troca.itens().isEmpty()) {
            throw erro("A troca não possui itens.");
        }

        LocalDateTime agora = LocalDateTime.now();

        if (trocas.receber(id, retornaEstoque, agora) != 1) {
            throw erro(
                "A troca já foi recebida. Atualize a página."
            );
        }

        auditoria.registrar(
            "UPDATE",
            "troca",
            id,
            Map.of(
                "statusAnterior", "TROCA AUTORIZADA",
                "status", "TROCADO",
                "retornaEstoque", retornaEstoque
            )
        );

        if (retornaEstoque) {
            for (var item : troca.itens()) {

                // Decisão de implementação:
                // o pedido não identifica o lote originalmente vendido.
                // Usa o custo da entrada mais recente do livro,
                // inclusive quando esse lote estiver zerado.
                // O preço de venda não é usado como custo.
                Long custo = trocas.buscarCustoEntrada(
                    item.livro().id()
                );

                if (custo == null || custo <= 0) {
                    throw erro(
                        "Não há custo de entrada para o livro devolvido."
                    );
                }

                Long entradaId = trocas.registrarReentrada(
                    id,
                    item.id(),
                    item.livro().id(),
                    item.quantidade(),
                    custo,
                    agora
                );

                auditoria.registrar(
                    "INSERT",
                    "item_estoque",
                    entradaId,
                    Map.of(
                        "trocaId", id,
                        "livroId", item.livro().id(),
                        "quantidade", item.quantidade(),
                        "valorCustoCentavos", custo
                    )
                );
            }
        }

        // Na troca integral, a autorização também alterou a compra.
        // Na parcial, o pedido permanece ENTREGUE.
        if ("TROCA AUTORIZADA".equals(troca.pedido().status())) {

            int alterados = pedidos.transicionarStatus(
                troca.pedido().id(),
                "TROCA AUTORIZADA",
                "TROCADO"
            );

            if (alterados != 1) {
                throw erro(
                    "O pedido mudou. Atualize a página."
                );
            }

            auditoria.registrar(
                "UPDATE",
                "pedido",
                troca.pedido().id(),
                Map.of(
                    "statusAnterior", "TROCA AUTORIZADA",
                    "status", "TROCADO"
                )
            );
        }

        // RF0045: o cupom é emitido na mesma confirmação.
        // Esta chamada participa da transação de receber().
        finalizar(id);
    }

    @Transactional
    public void finalizar(Long id) {
        usuario.exigirAdministrador();
        Troca troca = exigir(id);

        if (!"TROCADO".equals(troca.status())
                || troca.dataRecebimento() == null) {

            throw erro(
                "Confirme o recebimento antes de emitir o cupom."
            );
        }

        if (troca.cupomId() != null) {
            throw erro(
                "O cupom desta troca já foi emitido."
            );
        }

        // Decisão declarada:
        // preços históricos dos itens × quantidades devolvidas,
        // sem incluir frete.
        BigDecimal valor = troca.itens().stream()
            .map(item -> item.getSubtotal())
            .reduce(
                BigDecimal.ZERO,
                (acumulado, subtotal) -> acumulado.add(subtotal)
            );

        if (valor.signum() <= 0) {
            throw erro("O valor da troca deve ser positivo.");
        }

        LocalDate validade = LocalDate.now().plusDays(30);

        String codigo =
            "TROCA-" + id + "-" + UUID.randomUUID();

        // Reutiliza o INSERT já existente para cupom de troca.
        Long cupomId = pedidos.salvarExcedente(
            codigo,
            troca.pedido().cliente().id(),
            troca.pedido().id(),
            valor,
            validade
        );

        auditoria.registrar(
            "INSERT",
            "cupom",
            cupomId,
            Map.of(
                "trocaId", id,
                "tipo", "TROCA",
                "codigo", codigo,
                "clienteId", troca.pedido().cliente().id(),
                "valor", valor,
                "validade", validade.toString()
            )
        );

        if (trocas.vincularCupom(id, cupomId) != 1) {
            throw erro(
                "O cupom já foi emitido. Atualize a página."
            );
        }

        auditoria.registrar(
            "UPDATE",
            "troca",
            id,
            Map.of("cupomId", cupomId)
        );
    }

    private Troca exigir(Long id) {
        Troca troca = trocas.buscarPorId(id);

        if (troca == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Troca não encontrada."
            );
        }

        return troca;
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(
            List.of(mensagem)
        );
    }
}