package com.fatec.livraria.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatbotRepository {
    public record Registro(Long id, String papel, String tipo, String texto,
        String conteudoJson, String provedor, LocalDateTime criadaEm, Boolean util) {}
    private final JdbcTemplate jdbc;
    public ChatbotRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Long criarConversa(Long cliente, LocalDateTime data) {
        return jdbc.queryForObject("INSERT INTO conversa(cliente_id,iniciada_em) VALUES (?,?) RETURNING id",
            Long.class, cliente, data.toString());
    }
    public boolean pertence(Long conversa, Long cliente) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM conversa WHERE id=? AND cliente_id=?",
            Integer.class, conversa, cliente) == 1;
    }
    public Long salvar(Long conversa, String papel, String tipo, String texto,
            String json, String provedor, LocalDateTime data) {
        return jdbc.queryForObject("""
            INSERT INTO mensagem_chat(conversa_id,papel,tipo,texto,conteudo_json,provedor,criada_em)
            VALUES (?,?,?,?,?,?,?) RETURNING id
            """, Long.class, conversa, papel, tipo, texto, json, provedor, data.toString());
    }
    public List<Registro> listar(Long conversa, Long cliente) {
        return jdbc.query("""
            SELECT m.*, f.util FROM mensagem_chat m JOIN conversa c ON c.id=m.conversa_id
            LEFT JOIN feedback_resposta f ON f.mensagem_chat_id=m.id
            WHERE c.id=? AND c.cliente_id=? ORDER BY m.id
            """, (rs,n) -> new Registro(rs.getLong("id"),rs.getString("papel"),rs.getString("tipo"),
                rs.getString("texto"),rs.getString("conteudo_json"),rs.getString("provedor"),
                LocalDateTime.parse(rs.getString("criada_em")),
                rs.getObject("util")==null ? null : rs.getInt("util")==1), conversa, cliente);
    }
    public void vincular(Long mensagem, Long livro) {
        jdbc.update("INSERT INTO recomendacao_mensagem(mensagem_chat_id,livro_id) VALUES (?,?)", mensagem,livro);
    }
    public Map<Long,List<Long>> recomendacoes(Long conversa,Long cliente) {
        Map<Long,List<Long>> resultado=new LinkedHashMap<>();
        jdbc.query("""
            SELECT r.mensagem_chat_id,r.livro_id FROM recomendacao_mensagem r
            JOIN mensagem_chat m ON m.id=r.mensagem_chat_id
            JOIN conversa c ON c.id=m.conversa_id WHERE c.id=? AND c.cliente_id=?
            ORDER BY r.mensagem_chat_id,r.livro_id
            """, rs -> {resultado.computeIfAbsent(rs.getLong(1),k->new ArrayList<>()).add(rs.getLong(2));},
            conversa,cliente);
        return resultado;
    }
    public boolean respostaPertence(Long mensagem,Long cliente) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM mensagem_chat m JOIN conversa c ON c.id=m.conversa_id
            WHERE m.id=? AND c.cliente_id=? AND m.papel='ASSISTENTE' AND m.tipo='TEXTO'
            """,Integer.class,mensagem,cliente)==1;
    }
    public Long registrarFeedback(Long mensagem,boolean util,LocalDateTime data) {
        var ids=jdbc.query("""
            INSERT INTO feedback_resposta(mensagem_chat_id,util,criada_em) VALUES (?,?,?)
            ON CONFLICT(mensagem_chat_id) DO NOTHING RETURNING id
            """,(rs,n)->rs.getLong(1),mensagem,util?1:0,data.toString());
        return ids.isEmpty()?null:ids.get(0);
    }
    public Boolean feedback(Long mensagem) {
        var valores=jdbc.query("SELECT util FROM feedback_resposta WHERE mensagem_chat_id=?",
            (rs,n)->rs.getInt(1)==1,mensagem);
        return valores.isEmpty()?null:valores.get(0);
    }
}