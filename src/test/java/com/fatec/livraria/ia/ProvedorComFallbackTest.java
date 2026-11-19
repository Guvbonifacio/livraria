package com.fatec.livraria.ia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.*;
import static com.fatec.livraria.ia.ProvedorIA.*;

@ExtendWith(OutputCaptureExtension.class)
class ProvedorComFallbackTest {

    private final List<Mensagem> conversa =
        List.of(
            Mensagem.cliente("Recomende um livro.")
        );

    private final List<Ferramenta> ferramentas =
        List.of(
            new Ferramenta(
                "recomendar_livros",
                "Recomenda livros disponíveis.",
                Map.of(),
                List.of()
            )
        );

    @Test
    void usaClaudeSemChamarReserva(CapturedOutput saida) {
        var chamadasReserva = new AtomicInteger();

        var esperado = new Resposta(
            "CLAUDE",
            "claude-opus-5-5",
            "Olá",
            List.of(),
            Parada.FIM,
            null
        );

        var provedor = new ProvedorComFallback(
            (c, f) -> esperado,
            (c, f) -> {
                chamadasReserva.incrementAndGet();

                throw new AssertionError(
                    "Reserva não deveria ser chamada."
                );
            }
        );

        assertSame(
            esperado,
            provedor.responder(conversa, ferramentas)
        );

        assertEquals(0, chamadasReserva.get());

        assertTrue(
            saida.getOut()
                .contains("IA atendida provedor=CLAUDE")
        );
    }

    @Test
    void falhaDeRedeUsaOllama(CapturedOutput saida) {
        var esperado = new Resposta(
            "OLLAMA",
            "qwen2.5:7b",
            "",
            List.of(
                new Chamada(
                    "toolu_local",
                    "recomendar_livros",
                    Map.of()
                )
            ),
            Parada.FERRAMENTAS,
            null
        );

        var provedor = new ProvedorComFallback(
            (c, f) -> {
                throw new FalhaProvedor(
                    "CLAUDE",
                    FalhaProvedor.Tipo.CONEXAO,
                    "Rede indisponível.",
                    null
                );
            },
            (c, f) -> {
                assertSame(conversa, c);
                assertSame(ferramentas, f);
                return esperado;
            }
        );

        assertSame(
            esperado,
            provedor.responder(conversa, ferramentas)
        );

        assertTrue(
            saida.getOut()
                .contains("IA atendida provedor=OLLAMA")
        );
    }

    @Test
    void credencialInvalidaUsaReservaSemExporMensagem(
            CapturedOutput saida) {

        var provedor = new ProvedorComFallback(
            (c, f) -> {
                throw new FalhaProvedor(
                    "CLAUDE",
                    FalhaProvedor.Tipo.CREDENCIAL,
                    "NAO_LOGAR_ESTA_MENSAGEM",
                    new RuntimeException("NAO_LOGAR_CAUSA")
                );
            },
            (c, f) -> new Resposta(
                "OLLAMA",
                "qwen2.5:7b",
                "Olá",
                List.of(),
                Parada.FIM,
                null
            )
        );

        assertEquals(
            "OLLAMA",
            provedor.responder(
                conversa, ferramentas
            ).provedor()
        );

        assertFalse(
            saida.getAll().contains("NAO_LOGAR")
        );
    }

    @Test
    void configuracaoInvalidaNaoEhEscondida() {
        var erro = new FalhaProvedor(
            "CLAUDE",
            FalhaProvedor.Tipo.CONFIGURACAO,
            "Modelo não encontrado.",
            null
        );

        var provedor = new ProvedorComFallback(
            (c, f) -> {
                throw erro;
            },
            (c, f) -> {
                throw new AssertionError(
                    "Não deve mascarar configuração inválida."
                );
            }
        );

        assertSame(
            erro,
            assertThrows(
                FalhaProvedor.class,
                () -> provedor.responder(
                    conversa, ferramentas
                )
            )
        );
    }

    @Test
    void duasFalhasProduzemErroControlado() {
        var provedor = new ProvedorComFallback(
            (c, f) -> {
                throw new FalhaProvedor(
                    "CLAUDE",
                    FalhaProvedor.Tipo.CONEXAO,
                    "Sem rede.",
                    null
                );
            },
            (c, f) -> {
                throw new FalhaProvedor(
                    "OLLAMA",
                    FalhaProvedor.Tipo.CONEXAO,
                    "Sem serviço local.",
                    null
                );
            }
        );

        var erro = assertThrows(
            FalhaProvedor.class,
            () -> provedor.responder(
                conversa, ferramentas
            )
        );

        assertEquals("NENHUM", erro.provedor());
        assertEquals(1, erro.getSuppressed().length);

        assertEquals(
            "OLLAMA",
            ((FalhaProvedor) erro.getCause()).provedor()
        );
    }

    @Test
    void modoLocalNaoTentaApiPaga() {
        var erro = new FalhaProvedor(
            "OLLAMA",
            FalhaProvedor.Tipo.CONEXAO,
            "Sem Ollama.",
            null
        );

        var provedor = new ProvedorComFallback(
            (c, f) -> {
                throw erro;
            },
            null
        );

        assertSame(
            erro,
            assertThrows(
                FalhaProvedor.class,
                () -> provedor.responder(
                    conversa, ferramentas
                )
            )
        );
    }

    @Test
    void erroDeProgramacaoNaoAcionaReserva() {
        var erro = new IllegalStateException(
            "Erro de programação."
        );

        var provedor = new ProvedorComFallback(
            (c, f) -> {
                throw erro;
            },
            (c, f) -> {
                throw new AssertionError(
                    "Não deve esconder defeitos de código."
                );
            }
        );

        assertSame(
            erro,
            assertThrows(
                IllegalStateException.class,
                () -> provedor.responder(
                    conversa, ferramentas
                )
            )
        );
    }

    @Test
    void concluiFerramentasNaReservaETentaPrincipalNaNovaPergunta() {
        var tentativas = new AtomicInteger();

        var provedor = new ProvedorComFallback(
            (c, f) -> {
                tentativas.incrementAndGet();

                throw new FalhaProvedor(
                    "CLAUDE",
                    FalhaProvedor.Tipo.CONEXAO,
                    "Sem rede.",
                    null
                );
            },
            (c, f) -> {
                boolean terminouFerramenta =
                    c.getLast().papel()
                        == Papel.FERRAMENTA;

                return new Resposta(
                    "OLLAMA",
                    "qwen2.5:7b",
                    terminouFerramenta ? "O Hobbit." : "",
                    terminouFerramenta
                        ? List.of()
                        : List.of(
                            new Chamada(
                                "toolu_local",
                                "recomendar_livros",
                                Map.of()
                            )
                        ),
                    terminouFerramenta
                        ? Parada.FIM
                        : Parada.FERRAMENTAS,
                    new ConteudoNativo(
                        "OLLAMA",
                        List.of()
                    )
                );
            }
        );

        var resposta = provedor.responder(
            conversa, ferramentas
        );

        var continuacao = new ArrayList<>(conversa);

        continuacao.add(resposta.mensagem());

        continuacao.add(
            Mensagem.resultado(
                resposta.chamadas().get(0),
                "[{\"id\":1}]",
                false
            )
        );

        assertEquals(
            "O Hobbit.",
            provedor.responder(
                continuacao, ferramentas
            ).texto()
        );

        assertEquals(1, tentativas.get());

        continuacao.add(
            Mensagem.cliente("Outra pergunta.")
        );

        provedor.responder(
            continuacao, ferramentas
        );

        assertEquals(2, tentativas.get());
    }
}