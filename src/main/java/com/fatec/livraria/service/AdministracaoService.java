package com.fatec.livraria.service;

import com.fatec.livraria.model.DadosAdministrativos.*;
import com.fatec.livraria.repository.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdministracaoService {

    private final PedidoRepository pedidos;
    private final TrocaRepository trocas;
    private final LivroRepository livros;
    private final UsuarioAtual usuario;
    private final ClienteService clientes;
    private final LogRepository logs;

    private static final List<String> STATUS = List.of(
        "EM PROCESSAMENTO",
        "APROVADA",
        "REPROVADA",
        "EM TRANSPORTE",
        "ENTREGUE",
        "EM TROCA",
        "TROCA AUTORIZADA",
        "TROCADO"
    );

    public AdministracaoService(
            PedidoRepository pedidos,
            TrocaRepository trocas,
            LivroRepository livros,
            UsuarioAtual usuario,
            ClienteService clientes,
            LogRepository logs) {

        this.pedidos = pedidos;
        this.trocas = trocas;
        this.livros = livros;
        this.usuario = usuario;
        this.clientes = clientes;
        this.logs = logs;
    }

    public List<Pedido> listarPedidos(
            String status,
            String dataInicial,
            String dataFinal) {

        usuario.exigirAdministrador();

        if (status != null
                && !status.isBlank()
                && !STATUS.contains(status)) {
            throw erro("Status de pedido inválido.");
        }

        LocalDate inicio = data(dataInicial);
        LocalDate fim = data(dataFinal);

        if (inicio != null
                && fim != null
                && inicio.isAfter(fim)) {
            throw erro(
                "A data inicial não pode ser posterior à data final."
            );
        }

        if (fim != null && fim.equals(LocalDate.MAX)) {
            throw erro("Data final inválida.");
        }

        return pedidos.listarAdministrativo(
            status,
            inicio,
            fim
        );
    }

    public Pedido buscarPedido(Long id) {
        usuario.exigirAdministrador();
        return pedidos.buscarAdministrativo(id);
    }

    public List<Troca> listarTrocas() {
        usuario.exigirAdministrador();
        return trocas.listar();
    }

    public Troca buscarTroca(Long id) {
        usuario.exigirAdministrador();
        return trocas.buscarPorId(id);
    }

    public List<String> listarCategorias() {
        usuario.exigirAdministrador();
        return livros.listarCategorias();
    }

    private LocalDate data(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw erro("Informe uma data válida.");
        }
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }

    public void exigirAcesso() {
        usuario.exigirAdministrador();
    }

    public com.fatec.livraria.model.Cliente buscarCliente(Long id) {
        usuario.exigirAdministrador();
        return clientes.exigirCliente(id);
    }

    public List<com.fatec.livraria.model.LogTransacao> historicoCliente(Long id) {
        usuario.exigirAdministrador();
        return logs.buscarPorRegistro("cliente", id);
    }
}