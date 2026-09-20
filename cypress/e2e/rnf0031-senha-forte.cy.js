describe('RNF0031 - Senha forte', () => {

  it('não permite cadastro com senha fraca', () => {

    const n = Date.now();

    cy.visit('/clientes/novo');

    cy.get('[name="nome"]').type('Senha Fraca Teste');
    cy.get('[name="genero"]').select('Feminino');
    cy.get('[name="dataNascimento"]').type('1995-05-20');
    cy.get('[name="cpf"]').type(String(n).slice(-11));
    cy.get('[name="telefone"]').type('11999990000');
    cy.get('[name="email"]').type(`senhafraca${n}@teste.com`);

    cy.get('[name="senha"]').type('abc123');
    cy.get('[name="confirmacaoSenha"]').type('abc123');

    cy.get('[name="enderecos[0].nomeCurto"]').type('Casa');
    cy.get('[name="enderecos[0].tipo"]').select('ENTREGA');
    cy.get('[name="enderecos[0].tipoResidencia"]').select('Casa');
    cy.get('[name="enderecos[0].logradouro"]').type('Rua das Flores');
    cy.get('[name="enderecos[0].numero"]').type('100');
    cy.get('[name="enderecos[0].bairro"]').type('Centro');
    cy.get('[name="enderecos[0].cep"]').type('08700000');
    cy.get('[name="enderecos[0].cidade"]').type('Mogi das Cruzes');
    cy.get('[name="enderecos[0].estado"]').type('SP');

    cy.get('[name="cartoes[0].numero"]').type('4111111111111111');
    cy.get('[name="cartoes[0].nomeImpresso"]').type('SENHA FRACA');
    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.get('[name="cartoes[0].codigoSeguranca"]').type('123');
    cy.get('[name="cartoes[0].preferencial"]').check();

    cy.contains('Finalizar Cadastro').click();

    cy.contains('mínimo 8 caracteres').should('be.visible');

    cy.url().should('include', '/clientes/salvar');
  });

});