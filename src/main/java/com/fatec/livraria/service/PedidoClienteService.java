package com.fatec.livraria.service;

import com.fatec.livraria.model.DadosAdministrativos.Pedido;
import com.fatec.livraria.model.HistoricoStatus;
import com.fatec.livraria.repository.LogRepository;
import com.fatec.livraria.repository.PedidoRepository;
import com.fatec.livraria.model.Notificacao;
import com.fatec.livraria.repository.NotificacaoRepository;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class PedidoClienteService {

    private final PedidoRepository pedidos;
    private final UsuarioAtual usuario;
    private final LogRepository logs;
    private final ObjectMapper json;
    private final NotificacaoRepository notificacoes;

    public PedidoClienteService(
        PedidoRepository pedidos,
        UsuarioAtual usuario,
        LogRepository logs,
        ObjectMapper json,
        NotificacaoRepository notificacoes) {

    this.pedidos = pedidos;
    this.usuario = usuario;
    this.logs = logs;
    this.json = json;
    this.notificacoes = notificacoes;
}

    public List<Pedido> listar() {
        return pedidos.listarDoCliente(
            usuario.getClienteId()
        );
    }

    public Pedido exigir(Long id) {
        Pedido pedido = pedidos.buscarDoCliente(
            id,
            usuario.getClienteId()
        );

        if (pedido == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pedido não encontrado."
            );
        }

        return pedido;
    }

    public Pedido exigirEntregue(Long id) {
        Pedido pedido = exigir(id);

        if (!"ENTREGUE".equals(pedido.status())) {
            throw new ValidacaoException(
                List.of(
                    "A troca só pode ser solicitada "
                    + "para pedido ENTREGUE."
                )
            );
        }

        return pedido;
    }

    public List<HistoricoStatus> historico(Long id) {
        exigir(id);

        // Usa somente os eventos realmente registrados na auditoria.
        return logs.buscarPorRegistro("pedido", id)
            .stream()
            .filter(log ->
                "INSERT".equals(log.getOperacao())
                || "UPDATE".equals(log.getOperacao())
            )
            .map(log -> {
                var status = json.readTree(log.getDados())
                    .path("status");

                return status.isString()
                    ? new HistoricoStatus(
                        log.getDataHora(),
                        status.asString()
                    )
                    : null;
            })
            .filter(Objects::nonNull)
            .toList();
    }

    public List<Notificacao> notificacoes() {
    return notificacoes.listarDoCliente(
        usuario.getClienteId()
    );
}
}