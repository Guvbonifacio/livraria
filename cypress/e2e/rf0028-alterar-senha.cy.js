describe('RF0028 - Alterar apenas a senha', () => {
  const senhaInicial = 'Senha@123';
  const novaSenha = 'NovaSenha@456';

  function cadastrarCliente() {
    const n = `${Date.now()}${Cypress._.random(1000, 9999)}`;
    const nome = `Cliente Senha ${n}`;

    return cy.request({
      method: 'POST',
      url: '/clientes/salvar',
      form: true,
      log: false,
      body: {
        nome,
        genero: 'Feminino',
        dataNascimento: '1995-05-20',
        cpf: n.slice(-11),
        telefone: '11999990000',
        email: `senha${n}@teste.com`,
        senha: senhaInicial,
        confirmacaoSenha: senhaInicial,

        'enderecos[0].nomeCurto': 'Casa',
        'enderecos[0].tipo': 'ENTREGA',
        'enderecos[0].tipoResidencia': 'Casa',
        'enderecos[0].logradouro': 'Rua das Flores',
        'enderecos[0].numero': '100',
        'enderecos[0].bairro': 'Centro',
        'enderecos[0].cep': '08700000',
        'enderecos[0].cidade': 'Mogi das Cruzes',
        'enderecos[0].estado': 'SP',

        'cartoes[0].numero': '4111111111111111',
        'cartoes[0].nomeImpresso': 'CLIENTE SENHA',
        'cartoes[0].bandeira': 'Visa',
        'cartoes[0].codigoSeguranca': '123',
        'cartoes[0].preferencial': 'true'
      }
    }).then((response) => {
      expect(response.status).to.equal(200);
      expect(response.redirects).to.have.length(1);

      const redirect = response.redirects[0];
      const match = redirect.match(/\/clientes\/(\d+)$/);

      expect(match, 'redirecionamento para o cliente criado')
        .not.to.be.null;

      return {
        id: match[1],
        nome
      };
    });
  }

  function enviarAlteracao(atual, nova) {
    cy.get('[data-bs-target="#modalSenha"]').click();

    cy.get('#modalSenha')
      .should('be.visible')
      .within(() => {
        cy.get('[name="senhaAtual"]')
          .type(atual, { log: false });

        cy.get('[name="novaSenha"]')
          .type(nova, { log: false });

        cy.get('[name="confirmacao"]')
          .type(nova, { log: false });

        cy.contains('button', 'Atualizar Senha').click();
      });
  }

  it('altera a senha e mantém o nome do cliente', () => {
    cadastrarCliente().then(({ id, nome }) => {
      cy.visit(`/clientes/${id}`);

      cy.get('[name="nome"]').should('have.value', nome);

      enviarAlteracao(senhaInicial, novaSenha);

      cy.location('pathname').should('equal', `/clientes/${id}`);

      cy.get('[data-cy="sucesso-senha"]')
        .should('be.visible')
        .and('contain.text', 'Senha alterada com sucesso.');

      cy.reload();
      cy.get('[name="nome"]').should('have.value', nome);

      // Uma segunda alteração usando a nova senha como senha atual
      // confirma que a primeira alteração foi persistida.
      enviarAlteracao(novaSenha, 'OutraSenha@789');

      cy.get('[data-cy="sucesso-senha"]').should('be.visible');
      cy.get('[name="nome"]').should('have.value', nome);

      cy.visit(`/admin/clientes/${id}`);

      cy.contains('[data-cy="log-dados"]', /^senha alterada$/)
        .closest('[data-cy="log-transacao"]')
        .within(() => {
          cy.get('[data-cy="log-operacao"]')
            .should('have.text', 'UPDATE');

          cy.get('[data-cy="log-tabela"]')
            .should('have.text', 'cliente');

          cy.get('[data-cy="log-registro-id"]')
            .should('have.text', id);
        });
    });
  });

  it('recusa senha atual incorreta sem alterar a senha existente', () => {
    cadastrarCliente().then(({ id, nome }) => {
      cy.visit(`/clientes/${id}`);

      enviarAlteracao('SenhaErrada@999', novaSenha);

      cy.get('#modalSenha').should('be.visible');

      cy.get('[data-cy="erros-senha"]')
        .should('be.visible')
        .and('contain.text', 'Senha atual incorreta.');

      cy.get('[data-cy="sucesso-senha"]').should('not.exist');
      cy.get('[name="nome"]').should('have.value', nome);

      cy.get('#modalSenha [name="senhaAtual"]')
        .should('have.value', '');

      cy.get('#modalSenha [name="novaSenha"]')
        .should('have.value', '');

      cy.get('#modalSenha [name="confirmacao"]')
        .should('have.value', '');

      // A senha original ainda deve funcionar após a tentativa recusada.
      cy.visit(`/clientes/${id}`);

      enviarAlteracao(senhaInicial, novaSenha);

      cy.get('[data-cy="sucesso-senha"]').should('be.visible');
      cy.get('[name="nome"]').should('have.value', nome);
    });
  });
});