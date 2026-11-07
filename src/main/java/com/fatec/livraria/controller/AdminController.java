package com.fatec.livraria.controller;

import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.service.AcessoAdministrativoException;
import com.fatec.livraria.service.AdministracaoService;
import com.fatec.livraria.service.AutorizacaoTrocaService;
import com.fatec.livraria.service.ClienteService;
import com.fatec.livraria.service.ConclusaoTrocaService;
import com.fatec.livraria.service.EntregaPedidoService;
import com.fatec.livraria.service.ProcessamentoPedidoService;
import com.fatec.livraria.service.ValidacaoException;
import com.fatec.livraria.service.AnaliseVendasService;

import java.util.List;
import java.util.Map;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ClienteService clienteService;
    private final AdministracaoService administracao;
    private final ProcessamentoPedidoService processamento;
    private final EntregaPedidoService entrega;
    private final AutorizacaoTrocaService autorizacaoTroca;
    private final ConclusaoTrocaService conclusaoTroca;
    private final AnaliseVendasService analiseVendas;

    public AdminController(
            ClienteService clienteService,
            AdministracaoService administracao,
            ProcessamentoPedidoService processamento,
            EntregaPedidoService entrega,
            AutorizacaoTrocaService autorizacaoTroca,
            ConclusaoTrocaService conclusaoTroca,
            AnaliseVendasService analiseVendas) {

        this.clienteService = clienteService;
        this.administracao = administracao;
        this.processamento = processamento;
        this.entrega = entrega;
        this.autorizacaoTroca = autorizacaoTroca;
        this.conclusaoTroca = conclusaoTroca;
        this.analiseVendas = analiseVendas;
    }

    // Painel principal
    @GetMapping
    public String admin() {
        administracao.exigirAcesso();

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

        Cliente cliente = administracao.buscarCliente(id);

        model.addAttribute("cliente", cliente);

        model.addAttribute(
            "logs",
            administracao.historicoCliente(id)
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
        } catch (AcessoAdministrativoException e) {
            throw e;

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

    // Processa pagamento, despacho e confirmação de entrega.
    @PostMapping("/pedidos/{id}/status")
    public String alterarStatusPedido(
            @PathVariable Long id,
            @RequestParam String novoStatus,
            RedirectAttributes redirect) {

        try {
            String status;

            if ("EM TRANSPORTE".equals(novoStatus)
                    || "ENTREGUE".equals(novoStatus)) {

                status = entrega.transicionar(id, novoStatus);

            } else {
                status = processamento.processar(id, novoStatus);
            }

            redirect.addFlashAttribute(
                "sucessoProcessamento",
                "Status do pedido: " + status + "."
            );

        } catch (AcessoAdministrativoException e) {
            throw e;

        } catch (ValidacaoException e) {
            redirect.addFlashAttribute(
                "errosProcessamento",
                e.getErros()
            );

        } catch (DataAccessException e) {
            redirect.addFlashAttribute(
                "errosProcessamento",
                List.of(
                    "Não foi possível atualizar o pedido. "
                    + "Nenhuma alteração foi confirmada."
                )
            );
        }

        return "redirect:/admin/pedidos/" + id;
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

    @PostMapping("/trocas/{id}/analisar")
    public String analisarTroca(
            @PathVariable Long id,
            @RequestParam String decisao,
            RedirectAttributes redirect) {

        try {
            autorizacaoTroca.autorizar(id, decisao);

            redirect.addFlashAttribute(
                "sucessoAutorizacao",
                "Troca autorizada. A notificação "
                + "foi disponibilizada ao cliente."
            );

        } catch (AcessoAdministrativoException e) {
            throw e;

        } catch (ValidacaoException e) {
            redirect.addFlashAttribute(
                "errosAutorizacao",
                e.getErros()
            );

        } catch (DataAccessException e) {
            redirect.addFlashAttribute(
                "errosAutorizacao",
                List.of(
                    "Não foi possível autorizar a troca. "
                    + "Nenhuma alteração foi confirmada."
                )
            );
        }

        return "redirect:/admin/trocas/" + id;
    }

    // Recebimento e decisão sobre retorno ao estoque
    @PostMapping("/trocas/{id}/receber")
    public String receberItemTroca(
            @PathVariable Long id,
            @RequestParam Boolean retornaEstoque,
            RedirectAttributes redirect) {

        try {
            conclusaoTroca.receber(
                id,
                Boolean.TRUE.equals(retornaEstoque)
            );

            redirect.addFlashAttribute(
                "sucessoConclusao",
                "Recebimento confirmado e cupom emitido."
            );

        } catch (AcessoAdministrativoException e) {
            throw e;

        } catch (ValidacaoException e) {
            redirect.addFlashAttribute(
                "errosConclusao",
                e.getErros()
            );

        } catch (DataAccessException e) {
            redirect.addFlashAttribute(
                "errosConclusao",
                List.of(
                    "Não foi possível receber a troca. "
                    + "Nenhuma alteração foi confirmada."
                )
            );
        }

        return "redirect:/admin/trocas/" + id;
    }

    @PostMapping("/trocas/{id}/finalizar")
    public String finalizarTroca(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        try {
            conclusaoTroca.finalizar(id);

            redirect.addFlashAttribute(
                "sucessoConclusao",
                "Cupom de troca emitido."
            );

        } catch (AcessoAdministrativoException e) {
            throw e;

        } catch (ValidacaoException e) {
            redirect.addFlashAttribute(
                "errosConclusao",
                e.getErros()
            );

        } catch (DataAccessException e) {
            redirect.addFlashAttribute(
                "errosConclusao",
                List.of(
                    "Não foi possível emitir o cupom. "
                    + "Nenhuma alteração foi confirmada."
                )
            );
        }

        return "redirect:/admin/trocas/" + id;
    }

    // As séries serão implementadas na etapa de análise.
    @GetMapping("/analise")
    public String analise(
            Model model,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(required = false) List<String> categorias,
            @RequestParam(defaultValue = "false") boolean consultar) {

        // A consulta exige ADMIN no Service, inclusive na abertura inicial.
        var disponiveis = administracao.listarCategorias();

        List<String> selecionadas = categorias != null
            ? categorias
            : consultar ? List.of() : disponiveis;

        model.addAttribute("categoriasDisponiveis", disponiveis);
        model.addAttribute("categoriasSelecionadas", selecionadas);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);

        model.addAttribute("etiquetas", List.of());
        model.addAttribute("series", List.of());
        model.addAttribute("errosAnalise", List.of());
        model.addAttribute("consultaRealizada", false);
        model.addAttribute("temVendas", false);

        boolean solicitada = consultar
            || dataInicio != null
            || dataFim != null
            || categorias != null;

        if (!solicitada) {
            return "admin-analise";
        }

        try {
            var resultado = analiseVendas.analisar(
                dataInicio,
                dataFim,
                selecionadas
            );

            model.addAttribute(
                "etiquetas",
                resultado.etiquetas()
            );

            // Mapas simples para a serialização JavaScript do Thymeleaf.
            model.addAttribute(
                "series",
                resultado.series().stream()
                    .map(serie -> Map.of(
                        "categoria", serie.categoria(),
                        "valores", serie.valores()
                    ))
                    .toList()
            );

            model.addAttribute("consultaRealizada", true);

            model.addAttribute(
                "temVendas",
                resultado.series().stream()
                    .anyMatch(serie ->
                        serie.valores().stream()
                            .anyMatch(valor -> valor.signum() > 0)
                    )
            );

        } catch (AcessoAdministrativoException e) {
            throw e;

        } catch (ValidacaoException e) {
            model.addAttribute(
                "errosAnalise",
                e.getErros()
            );
        }

        return "admin-analise";
    }

    @GetMapping("/analise/exportar")
    public ResponseEntity<byte[]> exportarAnalise(
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(required = false) List<String> categorias) {

        AnaliseVendasService.Resultado resultado;

        try {
            // Mesma autorização, validação e cálculo usados pelo gráfico.
            resultado = analiseVendas.analisar(
                dataInicio,
                dataFim,
                categorias
            );

        } catch (AcessoAdministrativoException e) {
            throw e;

        } catch (ValidacaoException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                e
            );
        }

        StringBuilder csv = new StringBuilder(
            "\uFEFFperíodo;categoria;valor de venda\r\n"
        );

        for (int mes = 0; mes < resultado.etiquetas().size(); mes++) {
            for (var serie : resultado.series()) {
                csv.append(
                        campoCsv(resultado.etiquetas().get(mes))
                    )
                    .append(';')
                    .append(campoCsv(serie.categoria()))
                    .append(';')
                    .append(
                        serie.valores().get(mes)
                            .setScale(2)
                            .toPlainString()
                            .replace('.', ',')
                    )
                    .append("\r\n");
            }
        }

        return ResponseEntity.ok()
            .contentType(
                new MediaType(
                    "text",
                    "csv",
                    StandardCharsets.UTF_8
                )
            )
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"analise-vendas.csv\""
            )
            .body(
                csv.toString().getBytes(StandardCharsets.UTF_8)
            );
    }

    private String campoCsv(String valor) {
        if (valor.contains(";")
                || valor.contains("\"")
                || valor.contains("\r")
                || valor.contains("\n")) {

            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }

        return valor;
    }
}