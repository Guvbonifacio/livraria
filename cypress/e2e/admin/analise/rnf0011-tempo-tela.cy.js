describe(
  'RNF0011 - Tempo até a tela de análise',
  { retries: 0 },
  () => {
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

    it('fica pronta em até um segundo desde antes do visit', () => {
      const relogio = performance;
      let inicio;

      cy.task('analise:totais').then(esperado => {
        cy.then(() => {
          inicio = relogio.now();
        });

        cy.visit(
          `/admin/analise?dataInicio=${c.dataInicio}`
          + `&dataFim=${c.dataFim}`
        );

        cy.window().its('dadosGrafico').should('deep.equal', {
          etiquetas: esperado.etiquetas,
          series: esperado.series.map(s => ({
            categoria: s.categoria,
            valores: s.valores
          }))
        });

        cy.get('[data-cy="grafico-vendas"]')
          .should('be.visible');

        cy.then(() => {
          const duracaoMs = relogio.now() - inicio;

          return cy.writeFile(
            'target/metricas/rnf0011-tela-analise.json',
            {
              requisito: 'RNF0011',
              medicao: 'Antes do visit até canvas visível e dados validados',
              limiteMs: 1000,
              duracaoMs,
              pedidos: c.quantidadePedidos,
              itens: c.quantidadeItens
            }
          ).then(() => {
            expect(
              duracaoMs,
              `Tela pronta em ${duracaoMs.toFixed(2)} ms`
            ).to.be.at.most(1000);
          });
        });
      });
    });
  }
);