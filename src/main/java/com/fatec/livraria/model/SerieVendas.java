package com.fatec.livraria.model;

import java.util.List;

public class SerieVendas {
    private String categoria;
    private List<Double> valores;

public SerieVendas(String categoria, List<Double> valores){
    this.categoria = categoria;
    this.valores = valores;
}
 
public String getCategoria(){
    return categoria;
}

public List<Double> getValores(){
    return valores;
}
}

