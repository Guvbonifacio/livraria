describe('RF0023 - Inativar cliente', () => {

  it('inativa o cliente sem excluí-lo do sistema', () => {

    const n = Date.now();
    const nome = `Inativar ${n}`;

    // Cadastra um novo cliente
    cy.visit('/clientes/novo');

    cy.get('[name="nome"]').type(nome);
    cy.get('[name="genero"]').select('Feminino');
    cy.get('[name="dataNascimento"]').type('1995-05-20');
    cy.get('[name="cpf"]').type(String(n).slice(-11));
    cy.get('[name="telefone"]').type('11999990000');
    cy.get('[name="email"]').type(`inativar${n}@teste.com`);

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
    cy.get('[name="cartoes[0].nomeImpresso"]').type('INATIVAR TESTE');
    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.get('[name="cartoes[0].codigoSeguranca"]').type('123');
    cy.get('[name="cartoes[0].preferencial"]').check();

    cy.contains('Finalizar Cadastro').click();
    cy.url().should('match', /\/clientes\/\d+$/);

    // Inativa a conta através do modal de confirmação
    cy.contains('Inativar Conta').click();
    cy.contains('Sim, inativar cadastro').click();

    // Valida que o perfil exibe a situação Inativo
    cy.contains('Inativo').should('be.visible');

    // Acessa a consulta administrativa e verifica a persistência dos dados
    cy.visit('/admin/clientes');

    cy.get('[name="nome"]').type(nome);
    cy.contains('Filtrar').click();

    // Confirma que o cliente não foi excluído
    cy.contains(nome).should('be.visible');

    // Confirma que permanece cadastrado como inativo
    cy.contains('Inativo').should('be.visible');

  });

});