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
    const periodo =
      `/admin/analise?dataInicio=${c.dataInicio}`
      + `&dataFim=${c.dataFim}`;

    cy.visit(periodo);

    cy.get('[data-cy="grafico-vendas"]')
      .should('be.visible');

    // Permite visualizar o acesso do administrador aos dados.
    cy.wait(1000);

    cy.get('[data-cy="perfil-sessao"]').select('CLIENTE');
    cy.get('[data-cy="aplicar-perfil"]').click();

    // Mesma URL das tres fases: a unica variavel e o perfil. Com as
    // datas, a recusa vem do proprio AnaliseVendasService.analisar.
    cy.request({
      url: periodo,
      failOnStatusCode: false
    }).its('status').should('eq', 403);

    // Torna a recusa visivel na apresentacao: a pagina de erro do
    // Spring aparece no navegador, e o grafico nao existe nela.
    cy.visit(periodo, { failOnStatusCode: false });

    cy.get('[data-cy="grafico-vendas"]').should('not.exist');
    cy.contains('403').should('be.visible');

    cy.wait(1000);

    // A pagina de erro nao tem o menu: volta ao catalogo, que o
    // perfil CLIENTE acessa, para restabelecer o perfil ADMIN.
    cy.visit('/');

    cy.get('[data-cy="perfil-sessao"]').select('ADMIN');
    cy.get('[data-cy="aplicar-perfil"]').click();

    cy.visit(periodo);

    cy.get('[data-cy="grafico-vendas"]')
      .should('be.visible');

    cy.window().its('dadosGrafico.series')
      .should('have.length', 4);

    // Permite visualizar o acesso restabelecido ao administrador.
    cy.wait(1000);
  });
});