package com.fatec.livraria.service;

import com.fatec.livraria.model.*;
import com.fatec.livraria.model.PlanoPagamento.Parcela;
import com.fatec.livraria.repository.PedidoRepository;
import com.fatec.livraria.repository.PedidoRepository.CupomDisponivel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PagamentoService {

    // RN0034 - mensagem unica: a regra e uma so, a redacao tambem.
    private static final String MINIMO_POR_CARTAO =
        "O valor mínimo por cartão é R$ 10,00.";

    private final ClienteService clientes;
    private final CheckoutService checkout;
    private final UsuarioAtual usuario;
    private final PedidoRepository pedidos;

    public PagamentoService(
            ClienteService clientes,
            CheckoutService checkout,
            UsuarioAtual usuario,
            PedidoRepository pedidos) {

        this.clientes = clientes;
        this.checkout = checkout;
        this.usuario = usuario;
        this.pedidos = pedidos;
    }

    public List<CartaoCredito> listarCartoes() {
        return clientes
            .exigirCliente(usuario.getClienteId())
            .getCartoes();
    }

    public List<CupomDisponivel> listarCupons() {
        return pedidos.listarCupons(usuario.getClienteId())
            .stream()
            .map(pedidos::buscarCupom)
            .filter(c -> c != null
                && !c.validade().isBefore(LocalDate.now()))
            .toList();
    }

    // Mantém compatibilidade com a validação anterior.
    public void validarCartoes(Map<Long, String> valores) {
        var formulario = new FormularioPagamento();
        formulario.setValoresCartoes(valores);

        var entrega = checkout.obterEntrega();

        if (entrega == null) {
            throw erro(
                "Selecione o endereço antes de continuar."
            );
        }

        preparar(formulario, entrega.getTotal());
    }

    // Não grava pagamentos, não consome cupons e não emite crédito. O total deve ser calculado pelo Service.
    public PlanoPagamento preparar(
            FormularioPagamento formulario,
            BigDecimal total) {

        if (formulario == null
                || total == null
                || total.signum() <= 0) {
            throw erro("Dados de pagamento inválidos.");
        }

        var parcelas = new ArrayList<Parcela>();
        var ids = new HashSet<Long>();

        BigDecimal somaCupons = BigDecimal.ZERO;
        BigDecimal menorCupom = null;
        int promocionais = 0;

        var selecionados = formulario.getCuponsIds();

        if (selecionados != null) {
            for (Long id : selecionados) {
                if (id == null || !ids.add(id)) {
                    throw erro("Cupom repetido ou inválido.");
                }

                var cupom = pedidos.buscarCupom(id);

                if (cupom == null
                        || !cupom.ativo()
                        || cupom.validade().isBefore(LocalDate.now())
                        || (
                            cupom.clienteId() != null
                            && !cupom.clienteId().equals(
                                usuario.getClienteId()
                            )
                        )) {

                    throw erro(
                        "Cupom inválido, vencido ou "
                        + "indisponível para este cliente."
                    );
                }

                if ("PROMOCIONAL".equals(cupom.tipo())
                        && ++promocionais > 1) {
                    throw erro(
                        "Utilize no máximo um cupom "
                        + "promocional por pedido."
                    );
                }

                somaCupons = somaCupons.add(cupom.valor());

                menorCupom = menorCupom == null
                    ? cupom.valor()
                    : menorCupom.min(cupom.valor());

                parcelas.add(new Parcela(
                    "CUPOM_" + cupom.tipo(),
                    cupom.valor(),
                    null,
                    null,
                    null,
                    id
                ));
            }
        }

        // RN0036: se retirar algum cupom ainda cobre o total, existe cupom desnecessário na seleção.
        if (menorCupom != null
                && somaCupons.subtract(menorCupom)
                    .compareTo(total) >= 0) {

            throw erro(
                "Remova o cupom desnecessário: "
                + "os demais já cobrem a compra."
            );
        }

        BigDecimal restante = total
            .subtract(somaCupons)
            .max(BigDecimal.ZERO);

        var permitidos = new HashMap<Long, CartaoCredito>();

        listarCartoes().forEach(
            cartao -> permitidos.put(cartao.getId(), cartao)
        );

        var cartoes = new ArrayList<Parcela>();
        BigDecimal somaCartoes = BigDecimal.ZERO;

        if (formulario.getValoresCartoes() != null) {
            for (var entrada :
                    formulario.getValoresCartoes().entrySet()) {

                String texto = entrada.getValue();

                if (texto == null || texto.isBlank()) {
                    continue;
                }

                var cartao = permitidos.get(entrada.getKey());

                if (cartao == null) {
                    throw erro(
                        "Cartão não encontrado no seu perfil."
                    );
                }

                texto = texto.trim();

                if (!texto.matches("[0-9]+(?:[.,][0-9]{1,2})?")) {
                    throw erro(
                        "Informe um valor monetário válido, "
                        + "com até duas casas decimais."
                    );
                }

                var valor = new BigDecimal(
                    texto.replace(',', '.')
                );

                if (valor.signum() <= 0) {
                    throw erro(MINIMO_POR_CARTAO);
                }

                somaCartoes = somaCartoes.add(valor);

                cartoes.add(new Parcela(
                    "CARTAO",
                    valor,
                    cartao.getId(),
                    cartao.getNumero(),
                    cartao.getBandeira(),
                    null
                ));
            }
        }

        // RN0035é a excessão que admite um cartão com o saldo inferior a R$ 10
        // somente depois do uso integral dos cupons selecionados.
        boolean excecao =
            somaCupons.signum() > 0
            && restante.signum() > 0
            && restante.compareTo(new BigDecimal("10.00")) < 0
            && cartoes.size() == 1;

        for (var cartao : cartoes) {
            if (cartao.valor()
                    .compareTo(new BigDecimal("10.00")) < 0
                    && !excecao) {

                throw erro(MINIMO_POR_CARTAO);
            }
        }

        if (somaCartoes.compareTo(restante) != 0
                || parcelas.isEmpty() && cartoes.isEmpty()) {

            throw erro(
                "A soma dos cartões deve cobrir exatamente "
                + "o restante da compra, incluindo o frete."
            );
        }

        parcelas.addAll(cartoes);

        return new PlanoPagamento(
            parcelas,
            somaCupons.subtract(total).max(BigDecimal.ZERO)
        );
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }
}