package com.fatec.livraria.service;

import com.fatec.livraria.repository.RecomendacaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "livraria.ia.principal=falso", "livraria.ia.diagnostico-inicial=false",
    "livraria.carrinho.agendamento-ativo=false"
})
@AutoConfigureMockMvc
@Sql(scripts={"/cenario-recomendacao.sql","classpath:data.sql","classpath:data-treino.sql"})
class RetreinoRecomendacaoTest extends TesteComSessao {
    @Autowired RecomendacaoAdminService admin;
    @Autowired RecomendacaoRepository repository;
    @Autowired ChatbotService chat;
    @Autowired UsuarioAtual usuario;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoBean Clock relogio;

    @Override protected String perfilInicial() {return "ADMIN";}
    private void em(String instante) {
        when(relogio.instant()).thenReturn(Instant.parse(instante));
        when(relogio.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void feedbackNovoEntraNoRetreinoEAnteriorPermaneceInteira() {
        em("2026-11-18T15:00:00Z");
        var anterior=admin.treinar(null,null,null);
        var cabecalho=jdbc.queryForMap("SELECT * FROM modelo_recomendacao WHERE id=?",anterior.id());
        var coeficientes=jdbc.queryForList("SELECT * FROM coeficiente_modelo WHERE modelo_id=? ORDER BY feature",anterior.id());
        var vieses=jdbc.queryForList("SELECT * FROM vies_categoria WHERE modelo_id=? ORDER BY categoria_id",anterior.id());
        chat.enviar("Recomende livros");
        var resposta=chat.historico().get(1);
        Long categoria=jdbc.queryForObject("SELECT categoria_id FROM livro_categoria WHERE livro_id=? ORDER BY categoria_id LIMIT 1",
            Long.class,resposta.recomendados().get(0).id());
        var corte=java.time.LocalDateTime.parse("2026-11-19T00:00:00");
        var fim=java.time.LocalDateTime.parse("2026-11-20T00:00:00");
        long votosAntes=repository.extrair(corte,fim).stream()
            .filter(l -> l.clienteId()==1L&&l.categoriaId()==categoria.longValue())
            .findFirst().orElseThrow().votosNaoUteis();
        chat.avaliar(resposta.id(),false); // Evento real da aplicação, após a versão inicial.
        var linha=repository.extrair(corte,fim).stream()
            .filter(l -> l.clienteId()==1L&&l.categoriaId()==categoria.longValue()).findFirst().orElseThrow();
        assertEquals(votosAntes+1,linha.votosNaoUteis());

        // Compra/feedback positivo em outra janela fornece rótulos positivos; os outros pares ficam negativos.
        em("2026-11-19T15:00:00Z");
        chat.enviar("Recomende livros");
        chat.avaliar(chat.historico().get(3).id(),true);
        em("2026-11-21T15:00:00Z");
        var nova=admin.treinar("2026-11-19","2026-11-19","2026-11-20");
        assertNotEquals(anterior.id(),nova.id());
        assertNotEquals(anterior.versao(),nova.versao());
        assertEquals(80,nova.amostras());
        var antes=anterior.coeficientes().stream().filter(c -> c.feature().equals("votos_nao_uteis")).findFirst().orElseThrow();
        var depois=nova.coeficientes().stream().filter(c -> c.feature().equals("votos_nao_uteis")).findFirst().orElseThrow();
        assertTrue(depois.media()>antes.media());
        double mediaEsperada=repository.extrair(corte,fim).stream()
            .mapToLong(l -> l.votosNaoUteis()).average().orElseThrow();
        assertEquals(mediaEsperada,depois.media(),1e-12);
        assertEquals(anterior,repository.buscar(anterior.id()));
        assertEquals(cabecalho,jdbc.queryForMap("SELECT * FROM modelo_recomendacao WHERE id=?",anterior.id()));
        assertEquals(coeficientes,jdbc.queryForList("SELECT * FROM coeficiente_modelo WHERE modelo_id=? ORDER BY feature",anterior.id()));
        assertEquals(vieses,jdbc.queryForList("SELECT * FROM vies_categoria WHERE modelo_id=? ORDER BY categoria_id",anterior.id()));
        assertEquals(nova,repository.buscarAtual());
        assertEquals(List.of(nova,anterior),admin.painel().versoes());
    }

    @Test
    void telaMostraHistoricoEFormularioCriaVersaoSemQuebrarJsonAnterior() throws Exception {
        em("2026-11-18T15:00:00Z");
        mvc.perform(get("/admin/recomendacao").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Nenhum modelo treinado")));
        mvc.perform(post("/admin/recomendacao/treinar").session(sessao))
            .andExpect(status().isOk()).andExpect(jsonPath("$.amostras").value(80));
        mvc.perform(post("/admin/recomendacao/treinar").session(sessao).param("origem","tela")
            .param("dataCorte","2026-11-01").param("fimObservacao","2026-11-10").param("fimValidacao","2026-11-17"))
            .andExpect(redirectedUrl("/admin/recomendacao"));
        mvc.perform(get("/admin/recomendacao").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("votos_nao_uteis")))
            .andExpect(content().string(containsString("Vigente")));
        assertEquals(2,admin.painel().versoes().size());
    }

    @Test
    void clienteNaoConsultaNemTreinaENenhumaVersaoEhGravada() throws Exception {
        usuario.simularPerfil("CLIENTE");
        assertThrows(AcessoAdministrativoException.class,()->admin.painel());
        assertThrows(AcessoAdministrativoException.class,()->admin.treinar(null,null,null));
        mvc.perform(get("/admin/recomendacao").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/recomendacao/treinar").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/recomendacao/treinar").session(sessao).param("origem","tela"))
            .andExpect(status().isForbidden());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM modelo_recomendacao",Integer.class));
    }

    @Test
    void datasInvalidasNaoGravamEValidacaoFuturaFicaPendente() {
        em("2026-11-18T15:00:00Z");
        assertThrows(ValidacaoException.class,()->admin.treinar("errada","2026-11-10","2026-11-17"));
        assertThrows(ValidacaoException.class,()->admin.treinar("2026-11-20","2026-11-19","2026-11-21"));
        assertThrows(ValidacaoException.class,()->admin.treinar("2026-11-01",null,null));
        assertThrows(ValidacaoException.class,()->admin.treinar("2026-11-19","2026-11-20","2026-11-21"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM modelo_recomendacao",Integer.class));
        assertNull(admin.treinar("2026-11-01","2026-11-10","2026-11-20").metricaValidacao());
    }
}