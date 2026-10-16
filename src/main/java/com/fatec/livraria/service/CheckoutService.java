package com.fatec.livraria.service;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.Endereco;
import com.fatec.livraria.model.EntregaCheckout;
import com.fatec.livraria.model.FormularioEnderecoCheckout;
import com.fatec.livraria.model.ItemCarrinho;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class CheckoutService {

    private final ClienteService clienteService;
    private final CarrinhoService carrinhoService;
    private final FreteService freteService;
    private final UsuarioAtual usuarioAtual;

    private Long clienteEntrega;
    private Endereco enderecoEntrega;

    public CheckoutService(ClienteService clienteService,
                           CarrinhoService carrinhoService,
                           FreteService freteService,
                           UsuarioAtual usuarioAtual) {
        this.clienteService = clienteService;
        this.carrinhoService = carrinhoService;
        this.freteService = freteService;
        this.usuarioAtual = usuarioAtual;
    }

    public List<Endereco> listarEnderecos() {
        Cliente cliente = clienteService.exigirCliente(
            usuarioAtual.getClienteId()
        );

        return cliente.getEnderecos().stream()
            .filter(e -> "ENTREGA".equals(e.getTipo()))
            .toList();
    }

    // Apenas calcula: não salva endereço nem confirma a seleção.
    public synchronized EntregaCheckout cotar(
            FormularioEnderecoCheckout formulario) {
        Cliente cliente = clienteService.exigirCliente(
            usuarioAtual.getClienteId()
        );

        List<ItemCarrinho> itens = exigirItens();
        Endereco endereco = resolverEndereco(formulario, cliente);

        return montarResumo(endereco, itens);
    }

    public synchronized void confirmarEndereco(
            FormularioEnderecoCheckout formulario) {
        Long clienteId = usuarioAtual.getClienteId();

        Cliente cliente = clienteService.exigirCliente(clienteId);
        List<ItemCarrinho> itens = exigirItens();
        Endereco endereco = resolverEndereco(formulario, cliente);

        // Valida o cálculo antes de qualquer persistência.
        montarResumo(endereco, itens);

        if ("NOVO".equals(formulario.getOrigem())
                && formulario.isSalvarPerfil()) {
            // O método existente salva endereço e log
            // dentro da mesma transação.
            clienteService.adicionarEndereco(
                clienteId, endereco
            );
        }

        // Atualiza a sessão somente após o salvamento opcional.
        clienteEntrega = clienteId;
        enderecoEntrega = copiar(endereco);
    }

    // Recalcula usando o carrinho atual.
    public synchronized EntregaCheckout obterEntrega() {
        if (enderecoEntrega == null
                || !Objects.equals(
                    clienteEntrega,
                    usuarioAtual.getClienteId())) {
            return null;
        }

        clienteService.exigirCliente(clienteEntrega);

        return montarResumo(
            copiar(enderecoEntrega),
            exigirItens()
        );
    }

    public synchronized void limparEntrega() {
        clienteEntrega = null;
        enderecoEntrega = null;
    }

    private Endereco resolverEndereco(
            FormularioEnderecoCheckout formulario,
            Cliente cliente) {
        if (formulario == null) {
            throw erro("Selecione um endereço de entrega.");
        }

        Endereco endereco;

        if ("CADASTRADO".equals(formulario.getOrigem())) {
            endereco = cliente.getEnderecos().stream()
                .filter(e -> Objects.equals(
                    e.getId(),
                    formulario.getEnderecoId()
                ))
                .filter(e -> "ENTREGA".equals(e.getTipo()))
                .findFirst()
                .map(this::copiar)
                .orElseThrow(() -> erro(
                    "Endereço de entrega não encontrado no seu perfil."
                ));

        } else if ("NOVO".equals(formulario.getOrigem())) {
            if (formulario.getNovoEndereco() == null) {
                throw erro("Informe o novo endereço.");
            }

            endereco = copiar(formulario.getNovoEndereco());

            // O navegador não escolhe ID nem tipo do novo registro.
            endereco.setId(null);
            endereco.setTipo("ENTREGA");

        } else {
            throw erro(
                "Selecione um endereço cadastrado ou informe um novo."
            );
        }

        clienteService.validarEndereco(endereco);

        return endereco;
    }

    private List<ItemCarrinho> exigirItens() {
        List<ItemCarrinho> itens = carrinhoService.listar();

        if (itens.isEmpty()) {
            throw erro("Seu carrinho está vazio.");
        }

        return itens;
    }

    private EntregaCheckout montarResumo(
            Endereco endereco,
            List<ItemCarrinho> itens) {
        return new EntregaCheckout(
            copiar(endereco),
            carrinhoService.calcularTotal(itens),
            freteService.calcular(itens, endereco)
        );
    }

    private Endereco copiar(Endereco e) {
        Endereco copia = new Endereco(
            e.getNomeCurto(),
            e.getTipo(),
            e.getTipoResidencia(),
            e.getTipoLogradouro(),
            e.getLogradouro(),
            e.getNumero(),
            e.getBairro(),
            e.getCep(),
            e.getCidade(),
            e.getEstado(),
            e.getPais(),
            e.getObservacoes()
        );

        copia.setId(e.getId());

        return copia;
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }
}