describe('RF0056 - Alterar período da análise', () => {
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

  it('gera outro resultado ao mudar as datas pelo formulário', () => {
    cy.visit(
      `/admin/analise?dataInicio=${c.dataInicio}`
      + `&dataFim=${c.dataFim}`
    );

    cy.window().its('dadosGrafico').then(anterior => {
      cy.get('[name="dataInicio"]')
        .clear()
        .type(c.filtrosTeste.umMes.dataInicio);

      cy.get('[name="dataFim"]')
        .clear()
        .type(c.filtrosTeste.umMes.dataFim);

      cy.get('[data-cy="filtrar-analise"]').click();

      cy.task(
        'analise:totais',
        c.filtrosTeste.umMes
      ).then(esperado => {
        cy.window().its('dadosGrafico').should(atual => {
          expect(atual).to.deep.eq({
            etiquetas: esperado.etiquetas,
            series: esperado.series.map(s => ({
              categoria: s.categoria,
              valores: s.valores
            }))
          });

          expect(atual).not.to.deep.eq(anterior);
          expect(atual.etiquetas).to.have.length(1);
        });
      });
    });
  });
});