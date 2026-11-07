describe('RN0072 - Limites do período de análise', () => {
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

  it('recusa intervalo menor que um mês e maior que 24 meses', () => {
    const filtros = [
      {
        dataInicio: '2026-02-01',
        dataFim: '2026-02-15'
      },
      c.filtrosTeste.vinteCincoMeses
    ];

    filtros.forEach(filtro => {
      cy.visit(
        `/admin/analise?dataInicio=${filtro.dataInicio}`
        + `&dataFim=${filtro.dataFim}`
      );

      cy.get('[data-cy="erros-analise"]')
        .should('be.visible')
        .and('contain.text', 'mínimo 1 mês')
        .and('contain.text', 'máximo 24 meses');

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
});