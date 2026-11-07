package com.fatec.livraria.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "livraria.carrinho.agendamento-ativo=false"
})
@Sql("/cenario-checkout.sql")
class PerfilAdministradorTest extends TesteComSessao {

    @Autowired UsuarioAtual usuario;
    @Autowired AdministracaoService administracao;
    @Autowired ClienteService clientes;
    @Autowired ProcessamentoPedidoService processamento;
    @Autowired EntregaPedidoService entrega;
    @Autowired AutorizacaoTrocaService autorizacao;
    @Autowired ConclusaoTrocaService conclusao;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = webAppContextSetup(contexto).build();
    }

    @Test
    void perfilInicialEClienteEValorInvalidoNaoElevaPermissao() {
        assertEquals("CLIENTE", usuario.getPerfil());

        assertThrows(
            ValidacaoException.class,
            () -> usuario.simularPerfil("ROOT")
        );

        assertThrows(
            ValidacaoException.class,
            () -> usuario.simularPerfil(null)
        );

        assertEquals("CLIENTE", usuario.getPerfil());
    }

    @Test
    void servicosBloqueiamClienteAntesDeGravar() {
        assertThrows(
            AcessoAdministrativoException.class,
            () -> administracao.listarPedidos(null, null, null)
        );

        assertThrows(
            AcessoAdministrativoException.class,
            () -> clientes.inativar(1L)
        );

        assertThrows(
            AcessoAdministrativoException.class,
            () -> processamento.processar(1L, "APROVADA")
        );

        assertThrows(
            AcessoAdministrativoException.class,
            () -> entrega.transicionar(1L, "EM TRANSPORTE")
        );

        assertThrows(
            AcessoAdministrativoException.class,
            () -> autorizacao.autorizar(1L, "ACEITAR")
        );

        assertThrows(
            AcessoAdministrativoException.class,
            () -> conclusao.receber(1L, true)
        );

        assertThrows(
            AcessoAdministrativoException.class,
            () -> conclusao.finalizar(1L)
        );

        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT ativo FROM cliente WHERE id = 1",
                Integer.class
            )
        );

        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM log_transacao",
                Integer.class
            )
        );
    }

    @Test
    void acessoDiretoPorHttpTambemEProibido() throws Exception {
        for (String url : List.of(
                "/admin",
                "/admin/clientes",
                "/admin/clientes/1",
                "/admin/pedidos",
                "/admin/pedidos/1",
                "/admin/trocas",
                "/admin/trocas/1",
                "/admin/analise")) {

            mvc.perform(get(url).session(sessao))
                .andExpect(status().isForbidden());
        }

        mvc.perform(
                post("/admin/clientes/1/inativar")
                    .session(sessao)
            )
            .andExpect(status().isForbidden());

        mvc.perform(
                post("/admin/pedidos/1/status")
                    .session(sessao)
                    .param("novoStatus", "APROVADA")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void trocaPersisteNaSessaoSemAlterarPerfilNoBanco()
            throws Exception {

        mvc.perform(
                post("/sessao/perfil")
                    .session(sessao)
                    .param("perfil", "ADMIN")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        assertEquals("ADMIN", usuario.getPerfil());
        assertDoesNotThrow(usuario::exigirAdministrador);

        mvc.perform(get("/admin").session(sessao))
            .andExpect(status().isOk());

        assertEquals(
            "CLIENTE",
            jdbc.queryForObject(
                "SELECT perfil FROM cliente WHERE id = 1",
                String.class
            )
        );

        assertEquals(1L, usuario.getClienteId());

        mvc.perform(
                post("/sessao/perfil")
                    .session(sessao)
                    .param("perfil", "CLIENTE")
            )
            .andExpect(status().is3xxRedirection());

        mvc.perform(get("/admin").session(sessao))
            .andExpect(status().isForbidden());
    }

    @Test
    void duasSessoesNaoCompartilhamPerfil() throws Exception {
        mvc.perform(
                post("/sessao/perfil")
                    .session(sessao)
                    .param("perfil", "ADMIN")
            )
            .andExpect(status().is3xxRedirection());

        var outra = new MockHttpSession(
            contexto.getServletContext()
        );

        mvc.perform(get("/admin").session(outra))
            .andExpect(status().isForbidden());

        mvc.perform(get("/admin").session(sessao))
            .andExpect(status().isOk());
    }

    @Test
    void perfilForjadoRetorna400SemMudarSessao()
            throws Exception {

        mvc.perform(
                post("/sessao/perfil")
                    .session(sessao)
                    .param("perfil", "ROOT")
            )
            .andExpect(status().isBadRequest());

        assertEquals("CLIENTE", usuario.getPerfil());
    }
}