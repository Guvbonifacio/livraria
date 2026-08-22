package com.fatec.livraria.model;

import java.time.LocalDateTime;
import java.util.List;

public class Pedido {
    private Long id;
    private LocalDateTime dataPedido;
    private String status;
    private Cliente cliente;
    private List<ItemCarrinho> itens;
    private double valorFrete;
    private double valorTotal;

    public Pedido(Long id, Cliente cliente, List<ItemCarrinho> itens, double valorFrete, double valorTotal) {
        this.id = id;
        this.dataPedido = LocalDateTime.now();
        this.status = "EM PROCESSAMENTO";
        this.cliente = cliente;
        this.itens = itens;
        this.valorFrete = valorFrete;
        this.valorTotal = valorTotal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDataPedido() { return dataPedido; }
    public void setDataPedido(LocalDateTime dataPedido) { this.dataPedido = dataPedido; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public List<ItemCarrinho> getItens() { return itens; }
    public void setItens(List<ItemCarrinho> itens) { this.itens = itens; }

    public double getValorFrete() { return valorFrete; }
    public void setValorFrete(double valorFrete) { this.valorFrete = valorFrete; }

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }
}