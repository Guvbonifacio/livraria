package com.fatec.livraria.service;

import com.fatec.livraria.repository.PreferenciaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PreferenciaService {
    private final PreferenciaRepository repository;
    private final UsuarioAtual usuario;
    private final ClienteService clientes;
    private final AuditoriaService auditoria;
    private final Clock relogio;
    public PreferenciaService(PreferenciaRepository repository,UsuarioAtual usuario,
            ClienteService clientes,AuditoriaService auditoria,Clock relogio) {
        this.repository=repository;this.usuario=usuario;this.clientes=clientes;
        this.auditoria=auditoria;this.relogio=relogio;
    }
    @Transactional(readOnly=true)
    public List<PreferenciaRepository.Opcao> listar() {return repository.listar(usuario.getClienteId());}

    @Transactional
    public void salvar(Long cliente,List<Long> categorias) {
        if(!usuario.getClienteId().equals(cliente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Altere somente suas preferências.");
        clientes.exigirCliente(cliente);
        var opcoes=repository.listar(cliente);
        Set<Long> escolhidas=new HashSet<>(categorias==null?List.of():categorias);
        if(escolhidas.stream().anyMatch(id -> opcoes.stream().noneMatch(o -> o.id().equals(id))))
            throw new ValidacaoException(List.of("Selecione somente categorias cadastradas."));
        LocalDateTime agora=LocalDateTime.ofInstant(relogio.instant(),ZoneId.systemDefault());
        LocalDateTime ultima=repository.ultimaVigencia(cliente);
        // Evita colisão mesmo quando duas alterações ocorrem no mesmo instante.
        if(ultima!=null && !agora.isAfter(ultima)) agora=ultima.plusNanos(1);
        for(var opcao:opcoes) {
            double peso=escolhidas.contains(opcao.id())?1.0:0.0;
            if(Double.compare(peso,opcao.peso())!=0) {
                Long id=repository.salvar(cliente,opcao.id(),peso,agora);
                auditoria.registrar("INSERT","preferencia_cliente",id,
                    Map.of("clienteId",cliente,"categoriaId",opcao.id(),"peso",peso,"vigenteDe",agora.toString()));
            }
        }
    }
}