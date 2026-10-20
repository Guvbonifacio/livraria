package com.fatec.livraria.service;

import com.fatec.livraria.repository.PedidoRepository;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaPedidoService {

    private final PedidoRepository pedidos;
    private final AuditoriaService auditoria;

    public EntregaPedidoService(
            PedidoRepository pedidos,
            AuditoriaService auditoria) {

        this.pedidos = pedidos;
        this.auditoria = auditoria;
    }

    @Transactional
    public String transicionar(Long id, String destino) {
        String origem;

        if ("EM TRANSPORTE".equals(destino)) {
            origem = "APROVADA";

        } else if ("ENTREGUE".equals(destino)) {
            origem = "EM TRANSPORTE";

        } else {
            throw new ValidacaoException(
                List.of("Transição de entrega inválida.")
            );
        }

        if (id == null
                || pedidos.transicionarStatus(id, origem, destino) != 1) {

            throw new ValidacaoException(
                List.of(
                    "Pedido não encontrado ou fora do status exigido: "
                    + origem + "."
                )
            );
        }

        auditoria.registrar(
            "UPDATE",
            "pedido",
            id,
            Map.of(
                "statusAnterior", origem,
                "status", destino
            )
        );

        return destino;
    }
}