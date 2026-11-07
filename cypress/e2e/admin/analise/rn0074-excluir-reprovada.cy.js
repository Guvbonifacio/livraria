describe('RN0074 - Excluir compra reprovada da análise', () => {
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

  it('mantém uma compra reprovada fora dos totais de vendas', () => {
    cy.task(
      'analise:pedidoControle',
      'REPROVADA'
    ).then(controle => {
      expect(controle.status).to.eq('REPROVADA');
      expect(controle.valorCentavos).to.be.greaterThan(0);

      cy.visit(`/admin/pedidos/${controle.id}`);

      cy.get('[data-cy="status-pedido"]').should(elemento => {
        expect(elemento.text().trim().toUpperCase())
          .to.eq('REPROVADA');
      });

      cy.task('analise:totais', {
        categorias: ['Fantasia']
      }).then(esperado => {
        cy.visit(
          `/admin/analise?dataInicio=${c.dataInicio}`
          + `&dataFim=${c.dataFim}&categorias=Fantasia`
        );

        cy.window().its('dadosGrafico').should('deep.equal', {
          etiquetas: esperado.etiquetas,
          series: esperado.series.map(s => ({
            categoria: s.categoria,
            valores: s.valores
          }))
        });
      });
    });
  });
});