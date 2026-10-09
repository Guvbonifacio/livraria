package com.fatec.livraria.service;

import com.fatec.livraria.model.CartaoCredito;
import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.model.Endereco;
import com.fatec.livraria.model.LogTransacao;
import com.fatec.livraria.repository.ClienteRepository;
import com.fatec.livraria.repository.LogRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final LogRepository logRepository;
    private final UsuarioAtual usuarioAtual;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public ClienteService(ClienteRepository repository,
                          LogRepository logRepository,
                          UsuarioAtual usuarioAtual,
                          ObjectMapper objectMapper) {
        this.repository = repository;
        this.logRepository = logRepository;
        this.usuarioAtual = usuarioAtual;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long cadastrar(Cliente cliente, String confirmacaoSenha) {
        if (cliente.getCpf() != null) {
            cliente.setCpf(cliente.getCpf().replaceAll("\\D", ""));
        }
        if (cliente.getEmail() != null) {
            cliente.setEmail(cliente.getEmail().trim().toLowerCase());
        }

        List<String> erros = new ArrayList<>();

        if (vazio(cliente.getNome())) erros.add("Nome é obrigatório");
        if (vazio(cliente.getCpf())) erros.add("CPF é obrigatório");
        if (vazio(cliente.getEmail())) erros.add("E-mail é obrigatório");
        if (vazio(cliente.getTelefone())) erros.add("Telefone é obrigatório");
        if (cliente.getDataNascimento() == null) erros.add("Data de nascimento é obrigatória");
        if (vazio(cliente.getGenero())) erros.add("Gênero é obrigatório");

        if (!vazio(cliente.getCpf())) {
            if (!cpfValido(cliente.getCpf())) {
                erros.add("CPF inválido");
            } else if (repository.existePorCpf(cliente.getCpf())) {
                erros.add("CPF já cadastrado");
            }
        }

        if (!vazio(cliente.getEmail()) && repository.existePorEmail(cliente.getEmail())) {
            erros.add("E-mail já cadastrado");
        }

        if (!senhaForte(cliente.getSenha())) {
            erros.add("A senha deve ter no mínimo 8 caracteres, com letras maiúsculas, minúsculas e caractere especial");
        }

        if (cliente.getSenha() == null || !cliente.getSenha().equals(confirmacaoSenha)) {
            erros.add("A confirmação não confere com a senha");
        }

        if (cliente.getEnderecos() == null || cliente.getEnderecos().isEmpty()) {
            erros.add("Informe ao menos um endereço");
        }

        if (cliente.getCartoes() == null || cliente.getCartoes().isEmpty()) {
            erros.add("Informe ao menos um cartão");
        } else {
            validarCartoesPreferenciais(cliente.getCartoes(), erros);
        }

        rejeitarErros(erros);

        cliente.setSenha(encoder.encode(cliente.getSenha()));
        garantirUmPreferencial(cliente.getCartoes());
        cliente.getCartoes().forEach(cartao -> cartao.setNumero(mascarar(cartao.getNumero())));

        Long id = repository.salvar(cliente);
        cliente.setId(id);

        registrarLog("INSERT", "cliente", id, dadosCliente(cliente, true));

        cliente.getEnderecos().forEach(endereco -> {
            Long enderecoId = repository.salvarEndereco(id, endereco);
            endereco.setId(enderecoId);
            registrarLog("INSERT", "endereco", enderecoId, dadosEndereco(id, endereco));
        });

        cliente.getCartoes().forEach(cartao -> {
            Long cartaoId = repository.salvarCartao(id, cartao);
            cartao.setId(cartaoId);
            registrarLog("INSERT", "cartao_credito", cartaoId, dadosCartao(id, cartao));
        });

        return id;
    }

    public List<Cliente> consultar(String nome, String cpf, String email) {
        return repository.consultar(nome, cpf, email);
    }

    public Cliente buscarPorId(Long id) {
        return repository.buscarPorId(id);
    }

    @Transactional
    public void alterar(Cliente cliente) {
        if (cliente.getCpf() != null) {
            cliente.setCpf(cliente.getCpf().replaceAll("\\D", ""));
        }
        if (cliente.getEmail() != null) {
            cliente.setEmail(cliente.getEmail().trim().toLowerCase());
        }

        List<String> erros = new ArrayList<>();

        if (vazio(cliente.getNome())) erros.add("Nome é obrigatório");
        if (vazio(cliente.getCpf())) erros.add("CPF é obrigatório");
        if (vazio(cliente.getEmail())) erros.add("E-mail é obrigatório");
        if (vazio(cliente.getTelefone())) erros.add("Telefone é obrigatório");
        if (cliente.getDataNascimento() == null) erros.add("Data de nascimento é obrigatória");
        if (vazio(cliente.getGenero())) erros.add("Gênero é obrigatório");

        if (!vazio(cliente.getCpf())) {
            if (!cpfValido(cliente.getCpf())) {
                erros.add("CPF inválido");
            } else if (repository.existePorCpf(cliente.getCpf(), cliente.getId())) {
                erros.add("CPF já cadastrado");
            }
        }

        if (!vazio(cliente.getEmail()) && repository.existePorEmail(cliente.getEmail(), cliente.getId())) {
            erros.add("E-mail já cadastrado");
        }

        rejeitarErros(erros);

        Cliente anterior = repository.buscarPorId(cliente.getId());
        repository.atualizar(cliente);

        registrarLog(
            "UPDATE",
            "cliente",
            cliente.getId(),
            dados("antes", dadosCliente(anterior, false), "depois", dadosCliente(cliente, false))
        );
    }

    @Transactional
    public void inativar(Long id) {
        Cliente anterior = repository.buscarPorId(id);
        repository.inativar(id);

        registrarLog(
            "UPDATE",
            "cliente",
            id,
            dados("antes", dados("ativo", anterior.isAtivo()), "depois", dados("ativo", false))
        );
    }

    @Transactional
    public void alterarSenha(Long id, String senhaAtual, String novaSenha, String confirmacao) {
        Cliente cliente = repository.buscarPorId(id);
        List<String> erros = new ArrayList<>();

        if (senhaAtual == null || !encoder.matches(senhaAtual, cliente.getSenha())) {
            erros.add("Senha atual incorreta.");
        }

        if (!senhaForte(novaSenha)) {
            erros.add("A nova senha deve ter no mínimo 8 caracteres, com letras maiúsculas, minúsculas e caractere especial.");
        }

        if (novaSenha == null || !novaSenha.equals(confirmacao)) {
            erros.add("A confirmação não confere com a nova senha.");
        }

        rejeitarErros(erros);

        repository.atualizarSenha(id, encoder.encode(novaSenha));

        logRepository.registrar(new LogTransacao(
            null,
            LocalDateTime.now(),
            usuarioAtual.getIdentificador(),
            "UPDATE",
            "cliente",
            id,
            "senha alterada"
        ));
    }

    @Transactional
    public Long adicionarEndereco(Long clienteId, Endereco endereco) {
        repository.buscarPorId(clienteId);
        validarEndereco(endereco);

        Long id = repository.salvarEndereco(clienteId, endereco);
        endereco.setId(id);

        registrarLog("INSERT", "endereco", id, dadosEndereco(clienteId, endereco));
        return id;
    }

    @Transactional
    public void alterarEndereco(Long clienteId, Long enderecoId, Endereco endereco) {
        Endereco anterior = exigirEndereco(clienteId, enderecoId);

        validarEndereco(endereco);
        endereco.setId(enderecoId);

        repository.atualizarEndereco(clienteId, endereco);

        registrarLog(
            "UPDATE",
            "endereco",
            enderecoId,
            dados("antes", dadosEndereco(clienteId, anterior), "depois", dadosEndereco(clienteId, endereco))
        );
    }

    @Transactional
    public void removerEndereco(Long clienteId, Long enderecoId) {
        Endereco anterior = exigirEndereco(clienteId, enderecoId);

        if (repository.buscarEnderecos(clienteId).size() <= 1) {
            throw erro("Não é permitido remover o último endereço.");
        }

        repository.excluirEndereco(clienteId, enderecoId);
        registrarLog("DELETE", "endereco", enderecoId, dadosEndereco(clienteId, anterior));
    }

    @Transactional
    public Long adicionarCartao(Long clienteId, CartaoCredito cartao) {
        repository.buscarPorId(clienteId);
        validarCartao(cartao);

        List<CartaoCredito> anteriores = repository.buscarCartoes(clienteId);
        boolean escolher = cartao.isPreferencial() || anteriores.stream().noneMatch(c -> c.isPreferencial());

        cartao.setPreferencial(false);
        cartao.setNumero(mascarar(cartao.getNumero()));

        Long id = repository.salvarCartao(clienteId, cartao);
        cartao.setId(id);

        registrarLog("INSERT", "cartao_credito", id, dadosCartao(clienteId, cartao));

        if (escolher) {
            atualizarPreferenciaComLog(clienteId, id, repository.buscarCartoes(clienteId));
        }

        return id;
    }

    @Transactional
    public void alterarCartao(Long clienteId, Long cartaoId, CartaoCredito cartao) {
        CartaoCredito anterior = exigirCartao(clienteId, cartaoId);

        validarCartao(cartao);

        boolean escolher = cartao.isPreferencial();
        cartao.setId(cartaoId);
        cartao.setNumero(mascarar(cartao.getNumero()));
        cartao.setPreferencial(anterior.isPreferencial());

        repository.atualizarCartao(clienteId, cartao);

        registrarLog(
            "UPDATE",
            "cartao_credito",
            cartaoId,
            dados("antes", dadosCartao(clienteId, anterior), "depois", dadosCartao(clienteId, cartao))
        );

        if (escolher && !anterior.isPreferencial()) {
            atualizarPreferenciaComLog(clienteId, cartaoId, repository.buscarCartoes(clienteId));
        }
    }

    @Transactional
    public void removerCartao(Long clienteId, Long cartaoId) {
        List<CartaoCredito> cartoes = repository.buscarCartoes(clienteId);
        CartaoCredito anterior = encontrarCartao(cartoes, cartaoId);

        if (cartoes.size() <= 1) {
            throw erro("Não é permitido remover o último cartão.");
        }

        repository.excluirCartao(clienteId, cartaoId);
        registrarLog("DELETE", "cartao_credito", cartaoId, dadosCartao(clienteId, anterior));

        if (anterior.isPreferencial()) {
            List<CartaoCredito> restantes = repository.buscarCartoes(clienteId);
            atualizarPreferenciaComLog(clienteId, restantes.get(0).getId(), restantes);
        }
    }

    @Transactional
    public void definirCartaoPreferencial(Long clienteId, Long cartaoId) {
        List<CartaoCredito> cartoes = repository.buscarCartoes(clienteId);
        CartaoCredito escolhido = encontrarCartao(cartoes, cartaoId);

        if (!escolhido.isPreferencial()) {
            atualizarPreferenciaComLog(clienteId, cartaoId, cartoes);
        }
    }

    private void atualizarPreferenciaComLog(Long clienteId, Long cartaoId, List<CartaoCredito> anteriores) {
        encontrarCartao(anteriores, cartaoId);
        repository.definirPreferencial(clienteId, cartaoId);

        for (CartaoCredito cartao : anteriores) {
            boolean novaPreferencia = cartao.getId().equals(cartaoId);

            if (cartao.isPreferencial() != novaPreferencia) {
                registrarLog(
                    "UPDATE",
                    "cartao_credito",
                    cartao.getId(),
                    dados(
                        "cliente_id", clienteId,
                        "antes", dados("preferencial", cartao.isPreferencial()),
                        "depois", dados("preferencial", novaPreferencia)
                    )
                );
            }
        }
    }

    private Endereco exigirEndereco(Long clienteId, Long enderecoId) {
        Endereco endereco = repository.buscarEnderecoPorId(clienteId, enderecoId);
        if (endereco == null) {
            throw erro("Endereço não encontrado para este cliente.");
        }
        return endereco;
    }

    private CartaoCredito exigirCartao(Long clienteId, Long cartaoId) {
        return encontrarCartao(repository.buscarCartoes(clienteId), cartaoId);
    }

    private CartaoCredito encontrarCartao(List<CartaoCredito> cartoes, Long id) {
        return cartoes.stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> erro("Cartão não encontrado para este cliente."));
    }

    private void validarEndereco(Endereco e) {
        List<String> erros = new ArrayList<>();

        if (vazio(e.getNomeCurto())) erros.add("Nome curto do endereço é obrigatório.");
        if (!"ENTREGA".equals(e.getTipo()) && !"COBRANCA".equals(e.getTipo())) {
            erros.add("Tipo do endereço deve ser ENTREGA ou COBRANCA.");
        }
        if (vazio(e.getTipoResidencia())) erros.add("Tipo de residência é obrigatório.");
        if (vazio(e.getLogradouro())) erros.add("Logradouro é obrigatório.");
        if (vazio(e.getNumero())) erros.add("Número do endereço é obrigatório.");
        if (vazio(e.getBairro())) erros.add("Bairro é obrigatório.");
        if (vazio(e.getCep())) erros.add("CEP é obrigatório.");
        if (vazio(e.getCidade())) erros.add("Cidade é obrigatória.");
        if (vazio(e.getEstado())) erros.add("Estado é obrigatório.");

        rejeitarErros(erros);
    }

    private void validarCartao(CartaoCredito c) {
        List<String> erros = new ArrayList<>();
        String numero = c.getNumero() == null ? "" : c.getNumero().replaceAll("[ -]", "");

        if (!numero.matches("[0-9]{13,19}")) {
            erros.add("Informe o número completo do cartão, com 13 a 19 dígitos.");
        }
        if (vazio(c.getNomeImpresso())) erros.add("Nome impresso é obrigatório.");
        if (vazio(c.getBandeira())) erros.add("Bandeira é obrigatória.");
        if (c.getCodigoSeguranca() == null || !c.getCodigoSeguranca().matches("[0-9]{3,4}")) {
            erros.add("Código de segurança deve ter 3 ou 4 dígitos.");
        }

        rejeitarErros(erros);
    }

    private boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private boolean senhaForte(String senha) {
        return senha != null && senha.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$");
    }

    private boolean cpfValido(String cpf) {
        if (cpf == null) return false;
        String numeros = cpf.replaceAll("\\D", "");
        if (numeros.length() != 11) return false;
        return !numeros.matches("(\\d)\\1{10}");
    }

    private void validarCartoesPreferenciais(List<CartaoCredito> cartoes, List<String> erros) {
        long quantidade = cartoes.stream()
            .filter(c -> c != null && c.isPreferencial())
            .count();

        if (quantidade > 1) {
            erros.add("Apenas um cartão pode ser preferencial");
        }
    }

    private void garantirUmPreferencial(List<CartaoCredito> cartoes) {
        boolean existePreferencial = cartoes.stream().anyMatch(c -> c != null && c.isPreferencial());
        if (!existePreferencial && !cartoes.isEmpty()) {
            cartoes.get(0).setPreferencial(true);
        }
    }

    private String mascarar(String numero) {
        if (numero == null) return null;
        String somenteNumeros = numero.replaceAll("\\D", "");
        if (somenteNumeros.length() < 4) return numero;
        String ultimosQuatro = somenteNumeros.substring(somenteNumeros.length() - 4);
        return "**** **** **** " + ultimosQuatro;
    }

    private ValidacaoException erro(String mensagem) {
        return new ValidacaoException(List.of(mensagem));
    }

    private void rejeitarErros(List<String> erros) {
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }

    private Map<String, Object> dadosEndereco(Long clienteId, Endereco e) {
        return dados(
            "cliente_id", clienteId,
            "nome_curto", e.getNomeCurto(),
            "tipo", e.getTipo(),
            "tipo_residencia", e.getTipoResidencia(),
            "logradouro", e.getLogradouro(),
            "numero", e.getNumero(),
            "bairro", e.getBairro(),
            "cep", e.getCep(),
            "cidade", e.getCidade(),
            "estado", e.getEstado()
        );
    }

    private Map<String, Object> dadosCartao(Long clienteId, CartaoCredito c) {
        return dados(
            "cliente_id", clienteId,
            "numero_mascarado", c.getNumero(),
            "nome_impresso", c.getNomeImpresso(),
            "bandeira", c.getBandeira(),
            "preferencial", c.isPreferencial()
        );
    }

    private Map<String, Object> dadosCliente(Cliente c, boolean incluirAtivo) {
        Map<String, Object> valores = dados(
            "nome", c.getNome(),
            "genero", c.getGenero(),
            "data_nascimento", c.getDataNascimento(),
            "cpf", c.getCpf(),
            "telefone", c.getTelefone(),
            "email", c.getEmail()
        );

        if (incluirAtivo) {
            valores.put("ativo", true);
        }

        return valores;
    }

    private void registrarLog(String operacao, String tabela, Long id, Map<String, Object> valores) {
        if (id == null) {
            throw new IllegalArgumentException("O log exige um ID de registro.");
        }

        logRepository.registrar(new LogTransacao(
            null,
            LocalDateTime.now(),
            usuarioAtual.getIdentificador(),
            operacao,
            tabela,
            id,
            resumo(valores)
        ));
    }

    private String resumo(Map<String, Object> valores) {
        return objectMapper.writeValueAsString(valores);
    }

    private Map<String, Object> dados(Object... pares) {
        if (pares.length % 2 != 0) {
            throw new IllegalArgumentException("Informe pares de chave e valor.");
        }

        Map<String, Object> valores = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            valores.put((String) pares[i], pares[i + 1]);
        }

        return valores;
    }
}