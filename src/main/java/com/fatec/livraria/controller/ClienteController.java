package com.fatec.livraria.controller;

import com.fatec.livraria.model.CartaoCredito;
import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.Endereco;
import com.fatec.livraria.service.ClienteService;
import com.fatec.livraria.service.ValidacaoException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping("/novo")
    public String novoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cliente-form";
    }

    @PostMapping("/salvar")
    public String salvarCliente(@ModelAttribute Cliente cliente,
                                @RequestParam String confirmacaoSenha,
                                Model model) {
        try {
            Long id = service.cadastrar(cliente, confirmacaoSenha);
            return "redirect:/clientes/" + id;
        } catch (ValidacaoException e) {
            model.addAttribute("erros", e.getErros());
            model.addAttribute("cliente", cliente);
            return "cliente-form";
        }
    }

    @GetMapping("/{id}")
    public String detalharCliente(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", service.buscarPorId(id));
        prepararRelacionamentos(model);
        return "cliente-perfil";
    }

    @GetMapping("/{id}/editar")
    public String editarCliente(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", service.buscarPorId(id));
        return "cliente-editar";
    }

    @PostMapping("/{id}/atualizar")
    public String atualizarCliente(@PathVariable Long id,
                                   @ModelAttribute Cliente cliente,
                                   Model model) {
        cliente.setId(id);
        try {
            service.alterar(cliente);
            return "redirect:/clientes/" + id;
        } catch (ValidacaoException e) {
            model.addAttribute("erros", e.getErros());
            return "cliente-editar";
        }
    }

    @PostMapping("/{id}/senha")
    public String alterarSenha(@PathVariable Long id,
                               @RequestParam String senhaAtual,
                               @RequestParam String novaSenha,
                               @RequestParam String confirmacao,
                               Model model,
                               RedirectAttributes redirect) {
        try {
            service.alterarSenha(id, senhaAtual, novaSenha, confirmacao);
            redirect.addFlashAttribute("sucessoSenha", "Senha alterada com sucesso.");
            return "redirect:/clientes/" + id;
        } catch (ValidacaoException e) {
            model.addAttribute("cliente", service.buscarPorId(id));
            model.addAttribute("errosSenha", e.getErros());
            prepararRelacionamentos(model);
            return "cliente-perfil";
        }
    }

    @PostMapping("/{id}/enderecos")
    public String adicionarEndereco(@PathVariable Long id,
                                    @ModelAttribute Endereco endereco,
                                    RedirectAttributes redirect) {
        return executar(id, redirect, () -> service.adicionarEndereco(id, endereco), "Endereço adicionado com sucesso.");
    }

    @PostMapping("/{id}/enderecos/{idEnd}/alterar")
    public String alterarEndereco(@PathVariable Long id,
                                  @PathVariable Long idEnd,
                                  @ModelAttribute Endereco endereco,
                                  RedirectAttributes redirect) {
        return executar(id, redirect, () -> service.alterarEndereco(id, idEnd, endereco), "Endereço alterado com sucesso.");
    }

    @PostMapping("/{id}/enderecos/{idEnd}/remover")
    public String removerEndereco(@PathVariable Long id,
                                  @PathVariable Long idEnd,
                                  RedirectAttributes redirect) {
        return executar(id, redirect, () -> service.removerEndereco(id, idEnd), "Endereço removido com sucesso.");
    }

    @PostMapping("/{id}/cartoes")
    public String adicionarCartao(@PathVariable Long id,
                                 @ModelAttribute CartaoCredito cartao,
                                 RedirectAttributes redirect) {
        return executar(id, redirect, () -> service.adicionarCartao(id, cartao), "Cartão adicionado com sucesso.");
    }

    @PostMapping("/{id}/cartoes/{idCartao}/alterar")
    public String alterarCartao(@PathVariable Long id,
                                @PathVariable Long idCartao,
                                @ModelAttribute CartaoCredito cartao,
                                RedirectAttributes redirect) {
        return executar(id, redirect, () -> service.alterarCartao(id, idCartao, cartao), "Cartão alterado com sucesso.");
    }

    @PostMapping("/{id}/cartoes/{idCartao}/preferencial")
    public String definirPreferencial(@PathVariable Long id,
                                      @PathVariable Long idCartao,
                                      RedirectAttributes redirect) {
        return executar(id, redirect, () -> service.definirCartaoPreferencial(id, idCartao), "Cartão preferencial atualizado.");
    }

    @PostMapping("/{id}/cartoes/{idCartao}/remover")
    public String removerCartao(@PathVariable Long id,
                                @PathVariable Long idCartao,
                                RedirectAttributes redirect) {
        return executar(id, redirect, () -> service.removerCartao(id, idCartao), "Cartão removido com sucesso.");
    }

    private String executar(Long id, RedirectAttributes redirect, Runnable operacao, String sucesso) {
        try {
            operacao.run();
            redirect.addFlashAttribute("sucessoRelacionamentos", sucesso);
        } catch (ValidacaoException e) {
            redirect.addFlashAttribute("errosRelacionamentos", e.getErros());
        }
        return "redirect:/clientes/" + id;
    }

    private void prepararRelacionamentos(Model model) {
        model.addAttribute("novoEndereco", new Endereco());
        model.addAttribute("novoCartao", new CartaoCredito());
    }
}