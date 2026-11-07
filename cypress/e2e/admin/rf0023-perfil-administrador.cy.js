describe('RF0023 - Perfil de administrador em sessão', () => {
  beforeEach(() => {
    cy.task('vendas:preparar', 'padrao');
  });

  it('cliente não acessa a administração nem inativa pelo POST direto', () => {
    cy.visit('/');

    cy.get('[data-cy="perfil-sessao"]')
      .should('have.value', 'CLIENTE');

    cy.contains('a', 'Administração').should('not.exist');

    cy.request({
      url: '/admin/clientes',
      failOnStatusCode: false
    }).its('status').should('eq', 403);

    cy.request({
      method: 'POST',
      url: '/admin/clientes/1/inativar',
      failOnStatusCode: false,
      followRedirect: false
    }).its('status').should('eq', 403);

    cy.task('perfil:estado').then(estado => {
      // Sem autenticação, o perfil vive na sessão e nao tem
      // coluna no banco. O banco registra apenas o efeito.
      expect(estado.cliente).to.include({ ativo: 1 });

      expect(estado.logs).to.have.length(0);
    });
  });

  it('permite administrar e voltar a cliente sem alterar o perfil no banco', () => {
    cy.visit('/');

    cy.get('[data-cy="perfil-sessao"]').select('ADMIN');
    cy.get('[data-cy="aplicar-perfil"]').click();

    cy.get('[data-cy="perfil-sessao"]')
      .should('have.value', 'ADMIN');

    cy.contains('a', 'Administração').click();
    cy.location('pathname').should('eq', '/admin');

    cy.visit('/admin/clientes/1');

    cy.get(
      'form[action="/admin/clientes/1/inativar"] button'
    ).click();

    cy.task('perfil:estado').then(estado => {
      expect(estado.cliente).to.include({ ativo: 0 });

      expect(
        estado.logs.some(log =>
          log.usuario === 'usuario-teste (ADMIN)' &&
          log.tabela === 'cliente' &&
          log.operacao === 'UPDATE'
        )
      ).to.eq(true);
    });

    cy.get('[data-cy="perfil-sessao"]').select('CLIENTE');
    cy.get('[data-cy="aplicar-perfil"]').click();

    cy.request({
      url: '/admin',
      failOnStatusCode: false
    }).its('status').should('eq', 403);
  });

  it('rejeita perfil inválido e não concede administração a uma nova sessão', () => {
    cy.request({
      method: 'POST',
      url: '/sessao/perfil',
      form: true,
      body: { perfil: 'ROOT' },
      failOnStatusCode: false
    }).its('status').should('eq', 400);

    cy.request({
      method: 'POST',
      url: '/sessao/perfil',
      form: true,
      body: { perfil: 'ADMIN' }
    });

    cy.request('/admin')
      .its('status').should('eq', 200);

    cy.clearCookies();

    cy.request({
      url: '/admin',
      failOnStatusCode: false
    }).its('status').should('eq', 403);
  });
});