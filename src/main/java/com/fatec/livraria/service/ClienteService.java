package com.fatec.livraria.service;

import com.fatec.livraria.model.CartaoCredito;
import com.fatec.livraria.model.Cliente;
import com.fatec.livraria.repository.ClienteRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    // Cadastro de novo cliente com validações e criptografia
    @Transactional
    public Long cadastrar(Cliente cliente, String confirmacaoSenha) {
        // Sanitização dos dados antes das validações
        if (cliente.getCpf() != null) {
            cliente.setCpf(cliente.getCpf().replaceAll("\\D", ""));
        }
        if (cliente.getEmail() != null) {
            cliente.setEmail(cliente.getEmail().trim().toLowerCase());
        }

        List<String> erros = new ArrayList<>();

        // Validação de preenchimento dos campos obrigatórios
        if (vazio(cliente.getNome())) erros.add("Nome é obrigatório");
        if (vazio(cliente.getCpf())) erros.add("CPF é obrigatório");
        if (vazio(cliente.getEmail())) erros.add("E-mail é obrigatório");
        if (vazio(cliente.getTelefone())) erros.add("Telefone é obrigatório");
        if (cliente.getDataNascimento() == null) erros.add("Data de nascimento é obrigatória");
        if (vazio(cliente.getGenero())) erros.add("Gênero é obrigatório");

        // Validação de formato e duplicidade do CPF
        if (!vazio(cliente.getCpf())) {
            if (!cpfValido(cliente.getCpf())) {
                erros.add("CPF inválido");
            } else if (repository.existePorCpf(cliente.getCpf())) {
                erros.add("CPF já cadastrado");
            }
        }

        // Validação de duplicidade do E-mail
        if (!vazio(cliente.getEmail()) && repository.existePorEmail(cliente.getEmail())) {
            erros.add("E-mail já cadastrado");
        }

        // Validação dos critérios de força da senha
        if (!senhaForte(cliente.getSenha())) {
            erros.add("A senha deve ter no mínimo 8 caracteres, com letras maiúsculas, minúsculas e caractere especial");
        }

        // Confirmação de igualdade entre senha e confirmação
        if (cliente.getSenha() == null || !cliente.getSenha().equals(confirmacaoSenha)) {
            erros.add("A confirmação não confere com a senha");
        }

        // Validação da presença de ao menos um endereço
        if (cliente.getEnderecos() == null || cliente.getEnderecos().isEmpty()) {
            erros.add("Informe ao menos um endereço");
        }

        // Validação da presença de ao menos um cartão e limite de preferencial
        if (cliente.getCartoes() == null || cliente.getCartoes().isEmpty()) {
            erros.add("Informe ao menos um cartão");
        } else {
            validarCartoesPreferenciais(cliente.getCartoes(), erros);
        }

        // Interrompe o fluxo lançando exceção se houver algum erro registrado
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        // Criptografia da senha antes de salvar
        cliente.setSenha(encoder.encode(cliente.getSenha()));

        // Garante que exista ao menos um cartão marcado como preferencial
        garantirUmPreferencial(cliente.getCartoes());

        // Mascaramento do número de todos os cartões para segurança
        cliente.getCartoes().forEach(cartao -> cartao.setNumero(mascarar(cartao.getNumero())));

        // Persistência do cliente e dos relacionamentos no banco de dados
        Long id = repository.salvar(cliente);
        cliente.getEnderecos().forEach(endereco -> repository.salvarEndereco(id, endereco));
        cliente.getCartoes().forEach(cartao -> repository.salvarCartao(id, cartao));

        return id;
    }

    // Consulta de clientes com filtros opcionais via repositório
    public List<Cliente> consultar(String nome, String cpf, String email) {
        return repository.consultar(nome, cpf, email);
    }

    // Busca de cliente por ID no repositório
    public Cliente buscarPorId(Long id) {
        return repository.buscarPorId(id);
    }

    // Alteração dos dados cadastrais do cliente
    @Transactional
    public void alterar(Cliente cliente) {
        // Normalização
        if (cliente.getCpf() != null) {
            cliente.setCpf(cliente.getCpf().replaceAll("\\D", ""));
        }
        if (cliente.getEmail() != null) {
            cliente.setEmail(cliente.getEmail().trim().toLowerCase());
        }

        List<String> erros = new ArrayList<>();

        // Campos obrigatórios
        if (vazio(cliente.getNome())) erros.add("Nome é obrigatório");
        if (vazio(cliente.getCpf())) erros.add("CPF é obrigatório");
        if (vazio(cliente.getEmail())) erros.add("E-mail é obrigatório");
        if (vazio(cliente.getTelefone())) erros.add("Telefone é obrigatório");
        if (cliente.getDataNascimento() == null) erros.add("Data de nascimento é obrigatória");
        if (vazio(cliente.getGenero())) erros.add("Gênero é obrigatório");

        // Validação de CPF ignorando o próprio ID
        if (!vazio(cliente.getCpf())) {
            if (!cpfValido(cliente.getCpf())) {
                erros.add("CPF inválido");
            } else if (repository.existePorCpf(cliente.getCpf(), cliente.getId())) {
                erros.add("CPF já cadastrado");
            }
        }

        // Validação de E-mail ignorando o próprio ID
        if (!vazio(cliente.getEmail()) && repository.existePorEmail(cliente.getEmail(), cliente.getId())) {
            erros.add("E-mail já cadastrado");
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        repository.atualizar(cliente);
    }

    // Inativação lógica do cadastro do cliente
    public void inativar(Long id) {
        repository.inativar(id);
    }

    // Verifica se uma String é nula ou vazia
    private boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    // Valida o padrão de complexidade exigido para a senha via Regex
    private boolean senhaForte(String senha) {
        return senha != null && senha.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$");
    }

    // Valida a quantidade de dígitos e sequências numéricas repetidas do CPF
    private boolean cpfValido(String cpf) {
        if (cpf == null) return false;
        String numeros = cpf.replaceAll("\\D", "");
        if (numeros.length() != 11) return false;
        return !numeros.matches("(\\d)\\1{10}");
    }

    // Impede a seleção de mais de um cartão como preferencial
    private void validarCartoesPreferenciais(List<CartaoCredito> cartoes, List<String> erros) {
        long quantidade = cartoes.stream().filter(c -> c != null && c.isPreferencial()).count();
        if (quantidade > 1) {
            erros.add("Apenas um cartão pode ser preferencial");
        }
    }

    // Define o primeiro cartão como preferencial caso nenhum tenha sido selecionado
    private void garantirUmPreferencial(List<CartaoCredito> cartoes) {
        boolean existePreferencial = cartoes.stream().anyMatch(c -> c != null && c.isPreferencial());
        if (!existePreferencial && !cartoes.isEmpty()) {
            cartoes.get(0).setPreferencial(true);
        }
    }

    // Exibe apenas os últimos 4 dígitos do cartão de crédito
    private String mascarar(String numero) {
        if (numero == null) return null;
        String somenteNumeros = numero.replaceAll("\\D", "");
        if (somenteNumeros.length() < 4) return numero;
        String ultimosQuatro = somenteNumeros.substring(somenteNumeros.length() - 4);
        return "**** **** **** " + ultimosQuatro;
    }
}