describe('RF0022 - Alterar cliente', () => {
  it('altera os dados pessoais de um cliente cadastrado', () => {
    const n = Date.now();
    const nome = `Alterar ${n}`;
    const nomeAlterado = `${nome} Alterado`;

    // Cadastra um novo cliente
    cy.visit('/clientes/novo');

    cy.get('[name="nome"]').type(nome);
    cy.get('[name="genero"]').select('Feminino');
    cy.get('[name="dataNascimento"]').type('1995-05-20');
    cy.get('[name="cpf"]').type(String(n).slice(-11));
    cy.get('[name="telefone"]').type('11999990000');
    cy.get('[name="email"]').type(`alterar${n}@teste.com`);

    // Segurança
    cy.get('[name="senha"]').type('Senha@123');
    cy.get('[name="confirmacaoSenha"]').type('Senha@123');

    // Endereço
    cy.get('[name="enderecos[0].nomeCurto"]').type('Casa');
    cy.get('[name="enderecos[0].tipo"]').select('ENTREGA');
    cy.get('[name="enderecos[0].tipoResidencia"]').select('Casa');
    cy.get('[name="enderecos[0].logradouro"]').type('Rua das Flores');
    cy.get('[name="enderecos[0].numero"]').type('100');
    cy.get('[name="enderecos[0].bairro"]').type('Centro');
    cy.get('[name="enderecos[0].cep"]').type('08700000');
    cy.get('[name="enderecos[0].cidade"]').type('Mogi das Cruzes');
    cy.get('[name="enderecos[0].estado"]').type('SP');

    // Cartão
    cy.get('[name="cartoes[0].numero"]').type('4111111111111111');
    cy.get('[name="cartoes[0].nomeImpresso"]').type('ALTERAR TESTE');
    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.get('[name="cartoes[0].codigoSeguranca"]').type('123');
    cy.get('[name="cartoes[0].preferencial"]').check();

    // Finaliza o cadastro
    cy.contains('Finalizar Cadastro').click();

    // Valida navegação para o perfil
    cy.url().should('match', /\/clientes\/\d+$/);

    // Abre a edição e valida a tela
    cy.contains('Alterar dados').click();
    cy.contains('Alterar cadastro').should('be.visible');

    // Altera o nome
    cy.get('[name="nome"]')
      .clear()
      .type(nomeAlterado);

    // Salva a alteração
    cy.contains('Salvar alterações').click();

    // Validações pós-salvamento
    cy.url().should('match', /\/clientes\/\d+$/);
    cy.get('.alert-danger').should('not.exist');

    // Garante a persistência dos dados
    cy.reload();
    cy.get('[name="nome"]').should('have.value', nomeAlterado);
  });
});