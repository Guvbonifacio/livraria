describe('RF0056 - Validar a ordem das datas', () => {
  beforeEach(() => {
    cy.task('analise:preparar').then(() => {
      cy.request({
        method: 'POST',
        url: '/sessao/perfil',
        form: true,
        body: { perfil: 'ADMIN' }
      });
    });
  });

  it('recusa data final anterior à inicial e preserva os campos', () => {
    cy.visit(
      '/admin/analise?dataInicio=2026-03-01&dataFim=2026-02-28'
    );

    cy.get('[data-cy="erros-analise"]')
      .should('be.visible')
      .and('contain.text', 'anterior');

    cy.get('[name="dataInicio"]')
      .should('have.value', '2026-03-01');

    cy.get('[name="dataFim"]')
      .should('have.value', '2026-02-28');

    cy.get('[data-cy="grafico-vendas"]')
      .should('not.exist');

    cy.get('[data-cy="sem-vendas-analise"]')
      .should('not.exist');

    cy.window().its('dadosGrafico').should('deep.equal', {
      etiquetas: [],
      series: []
    });
  });
});