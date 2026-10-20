package com.fatec.livraria.service;

import com.fatec.livraria.repository.PedidoRepository;
import com.fatec.livraria.repository.PedidoRepository.PagamentoPendente;
import com.fatec.livraria.repository.PedidoRepository.PedidoPendente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ValidacaoPagamentoPedidoService {

    private final PedidoRepository pedidos;
    private final OperadoraSimulada operadora;

    public ValidacaoPagamentoPedidoService(
            PedidoRepository pedidos,
            OperadoraSimulada operadora) {

        this.pedidos = pedidos;
        this.operadora = operadora;
    }

    public boolean validar(
            PedidoPendente pedido,
            List<PagamentoPendente> pagamentos,
            boolean aceiteSimulado) {

        if (pagamentos.isEmpty()) {
            return false;
        }

        BigDecimal recebido = BigDecimal.ZERO;

        for (var pagamento : pagamentos) {
            if (!"PENDENTE".equals(pagamento.status())) {
                return false;
            }

            recebido = recebido.add(pagamento.valor());

            if ("CARTAO".equals(pagamento.forma())) {
                boolean autorizado = operadora.autorizar(
                    pagamento.id(),
                    aceiteSimulado
                );

                if (!autorizado) {
                    return false;
                }
            } else {
                var cupom = pedidos.buscarCupom(
                    pagamento.cupomId()
                );

                if (cupom == null) {
                    return false;
                }

                if (cupom.validade().isBefore(LocalDate.now())) {
                    return false;
                }

                if (!pagamento.forma().equals(
                        "CUPOM_" + cupom.tipo())) {
                    return false;
                }

                if (pagamento.valor()
                        .compareTo(cupom.valor()) != 0) {
                    return false;
                }

                if (cupom.clienteId() != null
                        && !cupom.clienteId().equals(
                            pedido.clienteId())) {
                    return false;
                }

                if (!pedidos.origemCupomAprovada(cupom.id())) {
                    return false;
                }

                if ("PROMOCIONAL".equals(cupom.tipo())) {
                    if (!cupom.ativo()) {
                        return false;
                    }
                } else {
                    boolean comprometidoEmOutroPedido =
                        pedidos.trocaComprometidaEmOutroPedido(
                            cupom.id(),
                            pedido.id()
                        );

                    if (comprometidoEmOutroPedido) {
                        return false;
                    }

                    // O cupom de troca foi inativado na finalização
                    // para impedir sua reutilização.
                    // O pagamento PENDENTE deste pedido identifica
                    // o comprometimento do cupom nesta compra.
                }
            }
        }

        BigDecimal excedente = pedidos.totalExcedente(
            pedido.id()
        );

        return recebido
            .subtract(excedente)
            .compareTo(pedido.total()) == 0;
    }
}