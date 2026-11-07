describe('RF0055 - Acesso administrativo à análise', () => {
  let c;

  beforeEach(() => {
    cy.task('analise:preparar').then(cenario => {
      c = cenario;

      cy.request({
        method: 'POST',
        url: '/sessao/perfil',
        form: true,
        body: { perfil: 'ADMIN' }
      });
    });
  });

  it('permite ADMIN, recusa CLIENTE e permite novamente ADMIN', () => {
    cy.visit(
      `/admin/analise?dataInicio=${c.dataInicio}`
      + `&dataFim=${c.dataFim}`
    );

    cy.get('[data-cy="grafico-vendas"]')
      .should('be.visible');

    cy.get('[data-cy="perfil-sessao"]').select('CLIENTE');
    cy.get('[data-cy="aplicar-perfil"]').click();

    cy.request({
      url: '/admin/analise',
      failOnStatusCode: false
    }).its('status').should('eq', 403);

    cy.get('[data-cy="perfil-sessao"]').select('ADMIN');
    cy.get('[data-cy="aplicar-perfil"]').click();

    cy.visit(
      `/admin/analise?dataInicio=${c.dataInicio}`
      + `&dataFim=${c.dataFim}`
    );

    cy.get('[data-cy="grafico-vendas"]')
      .should('be.visible');

    cy.window().its('dadosGrafico.series')
      .should('have.length', 4);
  });
});