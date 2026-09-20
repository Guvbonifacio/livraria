describe('RF0024 - Consultar cliente', () => {

  it('consulta cliente utilizando filtro por nome', () => {

    const n = Date.now();
    const nome = `Filtro${n}`;

    cy.visit('/clientes/novo');

    cy.get('[name="nome"]').type(nome);
    cy.get('[name="genero"]').select('Feminino');
    cy.get('[name="dataNascimento"]').type('1995-05-20');
    cy.get('[name="cpf"]').type(String(n).slice(-11));
    cy.get('[name="telefone"]').type('11999990000');
    cy.get('[name="email"]').type(`filtro${n}@teste.com`);

    cy.get('[name="senha"]').type('Senha@123');
    cy.get('[name="confirmacaoSenha"]').type('Senha@123');

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
    cy.get('[name="cartoes[0].nomeImpresso"]').type('FILTRO TESTE');
    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.get('[name="cartoes[0].codigoSeguranca"]').type('123');
    cy.get('[name="cartoes[0].preferencial"]').check();

    cy.contains('Finalizar Cadastro').click();

    cy.url().should('match', /\/clientes\/\d+$/);

    cy.visit('/admin/clientes');

    cy.get('[name="nome"]').type(nome);

    cy.contains('Filtrar').click();

    cy.contains(nome).should('be.visible');

    cy.contains('Maria Teste').should('not.exist');
  });

});