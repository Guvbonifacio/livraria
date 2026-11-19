package com.fatec.livraria.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PreferenciaRepository {
    public record Opcao(Long id,String nome,double peso) {}
    private final JdbcTemplate jdbc;
    public PreferenciaRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public List<Opcao> listar(Long cliente) {
        return jdbc.query("""
            WITH ultimas AS (
                SELECT categoria_id,peso_declarado,
                    ROW_NUMBER() OVER(PARTITION BY categoria_id ORDER BY vigente_de DESC,id DESC) AS posicao
                FROM preferencia_cliente WHERE cliente_id=?
            )
            SELECT c.id,c.nome,COALESCE(p.peso_declarado,0) AS peso
            FROM categoria c LEFT JOIN ultimas p ON p.categoria_id=c.id AND p.posicao=1
            ORDER BY c.nome
            """,(rs,n)->new Opcao(rs.getLong("id"),rs.getString("nome"),rs.getDouble("peso")),cliente);
    }
    public LocalDateTime ultimaVigencia(Long cliente) {
        String valor=jdbc.queryForObject("SELECT MAX(vigente_de) FROM preferencia_cliente WHERE cliente_id=?",String.class,cliente);
        return valor==null?null:LocalDateTime.parse(valor);
    }
    public Long salvar(Long cliente,Long categoria,double peso,LocalDateTime vigente) {
        return jdbc.queryForObject("""
            INSERT INTO preferencia_cliente(cliente_id,categoria_id,peso_declarado,vigente_de)
            VALUES (?,?,?,?) RETURNING id
            """,Long.class,cliente,categoria,peso,vigente.toString());
    }
}