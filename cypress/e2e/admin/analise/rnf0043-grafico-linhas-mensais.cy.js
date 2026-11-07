describe('RNF0043 - Gráfico de linhas mensais', () => {
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

  it('exibe linhas, etiquetas mensais e valores em reais', () => {
    cy.task('analise:totais').then(esperado => {
      cy.visit(
        `/admin/analise?dataInicio=${c.dataInicio}`
        + `&dataFim=${c.dataFim}`
      );

      cy.get('[data-cy="grafico-vendas"]')
        .should('be.visible');

      cy.window().then(janela => {
        const grafico = janela.graficoVendas;

        expect(grafico.config.type).to.eq('line');

        expect(grafico.data.labels)
          .to.deep.eq(esperado.etiquetas);

        expect(grafico.data.labels).to.have.length(14);

        grafico.data.labels.forEach(mes => {
          expect(mes).to.match(
            /^(jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)\/\d{4}$/
          );
        });

        const seriesDoGrafico = grafico.data.datasets.map(s => ({
          categoria: s.label,
          valores: s.data
        }));

        expect(seriesDoGrafico).to.deep.eq(
          esperado.series.map(s => ({
            categoria: s.categoria,
            valores: s.valores
          }))
        );

        const cores = new Set(
          grafico.data.datasets.map(s => s.borderColor)
        );

        expect(cores.size).to.eq(4);

        const moeda =
          grafico.options.scales.y.ticks.callback(1234.56);

        expect(moeda.replace(/\s/g, ' '))
          .to.eq('R$ 1.234,56');
      });
    });
  });
});