// LOCAL PARA "RODAR" o servidor, ou melhor, criar a conexão com a página web
package com.fatec.livraria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication //Varre todo o pacote e procura classes para executar
public class LivrariaApplication { //Classe principal da minha aplicação. É o start do programa

	public static void main(String[] args) {
		SpringApplication.run(LivrariaApplication.class, args);
	}

}