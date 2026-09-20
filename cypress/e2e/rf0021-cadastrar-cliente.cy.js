describe('RF0021 - Cadastrar cliente', () => {
  it('cadastra um cliente com dados válidos e o exibe na consulta', () => {
    const n = Date.now();

    cy.visit('/clientes/novo');
    cy.wait(1000);

    // Dados pessoais
    cy.get('[name="nome"]').type('Maria Teste');
    cy.wait(500);

    cy.get('[name="genero"]').select('Feminino');
    cy.wait(500);

    cy.get('[name="dataNascimento"]').type('1995-05-20');
    cy.wait(500);

    cy.get('[name="cpf"]').type(String(n).slice(-11));
    cy.wait(500);

    cy.get('[name="telefone"]').type('11999990000');
    cy.wait(500);

    cy.get('[name="email"]').type(`maria${n}@teste.com`);
    cy.wait(500);

    // Segurança
    cy.get('[name="senha"]').type('Senha@123');
    cy.wait(500);

    cy.get('[name="confirmacaoSenha"]').type('Senha@123');
    cy.wait(500);

    // Endereço
    cy.get('[name="enderecos[0].nomeCurto"]').type('Casa');
    cy.wait(500);

    cy.get('[name="enderecos[0].tipo"]').select('ENTREGA');
    cy.wait(500);

    cy.get('[name="enderecos[0].tipoResidencia"]').select('Casa');
    cy.wait(500);

    cy.get('[name="enderecos[0].logradouro"]').type('Rua das Flores');
    cy.wait(500);

    cy.get('[name="enderecos[0].numero"]').type('100');
    cy.wait(500);

    cy.get('[name="enderecos[0].bairro"]').type('Centro');
    cy.wait(500);

    cy.get('[name="enderecos[0].cep"]').type('08700000');
    cy.wait(500);

    cy.get('[name="enderecos[0].cidade"]').type('Mogi das Cruzes');
    cy.wait(500);

    cy.get('[name="enderecos[0].estado"]').type('SP');
    cy.wait(500);

    // Cartão de crédito
    cy.get('[name="cartoes[0].numero"]').type('4111111111111111');
    cy.wait(500);

    cy.get('[name="cartoes[0].nomeImpresso"]').type('MARIA TESTE');
    cy.wait(500);

    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.wait(500);

    cy.get('[name="cartoes[0].codigoSeguranca"]').type('123');
    cy.wait(500);

    cy.get('[name="cartoes[0].preferencial"]').check();
    cy.wait(1000);

    // Submissão
    cy.contains('Finalizar Cadastro').click();

    // Tempo para visualizar o resultado do cadastro
    cy.wait(2000);

    // Validações
    cy.url().should('match', /\/clientes\/\d+$/);
    cy.get('.alert-danger').should('not.exist');
    cy.get('[name="nome"]').should('have.value', 'Maria Teste');

    // Mantém a tela final visível por alguns segundos
    cy.wait(3000);
  });
});