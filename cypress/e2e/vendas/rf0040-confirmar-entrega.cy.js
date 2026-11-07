describe('RF0040 - Confirmar entrega', () => {
  it('confirma a entrega sem alterar estoque ou pagamentos', () => {
    cy.task('vendas:preparar', 'padrao').then(cenario => {
      cy.visit('/');

      cy.contains('.card', 'O Hobbit').within(() => {
        cy.contains('button', 'Adicionar').click();
      });

      cy.contains('a', 'Finalizar Compra').click();

      cy.get(
        `[name="enderecoId"][value="${cenario.enderecoId}"]`
      ).check();

      cy.get(
        'button[name="acao"][value="continuar"]'
      ).click();

      cy.location('pathname').should(
        'eq',
        '/carrinho/checkout/pagamento'
      );

      cy.get(
        `[name="valoresCartoes[${cenario.cartaoId}]"]`
      ).clear().type('47.00');

      cy.get(
        'button[name="acao"][value="finalizar"]'
      ).click();

      cy.location('pathname')
        .should('match', /^\/pedidos\/\d+\/confirmacao$/)
        .then(caminho => {
          const id = Number(caminho.split('/')[2]);

          // Assume ADMIN antes das ações administrativas.
          cy.request({
            method: 'POST',
            url: '/sessao/perfil',
            form: true,
            body: { perfil: 'ADMIN' }
          });

          cy.visit(`/admin/pedidos/${id}`);

          cy.get(
            'button[name="novoStatus"][value="APROVADA"]'
          ).click();

          cy.get('[data-cy="status-pedido"]')
            .should('have.attr', 'data-status', 'APROVADA');

          // A entrega não pode ser confirmada antes do despacho.
          cy.get('[data-cy="entregar-pedido"]')
            .should('not.exist');

          cy.get('[data-cy="despachar-pedido"]').click();

          cy.get('[data-cy="status-pedido"]')
            .should('have.attr', 'data-status', 'EM TRANSPORTE');

          cy.task('vendas:entrega', id).then(antes => {
            expect(antes.pedido.status)
              .to.eq('EM TRANSPORTE');

            cy.get('[data-cy="entregar-pedido"]').click();

            cy.get('[data-cy="status-pedido"]')
              .should('have.attr', 'data-status', 'ENTREGUE');

            cy.get('[data-cy="despachar-pedido"]')
              .should('not.exist');

            cy.get('[data-cy="entregar-pedido"]')
              .should('not.exist');

            cy.task('vendas:entrega', id).then(depois => {
              expect(depois.pedido.status).to.eq('ENTREGUE');

              expect(depois.estoque)
                .to.deep.eq(antes.estoque);

              expect(depois.pagamentos)
                .to.deep.eq(antes.pagamentos);

              const logsEntrega = depois.logs
                .filter(log => log.operacao === 'UPDATE')
                .map(log => JSON.parse(log.dados))
                .filter(dados =>
                  dados.status === 'ENTREGUE'
                );

              expect(logsEntrega).to.deep.eq([
                {
                  statusAnterior: 'EM TRANSPORTE',
                  status: 'ENTREGUE'
                }
              ]);
            });

            // Confirma que a entrega foi persistida.
            cy.reload();

            cy.get('[data-cy="status-pedido"]')
              .should('have.attr', 'data-status', 'ENTREGUE');

            cy.get('[data-cy="entregar-pedido"]')
              .should('not.exist');
          });
        });
    });
  });
});