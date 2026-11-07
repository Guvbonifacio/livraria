describe('RN0073 - Preservar mês sem vendas', () => {
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

  it('mantém fevereiro zerado entre meses com vendas de Autoajuda', () => {
    cy.task('analise:totais', {
      categorias: ['Autoajuda']
    }).then(esperado => {
      const indice =
        esperado.meses.indexOf(c.mesSemVendas.mes);

      expect(indice).to.be.greaterThan(0);

      cy.visit(
        `/admin/analise?dataInicio=${c.dataInicio}`
        + `&dataFim=${c.dataFim}&categorias=Autoajuda`
      );

      cy.get('[data-cy="grafico-vendas"]')
        .should('be.visible');

      cy.window().its('dadosGrafico').should('deep.equal', {
        etiquetas: esperado.etiquetas,
        series: esperado.series.map(s => ({
          categoria: s.categoria,
          valores: s.valores
        }))
      });

      cy.window().then(janela => {
        const grafico = janela.graficoVendas;

        expect(grafico.config.type).to.eq('line');

        expect(grafico.data.labels[indice])
          .to.eq('fev/2026');

        const valores = grafico.data.datasets[0].data;

        expect(valores).to.have.length(14);
        expect(valores[indice]).to.eq(0);
        expect(valores[indice - 1]).to.be.greaterThan(0);
        expect(valores[indice + 1]).to.be.greaterThan(0);
      });
    });
  });
});