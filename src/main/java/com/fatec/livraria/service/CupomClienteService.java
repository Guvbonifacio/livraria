package com.fatec.livraria.service;

import com.fatec.livraria.repository.CupomClienteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CupomClienteService {

    public record CupomVista(
        String codigo,
        String tipo,
        BigDecimal valor,
        LocalDate validade,
        String situacao
    ) {}

    private final CupomClienteRepository cupons;
    private final UsuarioAtual usuario;

    public CupomClienteService(
            CupomClienteRepository cupons,
            UsuarioAtual usuario) {

        this.cupons = cupons;
        this.usuario = usuario;
    }

    public List<CupomVista> listar() {
        LocalDate hoje = LocalDate.now();

        return cupons.listar(usuario.getClienteId())
            .stream()
            .map(c -> new CupomVista(
                c.codigo(),
                c.tipo(),
                c.valor(),
                c.validade(),
                !c.ativo()
                    ? "UTILIZADO"
                    : c.validade().isBefore(hoje)
                        ? "EXPIRADO"
                        : "DISPONÍVEL"
            ))
            .toList();
    }
}