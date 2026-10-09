function cadastrarClienteTeste(prefixo) {
  const n = `${Date.now()}${Cypress._.random(1000, 9999)}`;
  const nome = `${prefixo} ${n}`;

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
      email: `${prefixo}${n}@teste.com`,
      senha: 'Senha@123',
      confirmacaoSenha: 'Senha@123',

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
      'cartoes[0].nomeImpresso': 'CLIENTE TESTE',
      'cartoes[0].bandeira': 'Visa',
      'cartoes[0].codigoSeguranca': '123',
      'cartoes[0].preferencial': 'true'
    }
  }).then((resposta) => {
    expect(resposta.status).to.equal(200);
    expect(resposta.redirects).to.have.length(1);

    const match = resposta.redirects[0].match(/\/clientes\/(\d+)$/);

    expect(
      match,
      'cadastro deve redirecionar para o perfil'
    ).not.to.be.null;

    return {
      id: match[1],
      nome
    };
  });
}

describe('RF0027 - Cartão preferencial', () => {
  it('troca o preferencial, promove outro ao removê-lo e protege o último cartão', () => {
    cadastrarClienteTeste('Cartao').then(({ id, nome }) => {
      cy.visit(`/clientes/${id}`);

      cy.get('[data-cy="cartao-item"]').should('have.length', 1);
      cy.get('[data-cy="cartao-preferencial"]')
        .should('have.length', 1);

      cy.get('[data-bs-target="#modalCartao"]').click();

      cy.get('#modalCartao')
        .should('be.visible')
        .within(() => {
          cy.get('[name="numero"]')
            .type('5555555555554444', { log: false });

          cy.get('[name="nomeImpresso"]').type('SEGUNDO CARTAO');
          cy.get('[name="bandeira"]').select('Mastercard');

          cy.get('[name="codigoSeguranca"]')
            .type('123', { log: false });

          cy.get('[name="preferencial"]').should('not.be.checked');

          cy.contains('button', 'Salvar Cartão').click();
        });

      cy.get('[data-cy="cartao-item"]').should('have.length', 2);

      // O cartão anterior continua preferencial após a adição.
      cy.contains('[data-cy="cartao-numero"]', '1111')
        .closest('[data-cy="cartao-item"]')
        .within(() => {
          cy.get('[data-cy="cartao-preferencial"]')
            .should('be.visible');
        });

      // Escolhe o segundo cartão como preferencial.
      cy.contains('[data-cy="cartao-numero"]', '4444')
        .closest('[data-cy="cartao-item"]')
        .within(() => {
          cy.get('[data-cy="cartao-secundario"]')
            .should('be.visible');

          cy.get('[data-cy="tornar-preferencial"]').click();
        });

      cy.get('[data-cy="sucesso-relacionamentos"]')
        .should('contain.text', 'Cartão preferencial atualizado.');

      cy.reload();

      // Deve existir exatamente um preferencial.
      cy.get('[data-cy="cartao-preferencial"]')
        .should('have.length', 1);

      cy.contains('[data-cy="cartao-numero"]', '4444')
        .closest('[data-cy="cartao-item"]')
        .within(() => {
          cy.get('[data-cy="cartao-preferencial"]')
            .should('be.visible');
        });

      // O cartão anterior deve ter deixado de ser preferencial.
      cy.contains('[data-cy="cartao-numero"]', '1111')
        .closest('[data-cy="cartao-item"]')
        .within(() => {
          cy.get('[data-cy="cartao-secundario"]')
            .should('be.visible');

          cy.get('[data-cy="cartao-preferencial"]')
            .should('not.exist');
        });

      cy.get('[name="nome"]').should('have.value', nome);

      // Remove o preferencial e verifica a promoção do restante.
      cy.contains('[data-cy="cartao-numero"]', '4444')
        .closest('[data-cy="cartao-item"]')
        .within(() => {
          cy.get('[data-cy="remover-cartao"]').click();
        });

      cy.get('[data-cy="cartao-item"]').should('have.length', 1);

      cy.contains('[data-cy="cartao-numero"]', '1111')
        .closest('[data-cy="cartao-item"]')
        .within(() => {
          cy.get('[data-cy="cartao-preferencial"]')
            .should('be.visible');
        });

      // O servidor deve impedir a exclusão do último cartão.
      cy.get('[data-cy="remover-cartao"]').click();

      cy.get('[data-cy="erros-relacionamentos"]')
        .should(
          'contain.text',
          'Não é permitido remover o último cartão.'
        );

      cy.get('[data-cy="cartao-item"]').should('have.length', 1);

      cy.get('[data-cy="cartao-preferencial"]')
        .should('have.length', 1);

      cy.get('[name="nome"]').should('have.value', nome);
    });
  });
});