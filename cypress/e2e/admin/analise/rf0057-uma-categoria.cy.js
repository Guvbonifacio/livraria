describe('RF0057 - Análise de uma categoria', () => {
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

  it('compara os valores de Literatura Clássica com os totais esperados', () => {
    cy.task('analise:totais', {
      categorias: ['Literatura Clássica']
    }).then(esperado => {
      const categoria = encodeURIComponent('Literatura Clássica');

      cy.visit(
        `/admin/analise?dataInicio=${c.dataInicio}`
        + `&dataFim=${c.dataFim}&categorias=${categoria}`
      );

      cy.window().its('dadosGrafico').should('deep.equal', {
        etiquetas: esperado.etiquetas,
        series: esperado.series.map(s => ({
          categoria: s.categoria,
          valores: s.valores
        }))
      });

      cy.window().its('dadosGrafico.series')
        .should('have.length', 1);

      cy.get('[data-cy="grafico-vendas"]')
        .should('be.visible');
    });
  });
});