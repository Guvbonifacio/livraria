package com.fatec.livraria;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "livraria.carrinho.agendamento-ativo=false"
})
class LivrariaApplicationTests {

    @Test
    void contextLoads() {
        // Verifica a inicialização sem acessar os bancos em arquivo.
    }
}