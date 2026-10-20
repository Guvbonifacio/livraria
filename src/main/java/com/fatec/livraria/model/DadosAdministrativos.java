package com.fatec.livraria.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class DadosAdministrativos {

    private DadosAdministrativos() {}

    public record Cliente(
        Long id,
        String nome
    ) {}

    public record Livro(
        Long id,
        String titulo,
        BigDecimal preco
    ) {}

    public record Item(
        Long id,
        Livro livro,
        int quantidade
    ) {
        public BigDecimal getSubtotal() {
            return livro.preco().multiply(
                BigDecimal.valueOf(quantidade)
            );
        }
    }

    public record Pagamento(
        Long id,
        String forma,
        BigDecimal valor,
        String status,
        String numeroMascarado,
        String bandeira,
        String codigoCupom
    ) {}

    public record Pedido(
        Long id,
        LocalDateTime dataPedido,
        String status,
        Cliente cliente,
        Endereco enderecoEntrega,
        BigDecimal valorFrete,
        BigDecimal valorTotal,
        List<Item> itens,
        List<Pagamento> pagamentos
    ) {}

    public record Troca(
        Long id,
        Pedido pedido,
        String status,
        String motivo,
        LocalDateTime dataSolicitacao,
        LocalDateTime dataAutorizacao,
        LocalDateTime dataRecebimento,
        Boolean retornaEstoque,
        Long cupomId,
        List<Item> itens
    ) {}
}