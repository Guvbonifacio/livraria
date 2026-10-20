package com.fatec.livraria.controller;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.repository.LogRepository;
import com.fatec.livraria.service.AdministracaoService;
import com.fatec.livraria.service.ClienteService;
import com.fatec.livraria.service.ValidacaoException;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ClienteService clienteService;
    private final LogRepository logRepository;
    private final AdministracaoService administracao;

    public AdminController(
            ClienteService clienteService,
            LogRepository logRepository,
            AdministracaoService administracao) {

        this.clienteService = clienteService;
        this.logRepository = logRepository;
        this.administracao = administracao;
    }

    // Painel principal
    @GetMapping
    public String admin() {
        return "admin";
    }

    // Consulta de clientes
    @GetMapping("/clientes")
    public String listarClientes(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String email,
            Model model) {

        List<Cliente> clientes = clienteService.consultar(
            nome,
            cpf,
            email
        );

        model.addAttribute("clientes", clientes);

        return "admin-clientes";
    }

    // Detalhes do cliente e histórico de alterações
    @GetMapping("/clientes/{id}")
    public String detalheCliente(
            @PathVariable Long id,
            Model model) {

        Cliente cliente = clienteService.exigirCliente(id);

        model.addAttribute("cliente", cliente);

        model.addAttribute(
            "logs",
            logRepository.buscarPorRegistro("cliente", id)
        );

        return "admin-cliente-detalhe";
    }

    // Inativação de cliente
    @PostMapping("/clientes/{id}/inativar")
    public String inativarCliente(@PathVariable Long id) {
        clienteService.inativar(id);

        return "redirect:/admin/clientes/" + id;
    }

    // Consulta de pedidos por status e período
    @GetMapping("/pedidos")
    public String listarPedidos(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dataInicial,
            @RequestParam(required = false) String dataFinal,
            Model model) {

        try {
            model.addAttribute(
                "pedidos",
                administracao.listarPedidos(
                    status,
                    dataInicial,
                    dataFinal
                )
            );
        } catch (ValidacaoException e) {
            model.addAttribute("pedidos", List.of());
            model.addAttribute(
                "errosConsulta",
                e.getErros()
            );
        }

        return "admin-pedidos";
    }

    // Detalhes reais do pedido
    @GetMapping("/pedidos/{id}")
    public String detalhePedidoAdmin(
            @PathVariable Long id,
            Model model) {

        var pedido = administracao.buscarPedido(id);

        if (pedido == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pedido não encontrado."
            );
        }

        model.addAttribute("pedido", pedido);

        return "admin-pedido-detalhe";
    }

    // A transição de status será implementada na próxima etapa.
    @PostMapping("/pedidos/{id}/status")
    public String alterarStatusPedido(
            @PathVariable Long id,
            @RequestParam String novoStatus) {

        throw new ResponseStatusException(
            HttpStatus.NOT_IMPLEMENTED,
            "Operação prevista para a próxima etapa."
        );
    }

    // Consulta das solicitações reais de troca
    @GetMapping("/trocas")
    public String listarTrocas(Model model) {
        model.addAttribute(
            "trocas",
            administracao.listarTrocas()
        );

        return "admin-trocas";
    }

    // Detalhes reais da troca
    @GetMapping("/trocas/{id}")
    public String detalheTroca(
            @PathVariable Long id,
            Model model) {

        var troca = administracao.buscarTroca(id);

        if (troca == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Troca não encontrada."
            );
        }

        model.addAttribute("troca", troca);

        return "admin-troca-detalhe";
    }

    // A autorização será implementada na próxima etapa.
    // O DRS não define um estado de troca negada.
    @PostMapping("/trocas/{id}/analisar")
    public String analisarTroca(
            @PathVariable Long id,
            @RequestParam String decisao) {

        throw new ResponseStatusException(
            HttpStatus.NOT_IMPLEMENTED,
            "Operação prevista para a próxima etapa."
        );
    }

    // Recebimento e decisão sobre retorno ao estoque
    @PostMapping("/trocas/{id}/receber")
    public String receberItemTroca(
            @PathVariable Long id,
            @RequestParam Boolean retornaEstoque) {

        throw new ResponseStatusException(
            HttpStatus.NOT_IMPLEMENTED,
            "Operação prevista para a próxima etapa."
        );
    }

    // Conclusão da troca e emissão de cupom
    @PostMapping("/trocas/{id}/finalizar")
    public String finalizarTroca(
            @PathVariable Long id) {

        throw new ResponseStatusException(
            HttpStatus.NOT_IMPLEMENTED,
            "Operação prevista para a próxima etapa."
        );
    }

    // As séries serão implementadas na etapa de análise.
    @GetMapping("/analise")
    public String analise(
            Model model,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(required = false)
            List<String> categorias) {

        model.addAttribute("etiquetas", List.of());
        model.addAttribute("series", List.of());

        model.addAttribute(
            "categoriasDisponiveis",
            administracao.listarCategorias()
        );

        return "admin-analise";
    }
}