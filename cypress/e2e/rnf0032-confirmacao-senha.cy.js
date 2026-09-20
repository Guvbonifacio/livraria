describe('RNF0032 - Confirmação de senha', () => {

  it('não permite cadastro quando a confirmação da senha é diferente', () => {

    const n = Date.now();

    cy.visit('/clientes/novo');

    cy.get('[name="nome"]').type('Confirmação Senha Teste');
    cy.get('[name="genero"]').select('Feminino');
    cy.get('[name="dataNascimento"]').type('1995-05-20');
    cy.get('[name="cpf"]').type(String(n).slice(-11));
    cy.get('[name="telefone"]').type('11999990000');
    cy.get('[name="email"]').type(`confirmacao${n}@teste.com`);

    cy.get('[name="senha"]').type('Senha@123');
    cy.get('[name="confirmacaoSenha"]').type('Senha@124');

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
    cy.get('[name="cartoes[0].nomeImpresso"]').type('CONFIRMACAO TESTE');
    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.get('[name="cartoes[0].codigoSeguranca"]').type('123');
    cy.get('[name="cartoes[0].preferencial"]').check();

    cy.contains('Finalizar Cadastro').click();

    cy.contains('A confirmação não confere com a senha')
      .should('be.visible');

    cy.url().should('include', '/clientes/salvar');
  });

});