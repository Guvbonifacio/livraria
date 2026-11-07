describe('RN0074 - Contabilizar compra entregue', () => {
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

  it('aprova, despacha, entrega e confere o acréscimo nas vendas', () => {
    cy.task(
      'analise:pedidoControle',
      'EM PROCESSAMENTO'
    ).then(controle => {
      expect(controle.status).to.eq('EM PROCESSAMENTO');
      expect(controle.categorias).to.deep.eq(['Fantasia']);

      cy.task('analise:totais', {
        categorias: ['Fantasia']
      }).then(antes => {
        cy.visit(
          `/admin/analise?dataInicio=${c.dataInicio}`
          + `&dataFim=${c.dataFim}&categorias=Fantasia`
        );

        cy.window().its('dadosGrafico').should('deep.equal', {
          etiquetas: antes.etiquetas,
          series: antes.series.map(s => ({
            categoria: s.categoria,
            valores: s.valores
          }))
        });

        cy.visit(`/admin/pedidos/${controle.id}`);

        cy.get(
          'button[name="novoStatus"][value="APROVADA"]'
        ).click();

        cy.get('[data-cy="status-pedido"]').should(elemento => {
          expect(elemento.text().trim().toUpperCase())
            .to.eq('APROVADA');
        });

        cy.get('[data-cy="despachar-pedido"]').click();

        cy.get('[data-cy="status-pedido"]').should(elemento => {
          expect(elemento.text().trim().toUpperCase())
            .to.eq('EM TRANSPORTE');
        });

        cy.get('[data-cy="entregar-pedido"]').click();

        cy.get('[data-cy="status-pedido"]').should(elemento => {
          expect(elemento.text().trim().toUpperCase())
            .to.eq('ENTREGUE');
        });

        cy.task(
          'analise:pedidoControle',
          'EM PROCESSAMENTO'
        ).then(atual => {
          expect(atual.status).to.eq('ENTREGUE');
        });

        cy.task('analise:totais', {
          categorias: ['Fantasia'],
          statusEsperados: [
            {
              pedidoId: controle.id,
              status: 'ENTREGUE'
            }
          ]
        }).then(esperado => {
          const indice = c.meses.indexOf(controle.mes);

          const acrescimo =
            esperado.series[0].valoresCentavos[indice]
            - antes.series[0].valoresCentavos[indice];

          expect(acrescimo).to.eq(controle.valorCentavos);

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
});