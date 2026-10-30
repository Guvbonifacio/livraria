package com.fatec.livraria.service;

import com.fatec.livraria.model.ItemCarrinho;
import com.fatec.livraria.repository.LivroRepository;
import com.fatec.livraria.repository.NotificacaoRepository;
import com.fatec.livraria.repository.ReservaCarrinhoRepository;
import com.fatec.livraria.repository.ReservaCarrinhoRepository.Reserva;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaCarrinhoService {

    private final ReservaCarrinhoRepository reservas;
    private final LivroRepository livros;
    private final NotificacaoRepository notificacoes;
    private final UsuarioAtual usuario;
    private final AuditoriaService auditoria;
    private final Clock relogio;
    private final int minutos;

    public ReservaCarrinhoService(
            ReservaCarrinhoRepository reservas,
            LivroRepository livros,
            NotificacaoRepository notificacoes,
            UsuarioAtual usuario,
            AuditoriaService auditoria,
            Clock relogio,
            @Value("${livraria.carrinho.reserva-minutos:30}")
            int minutos) {

        if (minutos <= 5) {
            throw new IllegalArgumentException(
                "A reserva deve durar mais de cinco minutos."
            );
        }

        this.reservas = reservas;
        this.livros = livros;
        this.notificacoes = notificacoes;
        this.usuario = usuario;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.minutos = minutos;
    }

    public int getMinutos() {
        return minutos;
    }

    public List<String> listarSessoes() {
        return reservas.listarSessoes();
    }

    @Transactional
    public List<Reserva> processar(String sessao) {
        Instant agora = relogio.instant();
        var linhas = reservas.listar(sessao);

        var vencidas = linhas.stream()
            .filter(r ->
                !r.expirado()
                && !r.expiraEm().isAfter(agora)
            )
            .toList();

        var aviso = linhas.stream()
            .filter(r ->
                !r.expirado()
                && !r.avisado()
                && r.expiraEm().isAfter(agora)
                && !r.expiraEm()
                    .minusSeconds(300)
                    .isAfter(agora)
            )
            .toList();

        if (!vencidas.isEmpty()) {
            for (var r : vencidas) {
                atualizar(
                    r,
                    r.quantidade(),
                    r.expiraEm(),
                    r.avisado(),
                    true
                );
            }

            notificar(
                vencidas,
                "O prazo de reserva do carrinho expirou. "
                + "Produtos removidos: "
                + vencidas.stream()
                    .map(r ->
                        r.titulo() + " (" + r.quantidade() + ")"
                    )
                    .toList()
                + ". Adicione novamente os produtos "
                + "que deseja comprar."
            );
        }

        if (!aviso.isEmpty()) {
            for (var r : aviso) {
                atualizar(
                    r,
                    r.quantidade(),
                    r.expiraEm(),
                    true,
                    false
                );
            }

            String horario = DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.of("America/Sao_Paulo"))
                .format(aviso.get(0).expiraEm());

            notificar(
                aviso,
                "A reserva do seu carrinho expira "
                + "em cinco minutos, às " + horario
                + ". Finalize a compra antes do prazo."
            );
        }

        return reservas.listar(sessao);
    }

    private void notificar(
            List<Reserva> linhas,
            String mensagem) {

        Long cliente = linhas.get(0).clienteId();

        // Compatibilidade com antigas fixtures sem proprietário.
        // Reservas criadas pela aplicação sempre recebem cliente_id.
        if (cliente == null) {
            return;
        }

        Long id = notificacoes.salvar(
            cliente,
            mensagem,
            LocalDateTime.now(relogio)
        );

        auditoria.registrar(
            "INSERT",
            "notificacao",
            id,
            Map.of(
                "clienteId", cliente,
                "origem", "RN0044",
                "mensagem", mensagem
            )
        );
    }

    private void atualizar(
            Reserva reserva,
            int quantidade,
            Instant prazo,
            boolean aviso,
            boolean expirado) {

        int alterados = reservas.atualizar(
            reserva.id(),
            quantidade,
            prazo,
            aviso,
            expirado
        );

        if (alterados != 1) {
            throw erro("A reserva mudou. Tente novamente.");
        }

        auditoria.registrar(
            "UPDATE",
            "reserva_estoque",
            reserva.id(),
            Map.of(
                "livroId", reserva.livroId(),
                "quantidade", quantidade,
                "expiraEm", prazo.toString(),
                "avisoEnviado", aviso,
                "expirado", expirado
            )
        );
    }

    @Transactional
    public void reservar(
            String sessao,
            Long livroId,
            int quantidade,
            int anterior,
            boolean renovar) {

        if (quantidade <= 0) {
            throw erro("A quantidade deve ser maior que zero.");
        }

        Instant agora = relogio.instant();
        var linhas = reservas.listar(sessao);

        var existente = linhas.stream()
            .filter(r -> r.livroId().equals(livroId))
            .findFirst()
            .orElse(null);

        boolean prazoVencido = linhas.stream().anyMatch(r ->
            !r.expirado()
            && !r.expiraEm().isAfter(agora)
        );

        if (prazoVencido
                || (anterior > 0
                    && (existente == null
                        || existente.expirado()))) {

            throw erro(
                "A reserva expirou. Revise o carrinho "
                + "e adicione o produto novamente."
            );
        }

        // A consulta exclui somente as reservas desta sessão.
        var livro = livros.buscarPorId(livroId, sessao);

        if (livro == null || quantidade > livro.getEstoque()) {
            throw erro(
                "Quantidade solicitada acima do estoque disponível."
            );
        }

        if (!renovar && existente == null) {
            throw erro("Reserva não encontrada.");
        }

        Instant prazo = renovar
            ? agora.plusSeconds(minutos * 60L)
            : existente.expiraEm();

        if (existente == null) {
            Long id = reservas.salvar(
                sessao,
                usuario.getClienteId(),
                livroId,
                quantidade,
                prazo
            );

            auditoria.registrar(
                "INSERT",
                "reserva_estoque",
                id,
                Map.of(
                    "livroId", livroId,
                    "quantidade", quantidade,
                    "expiraEm", prazo.toString()
                )
            );

        } else {
            atualizar(
                existente,
                quantidade,
                prazo,
                renovar ? false : existente.avisado(),
                false
            );
        }

        if (renovar) {
            // RN0044: o prazo pertence ao carrinho inteiro.
            for (var r : reservas.listar(sessao)) {
                if (!r.expirado()
                        && !r.livroId().equals(livroId)) {

                    atualizar(
                        r,
                        r.quantidade(),
                        prazo,
                        false,
                        false
                    );
                }
            }
        }
    }

    @Transactional
    public void remover(String sessao, Long livroId) {
        for (var r : reservas.listar(sessao)) {
            if (r.livroId().equals(livroId)) {
                excluir(r);
            }
        }
    }

    private void excluir(Reserva reserva) {
        if (reservas.excluir(reserva.id()) != 1) {
            throw erro("A reserva mudou. Tente novamente.");
        }

        auditoria.registrar(
            "DELETE",
            "reserva_estoque",
            reserva.id(),
            Map.of(
                "livroId", reserva.livroId(),
                "quantidade", reserva.quantidade()
            )
        );
    }

    // RN0032: acompanha os ajustes de quantidade sem renovar prazo.
    @Transactional
    public void reconciliar(
            String sessao,
            Map<Long, Integer> quantidades) {

        for (var r : reservas.listar(sessao)) {
            if (r.expirado()) {
                continue;
            }

            Integer quantidade = quantidades.get(r.livroId());

            if (quantidade == null) {
                excluir(r);

            } else if (quantidade != r.quantidade()) {
                atualizar(
                    r,
                    quantidade,
                    r.expiraEm(),
                    r.avisado(),
                    false
                );
            }
        }
    }

    // Chamado dentro da mesma transação que grava o pedido.
    @Transactional
    public void vincular(
            String sessao,
            Long pedido,
            List<ItemCarrinho> itens) {

        if (sessao == null) {
            return;
        }

        var linhas = reservas.listar(sessao);

        for (var item : itens) {
            var reserva = linhas.stream()
                .filter(r ->
                    r.livroId().equals(item.getLivro().getId())
                )
                .findFirst()
                .orElseThrow(() -> erro(
                    "Reserva não encontrada. Revise o carrinho."
                ));

            if (reserva.quantidade() != item.getQuantidade()
                    || reserva.expirado()
                    || reservas.vincular(
                        reserva.id(),
                        sessao,
                        "pedido:" + pedido,
                        relogio.instant()
                    ) != 1) {

                throw erro(
                    "A reserva expirou ou mudou. Revise o carrinho."
                );
            }

            auditoria.registrar(
                "UPDATE",
                "reserva_estoque",
                reserva.id(),
                Map.of(
                    "pedidoId", pedido,
                    "livroId", reserva.livroId()
                )
            );
        }
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }
}