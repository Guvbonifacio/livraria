package com.fatec.livraria.controller;

import com.fatec.livraria.model.FormularioTroca;
import com.fatec.livraria.service.PedidoClienteService;
import com.fatec.livraria.service.SolicitacaoTrocaService;
import com.fatec.livraria.service.ValidacaoException;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PedidoController {

    private final PedidoClienteService pedidos;
    private final SolicitacaoTrocaService trocas;

    public PedidoController(
            PedidoClienteService pedidos,
            SolicitacaoTrocaService trocas) {

        this.pedidos = pedidos;
        this.trocas = trocas;
    }

    @GetMapping("/pedidos")
    public String pedidos(Model model) {
        model.addAttribute(
            "pedidos",
            pedidos.listar()
        );

        model.addAttribute(
            "notificacoes",
            pedidos.notificacoes()
        );

        return "pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String detalhesPedido(
            @PathVariable Long id,
            Model model) {

        model.addAttribute("pedido", pedidos.exigir(id));
        model.addAttribute("historico", pedidos.historico(id));
        model.addAttribute("trocas", trocas.listar(id));

        return "detalhes-pedido";
    }

    @GetMapping("/pedidos/{id}/troca")
    public String solicitarTroca(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirect) {

        try {
            model.addAttribute(
                "pedido",
                pedidos.exigirEntregue(id)
            );

            model.addAttribute(
                "itensSolicitados",
                trocas.itensSolicitados(id)
            );

            return "solicitar-troca";

        } catch (ValidacaoException e) {
            redirect.addFlashAttribute(
                "errosTroca",
                e.getErros()
            );

            return "redirect:/pedidos/" + id;
        }
    }

    @PostMapping("/pedidos/{id}/troca")
    public String registrarTroca(
            @PathVariable Long id,
            @ModelAttribute FormularioTroca formulario,
            BindingResult binding,
            RedirectAttributes redirect) {

        // Erros de conversão dos parâmetros recebidos pelo Spring.
        if (binding.hasErrors()) {
            redirect.addFlashAttribute(
                "errosTroca",
                List.of(
                    "Verifique os itens informados no formulário."
                )
            );

            return "redirect:/pedidos/" + id + "/troca";
        }

        try {
            Long trocaId = trocas.solicitar(id, formulario);

            redirect.addFlashAttribute(
                "sucessoTroca",
                "Solicitação de troca #" + trocaId
                    + " registrada com status EM TROCA."
            );

        } catch (ValidacaoException e) {
            redirect.addFlashAttribute(
                "errosTroca",
                e.getErros()
            );

        } catch (DataAccessException e) {
            redirect.addFlashAttribute(
                "errosTroca",
                List.of(
                    "Não foi possível registrar a troca. "
                    + "Nenhuma alteração foi confirmada."
                )
            );
        }

        return "redirect:/pedidos/" + id;
    }

    @GetMapping("/pedidos/{id}/troca/despacho")
    public String informarDespacho(
            @PathVariable Long id,
            Model model) {

        pedidos.exigir(id);

        model.addAttribute("pedidoId", id);

        return "informar-despacho";
    }
}