package com.fatec.livraria.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "livraria.carrinho.agendamento-ativo=false"
})
@Sql("/cenario-checkout.sql")
class AnaliseVendasServiceTest extends TesteComSessao {

    @Autowired
    AnaliseVendasService service;

    @Autowired
    UsuarioAtual usuario;

    @Autowired
    JdbcTemplate jdbc;

    @Override
    protected String perfilInicial() {
        return "ADMIN";
    }

    private void venda(
            String data,
            String isbn,
            int quantidade,
            long preco) {

        long total = quantidade * preco;

        jdbc.update("""
            INSERT INTO pedido (
                cliente_id,
                data_pedido,
                status,
                endereco_entrega_json,
                subtotal_centavos,
                frete_centavos,
                desconto_centavos,
                total_centavos
            )
            VALUES (1, ?, 'APROVADA', '{}', ?, 0, 0, ?)
            """,
            data,
            total,
            total
        );

        Long id = jdbc.queryForObject(
            "SELECT last_insert_rowid()",
            Long.class
        );

        jdbc.update("""
            INSERT INTO item_pedido (
                pedido_id,
                livro_id,
                titulo,
                quantidade,
                preco_unitario_centavos
            )
            SELECT ?, id, titulo, ?, ?
            FROM livro
            WHERE isbn = ?
            """,
            id,
            quantidade,
            preco,
            isbn
        );
    }

    @Test
    void exigeAdministradorAntesDeValidarOsFiltros() {
        usuario.simularPerfil("CLIENTE");

        assertThrows(
            AcessoAdministrativoException.class,
            () -> service.analisar(null, null, null)
        );
    }

    @Test
    void rf0056RecusaFimAnteriorAoInicio() {
        var erro = assertThrows(
            ValidacaoException.class,
            () -> service.analisar(
                "2026-03-01",
                "2026-02-28",
                null
            )
        );

        assertTrue(
            erro.getErros().get(0).contains("anterior")
        );
    }

    @Test
    void rn0072RecusaPeriodoMenorQueUmMes() {
        assertThrows(
            ValidacaoException.class,
            () -> service.analisar(
                "2026-02-01",
                "2026-02-15",
                null
            )
        );
    }

    @Test
    void rn0072AceitaFevereiroCompletoComoUmMes() {
        var resultado = service.analisar(
            "2026-02-01",
            "2026-02-28",
            List.of("Fantasia")
        );

        assertEquals(
            List.of("fev/2026"),
            resultado.etiquetas()
        );

        assertEquals(
            List.of(new BigDecimal("0.00")),
            resultado.series().get(0).valores()
        );
    }

    @Test
    void rn0072AceitaExatamenteVinteQuatroMeses() {
        var resultado = service.analisar(
            "2024-11-01",
            "2026-10-31",
            List.of("Fantasia")
        );

        assertEquals(24, resultado.etiquetas().size());

        assertEquals(
            "nov/2024",
            resultado.etiquetas().get(0)
        );

        assertEquals(
            "out/2026",
            resultado.etiquetas().get(23)
        );
    }

    @Test
    void rn0072RecusaVinteCincoMeses() {
        assertThrows(
            ValidacaoException.class,
            () -> service.analisar(
                "2024-10-01",
                "2026-10-31",
                null
            )
        );
    }

    @Test
    void aceitaDatasParciaisQueCompletamUmMes() {
        var resultado = service.analisar(
            "2026-01-15",
            "2026-02-14",
            List.of("Fantasia")
        );

        assertEquals(
            List.of("jan/2026", "fev/2026"),
            resultado.etiquetas()
        );
    }

    @Test
    void rn0073PreencheLacunaEConverteCentavosSemPerderPrecisao() {
        venda(
            "2026-01-15T12:00:00",
            "9788595084742",
            2,
            12345
        );

        venda(
            "2026-03-31T23:59:59",
            "9788595084742",
            1,
            7856
        );

        // Deve ficar fora do intervalo consultado.
        venda(
            "2026-04-01T00:00:00",
            "9788595084742",
            1,
            99999
        );

        var resultado = service.analisar(
            "2026-01-01",
            "2026-03-31",
            List.of("Fantasia")
        );

        assertEquals(
            List.of(
                "jan/2026",
                "fev/2026",
                "mar/2026"
            ),
            resultado.etiquetas()
        );

        assertEquals(
            List.of(
                new BigDecimal("246.90"),
                new BigDecimal("0.00"),
                new BigDecimal("78.56")
            ),
            resultado.series().get(0).valores()
        );
    }

    @Test
    void rn0073CategoriaSemNenhumaVendaTambemRecebeSerieDeZeros() {
        venda(
            "2026-01-15T12:00:00",
            "9788595084742",
            1,
            3000
        );

        var resultado = service.analisar(
            "2026-01-01",
            "2026-03-31",
            List.of("Literatura Clássica")
        );

        assertEquals(1, resultado.series().size());

        assertEquals(
            "Literatura Clássica",
            resultado.series().get(0).categoria()
        );

        assertEquals(
            List.of(
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00")
            ),
            resultado.series().get(0).valores()
        );
    }

    @Test
    void filtraCategoriasEmJavaESemDuplicarSerie() {
        venda(
            "2026-02-15T12:00:00",
            "9788576573135",
            1,
            1999
        );

        var resultado = service.analisar(
            "2026-02-01",
            "2026-02-28",
            List.of(
                "Ficção Científica",
                "Ficção Científica"
            )
        );

        assertEquals(1, resultado.series().size());

        assertEquals(
            "Ficção Científica",
            resultado.series().get(0).categoria()
        );

        assertEquals(
            List.of(new BigDecimal("19.99")),
            resultado.series().get(0).valores()
        );
    }

    @Test
    void categoriasNulasSelecionamTodasAsCadastradas() {
        var resultado = service.analisar(
            "2026-02-01",
            "2026-02-28",
            null
        );

        assertEquals(4, resultado.series().size());

        assertTrue(
            resultado.series().stream().allMatch(serie ->
                serie.valores().equals(
                    List.of(new BigDecimal("0.00"))
                )
            )
        );
    }

    @Test
    void recusaDatasAusentesOuInvalidas() {
        assertThrows(
            ValidacaoException.class,
            () -> service.analisar(
                null,
                "2026-02-28",
                null
            )
        );

        assertThrows(
            ValidacaoException.class,
            () -> service.analisar(
                "2026-02-01",
                "",
                null
            )
        );

        assertThrows(
            ValidacaoException.class,
            () -> service.analisar(
                "2026-02-30",
                "2026-03-31",
                null
            )
        );
    }

    @Test
    void recusaCategoriasVaziasOuDesconhecidas() {
        assertThrows(
            ValidacaoException.class,
            () -> service.analisar(
                "2026-02-01",
                "2026-02-28",
                List.of()
            )
        );

        assertThrows(
            ValidacaoException.class,
            () -> service.analisar(
                "2026-02-01",
                "2026-02-28",
                List.of("Inexistente")
            )
        );
    }
}