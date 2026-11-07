describe('RF0055 - Analisar vendas na tela administrativa', () => {
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

  it('abre sem inventar dados antes da primeira consulta', () => {
    cy.visit('/admin/analise');

    cy.get('[data-cy="orientacao-analise"]')
      .should('be.visible');

    cy.get('[data-cy="grafico-vendas"]')
      .should('not.exist');

    cy.get('[data-cy="sem-vendas-analise"]')
      .should('not.exist');

    cy.window().its('dadosGrafico').should('deep.equal', {
      etiquetas: [],
      series: []
    });
  });

  it('exibe os valores reais e os entrega ao Cypress', () => {
    cy.task('analise:totais').then(esperado => {
      cy.visit(
        `/admin/analise?dataInicio=${c.dataInicio}&dataFim=${c.dataFim}`
      );

      cy.get('[data-cy="grafico-vendas"]')
        .should('be.visible');

      const series = esperado.series.map(s => ({
        categoria: s.categoria,
        valores: s.valores
      }));

      cy.window().its('dadosGrafico').should('deep.equal', {
        etiquetas: esperado.etiquetas,
        series
      });

      cy.window().then(janela => {
        const datasets = janela.graficoVendas.data.datasets;

        expect(
          datasets.map(s => ({
            categoria: s.label,
            valores: s.data
          }))
        ).to.deep.eq(series);

        expect(
          new Set(datasets.map(s => s.borderColor)).size
        ).to.eq(4);
      });
    });
  });

  it('informa ausência de vendas e preserva os zeros reais', () => {
    const filtro = {
      dataInicio: '2026-02-01',
      dataFim: '2026-02-28',
      categorias: ['Autoajuda']
    };

    cy.task('analise:totais', filtro).then(esperado => {
      cy.visit(
        '/admin/analise?dataInicio=2026-02-01' +
        '&dataFim=2026-02-28&categorias=Autoajuda'
      );

      cy.get('[data-cy="sem-vendas-analise"]')
        .should('be.visible')
        .and('contain.text', 'Sem vendas no período');

      cy.get('[data-cy="grafico-vendas"]')
        .should('not.exist');

      cy.window().its('dadosGrafico').should('deep.equal', {
        etiquetas: esperado.etiquetas,
        series: esperado.series.map(s => ({
          categoria: s.categoria,
          valores: s.valores
        }))
      });
    });
  });

  it('exibe a validação e preserva os filtros sem dados falsos', () => {
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

  it('filtra uma categoria mantendo sua cor', () => {
    cy.visit(
      `/admin/analise?dataInicio=${c.dataInicio}&dataFim=${c.dataFim}`
    );

    cy.window().then(janela => {
      const cor = janela.graficoVendas.data.datasets
        .find(s => s.label === 'Ficção Científica')
        .borderColor;

      cy.get('[name="categorias"]').uncheck();

      cy.get(
        '[name="categorias"][value="Ficção Científica"]'
      ).check();

      cy.get('[data-cy="filtrar-analise"]').click();

      cy.task('analise:totais', {
        categorias: ['Ficção Científica']
      }).then(esperado => {
        cy.window().its('dadosGrafico').should('deep.equal', {
          etiquetas: esperado.etiquetas,
          series: esperado.series.map(s => ({
            categoria: s.categoria,
            valores: s.valores
          }))
        });
      });

      cy.window().then(atual => {
        expect(
          atual.graficoVendas.data.datasets
        ).to.have.length(1);

        expect(
          atual.graficoVendas.data.datasets[0].borderColor
        ).to.eq(cor);
      });
    });
  });

  it('não transforma todas desmarcadas em todas selecionadas', () => {
    cy.visit(
      `/admin/analise?dataInicio=${c.dataInicio}&dataFim=${c.dataFim}`
    );

    cy.get('[name="categorias"]').uncheck();
    cy.get('[data-cy="filtrar-analise"]').click();

    cy.get('[data-cy="erros-analise"]')
      .should('contain.text', 'ao menos uma categoria');

    cy.get('[name="categorias"]')
      .should('not.be.checked');

    cy.get('[data-cy="grafico-vendas"]')
      .should('not.exist');
  });
});