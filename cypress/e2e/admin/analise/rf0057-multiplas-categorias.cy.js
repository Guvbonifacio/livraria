describe('RF0057 - Comparar categorias', () => {
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

  it('compara Fantasia e Ficção Científica com seus totais esperados', () => {
    const categorias = ['Fantasia', 'Ficção Científica'];

    cy.task('analise:totais', { categorias }).then(esperado => {
      const query = new URLSearchParams({
        dataInicio: c.dataInicio,
        dataFim: c.dataFim
      });

      categorias.forEach(nome => {
        query.append('categorias', nome);
      });

      cy.visit(`/admin/analise?${query}`);

      cy.window().its('dadosGrafico').should('deep.equal', {
        etiquetas: esperado.etiquetas,
        series: esperado.series.map(s => ({
          categoria: s.categoria,
          valores: s.valores
        }))
      });

      cy.window().then(janela => {
        expect(janela.dadosGrafico.series).to.have.length(2);

        for (const serie of janela.dadosGrafico.series) {
          const total = serie.valores.reduce(
            (soma, valor) => soma + Math.round(valor * 100),
            0
          );

          const referencia = esperado.series.find(
            s => s.categoria === serie.categoria
          );

          expect(total).to.eq(referencia.totalCentavos);
        }
      });
    });
  });
});