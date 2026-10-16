package com.fatec.livraria.service;

import com.fatec.livraria.model.CartaoCredito;
import com.fatec.livraria.model.EntregaCheckout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PagamentoService {

    private static final BigDecimal MINIMO_CARTAO =
        new BigDecimal("10.00");

    private final ClienteService clientes;
    private final CheckoutService checkout;
    private final UsuarioAtual usuarioAtual;

    public PagamentoService(
            ClienteService clientes,
            CheckoutService checkout,
            UsuarioAtual usuarioAtual) {

        this.clientes = clientes;
        this.checkout = checkout;
        this.usuarioAtual = usuarioAtual;
    }

    public List<CartaoCredito> listarCartoes() {
        return clientes
            .exigirCliente(usuarioAtual.getClienteId())
            .getCartoes();
    }

    // Primeira regra: pagamento integral com cartões cadastrados.
    // A exceção da RN0035 será tratada junto aos cupons, no Service.
    // Validar não cobra, não cria pedido e não grava pagamento.
    public void validarCartoes(Map<Long, String> valoresCartoes) {
        EntregaCheckout entrega = checkout.obterEntrega();

        if (entrega == null) {
            throw new ValidacaoException(
                List.of("Selecione o endereço antes de continuar.")
            );
        }

        var permitidos = new HashSet<Long>();

        listarCartoes().forEach(
            cartao -> permitidos.add(cartao.getId())
        );

        var erros = new ArrayList<String>();
        BigDecimal soma = BigDecimal.ZERO;
        int selecionados = 0;

        if (valoresCartoes != null) {
            for (var entrada : valoresCartoes.entrySet()) {
                String texto = entrada.getValue();

                if (texto == null || texto.isBlank()) {
                    continue;
                }

                selecionados++;

                // Não confia no ID recebido pelo formulário.
                if (!permitidos.contains(entrada.getKey())) {
                    erros.add(
                        "Cartão não encontrado no seu perfil."
                    );
                    continue;
                }

                texto = texto.trim();

                if (!texto.matches("[0-9]+(?:[.,][0-9]{1,2})?")) {
                    erros.add(
                        "Informe um valor monetário válido, "
                        + "com até duas casas decimais."
                    );
                    continue;
                }

                BigDecimal valor = new BigDecimal(
                    texto.replace(',', '.')
                );

                if (valor.compareTo(MINIMO_CARTAO) < 0) {
                    erros.add(
                        "O valor mínimo por cartão é R$ 10,00."
                    );
                }

                soma = soma.add(valor);
            }
        }

        if (selecionados == 0) {
            erros.add(
                "Informe o valor de pelo menos um cartão."
            );
        }

        // Inclui o frete e usa o total recalculado no servidor.
        if (soma.compareTo(entrega.getTotal()) != 0) {
            erros.add(
                "A soma dos cartões deve ser igual ao total "
                + "da compra, incluindo o frete."
            );
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }
}