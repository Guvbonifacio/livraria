describe('RN0043 - Troca somente após entrega', () => {
  it('bloqueia a tela e o POST para compra em processamento', () => {
    cy.task('vendas:preparar', 'padrao').then(cenario => {
      cy.visit('/');

      cy.contains('.card', cenario.livros[0].titulo).within(() => {
        cy.contains('button', 'Adicionar').click();
      });

      cy.contains('a', 'Finalizar Compra').click();

      cy.get(
        `[name="enderecoId"][value="${cenario.enderecoId}"]`
      ).check();

      cy.get('button[name="acao"][value="continuar"]').click();

      cy.get('[data-cy="total-checkout"]')
        .invoke('attr', 'data-valor')
        .then(total => {
          cy.get(
            `[name="valoresCartoes[${cenario.cartaoId}]"]`
          ).clear().type(total);
        });

      cy.get('button[name="acao"][value="finalizar"]').click();

      cy.location('pathname')
        .should('match', /^\/pedidos\/\d+\/confirmacao$/)
        .then(caminho => {
          const id = Number(caminho.split('/')[2]);

          cy.visit(`/pedidos/${id}`);

          cy.get('[data-cy="status-pedido"]')
            .should('have.text', 'EM PROCESSAMENTO');

          cy.get('[data-cy="solicitar-troca"]')
            .should('not.exist');

          // Digitar a URL diretamente também deve ser bloqueado.
          cy.request({
            url: `/pedidos/${id}/troca`,
            followRedirect: false
          }).then(resposta => {
            expect(resposta.status).to.eq(302);
            expect(resposta.headers.location)
              .to.include(`/pedidos/${id}`);
          });

          cy.task('trocas:consultar', id).then(antes => {
            const item = antes.itens[0];

            // O Service precisa bloquear mesmo sem passar pela tela.
            cy.request({
              method: 'POST',
              url: `/pedidos/${id}/troca`,
              form: true,
              followRedirect: false,
              body: {
                itensIds: String(item.id),
                [`quantidades[${item.id}]`]: '1'
              }
            }).its('status').should('eq', 302);

            cy.visit(`/pedidos/${id}`);

            cy.contains('.alert-danger', 'ENTREGUE')
              .should('be.visible');

            cy.task('trocas:consultar', id).then(depois => {
              expect(depois.pedido.status)
                .to.eq('EM PROCESSAMENTO');

              expect(depois.trocas).to.have.length(0);
              expect(depois.itensTroca).to.have.length(0);
              expect(depois.logsTroca).to.have.length(0);
              expect(depois.notificacoes).to.have.length(0);

              expect(depois.estoque)
                .to.deep.eq(antes.estoque);
            });
          });
        });
    });
  });
});