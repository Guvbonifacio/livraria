package com.fatec.livraria.service;

import com.fatec.livraria.ia.ProvedorIA;
import com.fatec.livraria.ia.ProvedorIA.*;
import com.fatec.livraria.ia.ProvedorIAFalso;
import com.fatec.livraria.repository.PreferenciaRepository;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "livraria.ia.principal=falso",
    "livraria.ia.diagnostico-inicial=false",
    "livraria.carrinho.agendamento-ativo=false"
})
@Import(RecomendacaoServiceTest.RelogioTeste.class)
@AutoConfigureMockMvc
@Sql(scripts={"/cenario-recomendacao.sql","classpath:data.sql","classpath:data-treino.sql"})
class ChatbotServiceTest extends TesteComSessao {
    @Autowired ChatbotService chat;
    @Autowired RecomendacaoService modelo;
    @Autowired PreferenciaService preferencias;
    @Autowired PreferenciaRepository preferenciasRepository;
    @Autowired FerramentasChat ferramentas;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired MockMvc mvc;
    @MockitoBean(name="provedorIA") ProvedorIA provedor;
    @MockitoBean UsuarioAtual usuario;
    @BeforeEach
    void preparar() {
        when(usuario.getClienteId()).thenReturn(1L);
        when(usuario.getIdentificador()).thenReturn("teste-chat");
        var falso=new ProvedorIAFalso(json);
        when(provedor.responder(anyList(),anyList())).thenAnswer(i -> falso.responder(i.getArgument(0),i.getArgument(1)));
        modelo.treinar();
    }
    @Test
    void recomendaPeloModeloEPersisteProtocoloEVinculos() {
        chat.enviar("Recomende livros para mim");
        var historico=chat.historico();
        assertEquals(2,historico.size());
        var resposta=historico.get(1);
        assertEquals("FALSO",resposta.provedor());
        assertEquals(new HashSet<>(modelo.recomendar(1L).stream().map(l -> l.getId()).toList()),
            new HashSet<>(resposta.recomendados().stream().map(l -> l.id()).toList()));
        Long conversa=jdbc.queryForObject("SELECT conversa_id FROM mensagem_chat WHERE id=?",Long.class,resposta.id());
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM mensagem_chat WHERE conversa_id=?",Integer.class,conversa));
        verify(provedor,times(2)).responder(anyList(),anyList());
        assertEquals(historico,chat.historico());
        verifyNoMoreInteractions(provedor); // Consultar a tela não chama a API.
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM log_transacao WHERE tabela='conversa'",Integer.class));
    }
    @Test
    void consultaAutorCorretoEIndisponibilidadeSemMarcarComoRecomendacao() {
        chat.enviar("autor de O Poder do Hábito");
        assertTrue(chat.historico().get(1).texto().contains("Charles Duhigg"));
        assertTrue(chat.historico().get(1).recomendados().isEmpty());
        chat.enviar("estoque de Duna");
        assertTrue(chat.historico().get(3).texto().contains("indisponível"));
    }
    @Test
    void feedbackEhIdempotenteImutavelEProtegidoPorCliente() {
        chat.enviar("Recomende livros");
        Long resposta=chat.historico().get(1).id();
        chat.avaliar(resposta,false);
        chat.avaliar(resposta,false);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM feedback_resposta WHERE mensagem_chat_id=?",Integer.class,resposta));
        assertThrows(ValidacaoException.class,()->chat.avaliar(resposta,true));
        when(usuario.getClienteId()).thenReturn(3L);
        assertTrue(chat.historico().isEmpty());
        assertThrows(ResponseStatusException.class,()->chat.avaliar(resposta,false));
        assertThrows(ResponseStatusException.class,()->preferencias.salvar(1L,List.of()));
    }
    @Test
    void votoEmConversaSemRecomendacaoEhSoQualidade() {
        chat.enviar("Olá");
        Long resposta=chat.historico().get(1).id();
        chat.avaliar(resposta,true);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM recomendacao_mensagem WHERE mensagem_chat_id=?",Integer.class,resposta));
        assertEquals(Boolean.TRUE,chat.historico().get(1).util());
    }
    @Test
    void validaMensagemEArgumentosSemDeixarModeloEscolherCliente() {
        int antes=jdbc.queryForObject("SELECT COUNT(*) FROM mensagem_chat",Integer.class);
        assertThrows(ValidacaoException.class,()->chat.enviar(" "));
        assertThrows(ValidacaoException.class,()->chat.enviar("a".repeat(2001)));
        verifyNoInteractions(provedor);
        assertEquals(antes,jdbc.queryForObject("SELECT COUNT(*) FROM mensagem_chat",Integer.class));
        assertTrue(ferramentas.executar(new Chamada("1","recomendar_livros",Map.of("clienteId",3))).erro());
        assertTrue(ferramentas.executar(new Chamada("2","executar_sql",Map.of())).erro());
        assertTrue(ferramentas.executar(new Chamada("3","detalhar_livro",Map.of("livroId",1.2))).erro());
    }
    @Test
    void limitaLacoDaIAEGravaErroSemRecomendacao() {
        doReturn(new Resposta("FALSO","teste","",
            List.of(new Chamada("c1","buscar_livros",Map.of("termo","Duna"))),Parada.FERRAMENTAS,null))
            .when(provedor).responder(anyList(),anyList());
        assertThrows(ValidacaoException.class,()->chat.enviar("buscar Duna"));
        verify(provedor,times(5)).responder(anyList(),anyList());
        assertEquals("SISTEMA",chat.historico().get(1).provedor());
        assertTrue(chat.historico().get(1).recomendados().isEmpty());
    }
    @Test
    void falhaDoProvedorNaoVazaCredencialNemMensagemTecnica() {
        doThrow(new IllegalStateException("segredo-api")).when(provedor).responder(anyList(),anyList());
        var erro=assertThrows(ValidacaoException.class,()->chat.enviar("Olá"));
        assertFalse(erro.getErros().toString().contains("segredo-api"));
        assertFalse(chat.historico().get(1).texto().contains("segredo-api"));
    }
    @Test
    void preferenciasAcrescentamEventosPreservandoHistoricoDoTreino() {
        var anteriores=jdbc.queryForList("SELECT * FROM preferencia_cliente WHERE cliente_id=1 ORDER BY id");
        Long categoria=preferencias.listar().get(0).id();
        preferencias.salvar(1L,List.of(categoria));
        preferencias.salvar(1L,List.of());
        assertTrue(preferencias.listar().stream().allMatch(o -> o.peso()==0));
        for(var anterior:anteriores)
            assertEquals(anterior,jdbc.queryForMap("SELECT * FROM preferencia_cliente WHERE id=?",anterior.get("id")));
        assertTrue(preferenciasRepository.ultimaVigencia(1L).isAfter(java.time.LocalDateTime.parse("2026-11-17T23:59:59")));
        assertThrows(ValidacaoException.class,()->preferencias.salvar(1L,List.of(-1L)));
    }
    @Test
    void postRetornaAoSiteEHtmlEscapaTextoDoCliente() throws Exception {
        mvc.perform(post("/chat").session(sessao).param("texto","<script>alert(1)</script>")
            .param("retorno","https://outro-site.example"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/#painelChatbot"));
        mvc.perform(get("/").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }
}