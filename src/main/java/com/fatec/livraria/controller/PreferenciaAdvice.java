package com.fatec.livraria.controller;

import com.fatec.livraria.repository.PreferenciaRepository;
import com.fatec.livraria.service.PreferenciaService;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes=ClienteController.class)
public class PreferenciaAdvice {
    private final PreferenciaService preferencias;
    public PreferenciaAdvice(PreferenciaService preferencias) {this.preferencias=preferencias;}
    @ModelAttribute("opcoesPreferencia")
    public List<PreferenciaRepository.Opcao> opcoes() {return preferencias.listar();}
}