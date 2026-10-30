package com.fatec.livraria.service;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RelogioReservaConfig {

    @Bean
    public Clock relogioReserva() {
        return Clock.systemUTC();
    }
}