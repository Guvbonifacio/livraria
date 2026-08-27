package com.fatec.livraria.model;

import java.time.LocalDateTime;
import java.util.List;

public class Troca {

    private Long id;
    private Pedido pedido;
    private List<ItemCarrinho> itens;
    private String motivo;
    private String status;
    private LocalDateTime dataSolicitacao;
    private boolean retornaEstoque;

    public Troca(Long id, Pedido pedido, List<ItemCarrinho> itens, String motivo) {
        this.id = id;
        this.pedido = pedido;
        this.itens = itens;
        this.motivo = motivo;
        this.status = "TROCA SOLICITADA";
        this.dataSolicitacao = LocalDateTime.now();
    }

    

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public boolean isRetornaEstoque() {
        return retornaEstoque;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRetornaEstoque(boolean retornaEstoque) {
        this.retornaEstoque = retornaEstoque;
    }

}