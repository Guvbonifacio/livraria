package com.fatec.livraria.service;

import com.fatec.livraria.model.*;
import com.fatec.livraria.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final LivroRepository livros;
    private final ClienteService clientes;
    private final PagamentoService pagamentos;
    private final FreteService frete;
    private final LogRepository logs;
    private final UsuarioAtual usuario;
    private final ObjectMapper json;
    private final int validadeDias;

    public PedidoService(
            PedidoRepository pedidos,
            LivroRepository livros,
            ClienteService clientes,
            PagamentoService pagamentos,
            FreteService frete,
            LogRepository logs,
            UsuarioAtual usuario,
            ObjectMapper json,
            @Value("${livraria.cupom-troca.validade-dias:0}")
            int validadeDias) {

        this.pedidos = pedidos;
        this.livros = livros;
        this.clientes = clientes;
        this.pagamentos = pagamentos;
        this.frete = frete;
        this.logs = logs;
        this.usuario = usuario;
        this.json = json;
        this.validadeDias = validadeDias;
    }

    @Transactional
    public Long finalizar(
            List<ItemCarrinho> solicitados,
            Endereco endereco,
            FormularioPagamento formulario) {

        Long clienteId = usuario.getClienteId();

        if (!clientes.exigirCliente(clienteId).isAtivo()) {
            throw erro("Cliente inativo.");
        }

        clientes.validarEndereco(endereco);

        if (solicitados == null || solicitados.isEmpty()) {
            throw erro("Seu carrinho está vazio.");
        }

        var itens = new ArrayList<ItemCarrinho>();
        var ids = new HashSet<Long>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (var item : solicitados) {
            if (item == null
                    || item.getLivro() == null
                    || item.getQuantidade() <= 0
                    || !ids.add(item.getLivro().getId())) {
                throw erro("Item do carrinho inválido.");
            }

            var livro = livros.buscarPorId(
                item.getLivro().getId()
            );

            if (livro == null
                    || item.getQuantidade() > livro.getEstoque()) {
                throw erro(
                    "Livro indisponível ou quantidade "
                    + "acima do estoque."
                );
            }

            var atual = new ItemCarrinho(
                livro,
                item.getQuantidade()
            );

            itens.add(atual);
            subtotal = subtotal.add(atual.getSubtotalExato());
        }

        BigDecimal valorFrete = frete.calcular(itens, endereco);
        BigDecimal total = subtotal.add(valorFrete);

        var plano = pagamentos.preparar(formulario, total);

        if (plano.excedente().signum() > 0
                && validadeDias <= 0) {
            throw erro(
                "Configure a validade do cupom de troca "
                + "antes de emitir o excedente."
            );
        }

        Long pedidoId = pedidos.salvarPedido(
            clienteId,
            json.writeValueAsString(endereco),
            centavos(subtotal),
            centavos(valorFrete)
        );

        log("INSERT", "pedido", pedidoId, Map.of(
            "status", "EM PROCESSAMENTO",
            "totalCentavos", centavos(total)
        ));

        for (var item : itens) {
            Long itemId = pedidos.salvarItem(
                pedidoId,
                item.getLivro().getId(),
                item.getLivro().getTitulo(),
                item.getQuantidade(),
                centavos(BigDecimal.valueOf(
                    item.getLivro().getPreco()
                ))
            );

            log("INSERT", "item_pedido", itemId, Map.of(
                "pedidoId", pedidoId,
                "livroId", item.getLivro().getId(),
                "quantidade", item.getQuantidade()
            ));
        }

        for (var parcela : plano.parcelas()) {
            if ("CUPOM_TROCA".equals(parcela.forma())) {
                int alterados = pedidos.consumirTroca(
                    parcela.cupomId(),
                    clienteId,
                    LocalDate.now()
                );

                if (alterados != 1) {
                    throw erro(
                        "Cupom de troca já utilizado "
                        + "ou indisponível."
                    );
                }

                log("UPDATE", "cupom", parcela.cupomId(), Map.of(
                    "ativo", false,
                    "pedidoId", pedidoId
                ));
            }

            // Promoções respeitam ativo e validade.
            // Não há definição de uso único global para promoções.
            Long pagamentoId = pedidos.salvarPagamento(
                pedidoId,
                parcela
            );

            log("INSERT", "pagamento", pagamentoId, Map.of(
                "pedidoId", pedidoId,
                "forma", parcela.forma(),
                "valorCentavos", centavos(parcela.valor()),
                "status", "PENDENTE"
            ));
        }

        if (plano.excedente().signum() > 0) {
            Long cupomId = pedidos.salvarExcedente(
                "TROCA-" + UUID.randomUUID(),
                clienteId,
                pedidoId,
                plano.excedente(),
                LocalDate.now().plusDays(validadeDias)
            );

            log("INSERT", "cupom", cupomId, Map.of(
                "pedidoId", pedidoId,
                "tipo", "TROCA",
                "valorCentavos", centavos(plano.excedente())
            ));
        }

        return pedidoId;
    }

    public PedidoRepository.Confirmacao buscarConfirmacao(Long id) {
        return pedidos.buscarConfirmacao(
            id,
            usuario.getClienteId()
        );
    }

    private void log(
            String operacao,
            String tabela,
            Long id,
            Map<String, Object> dados) {

        logs.registrar(new LogTransacao(
            null,
            LocalDateTime.now(),
            usuario.getIdentificador(),
            operacao,
            tabela,
            id,
            json.writeValueAsString(dados)
        ));
    }

    private long centavos(BigDecimal valor) {
        return valor.movePointRight(2).longValueExact();
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }
}