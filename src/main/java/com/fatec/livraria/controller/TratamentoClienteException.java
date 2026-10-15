package com.fatec.livraria.controller;

import com.fatec.livraria.service.ClienteNaoEncontradoException;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class TratamentoClienteException {

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String clienteNaoEncontrado(
            ClienteNaoEncontradoException exception,
            Model model) {

        model.addAttribute("mensagem", exception.getMessage());

        return "cliente-nao-encontrado";
    }
}