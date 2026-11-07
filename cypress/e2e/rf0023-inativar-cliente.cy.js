describe('RF0023 - Inativar cliente', () => {
  it('inativa o cliente sem excluí-lo do sistema', () => {
    const n = Date.now();
    const nome = `Inativar ${n}`;

    // Cadastra um novo cliente.
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
    cy.get('[name="enderecos[0].tipoLogradouro"]').type('Rua');
    cy.get('[name="enderecos[0].logradouro"]').type('Rua das Flores');
    cy.get('[name="enderecos[0].numero"]').type('100');
    cy.get('[name="enderecos[0].bairro"]').type('Centro');
    cy.get('[name="enderecos[0].cep"]').type('08700000');
    cy.get('[name="enderecos[0].cidade"]').type('Mogi das Cruzes');
    cy.get('[name="enderecos[0].estado"]').type('SP');
    cy.get('[name="enderecos[0].pais"]').type('Brasil');

    cy.get('[name="cartoes[0].numero"]').type('4111111111111111');
    cy.get('[name="cartoes[0].nomeImpresso"]').type('INATIVAR TESTE');
    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.get('[name="cartoes[0].codigoSeguranca"]').type('123');
    cy.get('[name="cartoes[0].preferencial"]').check();

    cy.contains('Finalizar Cadastro').click();

    cy.location('pathname')
      .should('match', /^\/clientes\/\d+$/)
      .then(caminho => {
        const clienteId = caminho.split('/')[2];

        // Assume o perfil administrativo antes de inativar.
        cy.request({
          method: 'POST',
          url: '/sessao/perfil',
          form: true,
          body: { perfil: 'ADMIN' }
        });

        cy.visit(`/admin/clientes/${clienteId}`);

        // Inativa pelo formulário administrativo.
        cy.get(
          `form[action="/admin/clientes/${clienteId}/inativar"]`
        ).within(() => {
          cy.get('button[type="submit"]').click();
        });

        cy.contains('Inativo').should('be.visible');

        // Confirma que os dados continuam na consulta.
        cy.visit('/admin/clientes');

        cy.get('[name="nome"]').clear().type(nome);
        cy.contains('button', 'Filtrar').click();

        cy.contains('tr', nome).within(() => {
          cy.contains('Inativo').should('be.visible');
        });

        // Confirma a persistência após recarregar.
        cy.reload();

        cy.contains('tr', nome).within(() => {
          cy.contains('Inativo').should('be.visible');
        });
      });
  });
});