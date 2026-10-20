describe('RF0042 / RF0043 / RN0046 - Autorizar troca', () => {
  it('autoriza a troca integral e notifica o cliente uma única vez', () => {
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

          cy.visit(`/admin/pedidos/${id}`);

          cy.get(
            'button[name="novoStatus"][value="APROVADA"]'
          ).click();

          cy.get('[data-cy="status-pedido"]').should($elemento => {
            expect($elemento.text().trim().toUpperCase())
              .to.eq('APROVADA');
          });

          cy.get('[data-cy="despachar-pedido"]').click();

          cy.get('[data-cy="status-pedido"]').should($elemento => {
            expect($elemento.text().trim().toUpperCase())
              .to.eq('EM TRANSPORTE');
          });

          cy.get('[data-cy="entregar-pedido"]').click();

          cy.get('[data-cy="status-pedido"]').should($elemento => {
            expect($elemento.text().trim().toUpperCase())
              .to.eq('ENTREGUE');
          });

          cy.visit(`/pedidos/${id}`);

          cy.get('[data-cy="solicitar-troca"]').click();
          cy.get('[data-cy="selecionar-item-troca"]').check();
          cy.get('[data-cy="confirmar-troca"]').click();

          cy.get('[data-cy="sucesso-troca"]')
            .should('be.visible');

          cy.task('trocas:consultar', id).then(antes => {
            expect(antes.trocas).to.have.length(1);
            expect(antes.pedido.status).to.eq('EM TROCA');
            expect(antes.notificacoes).to.have.length(0);

            const trocaId = antes.trocas[0].id;

            cy.visit('/admin/trocas');

            cy.get(
              `[data-cy="linha-troca"][data-troca-id="${trocaId}"]`
            ).within(() => {
              cy.contains('a', 'Ver detalhes').click();
            });

            cy.get('[data-cy="status-troca"]')
              .should('contain.text', 'EM TROCA');

            cy.get('[data-cy="autorizar-troca"]').click();

            cy.get('[data-cy="sucesso-autorizacao"]')
              .should('be.visible');

            cy.get('[data-cy="status-troca"]')
              .should('contain.text', 'TROCA AUTORIZADA');

            cy.get('[data-cy="autorizar-troca"]')
              .should('not.exist');

            cy.task('trocas:consultar', id).then(depois => {
              expect(depois.pedido.status)
                .to.eq('TROCA AUTORIZADA');

              expect(depois.trocas[0].status)
                .to.eq('TROCA AUTORIZADA');

              expect(depois.trocas[0].data_autorizacao)
                .to.be.a('string').and.not.be.empty;

              expect(depois.notificacoes).to.have.length(1);

              const mensagem =
                `A troca #${trocaId} do pedido #${id} foi autorizada.`;

              expect(depois.notificacoes[0]).to.include({
                cliente_id: 1,
                mensagem,
                lida: 0
              });

              expect(
                depois.logsTroca.filter(
                  log => log.operacao === 'UPDATE'
                )
              ).to.have.length(1);

              expect(
                depois.logsNotificacao.filter(
                  log => log.operacao === 'INSERT'
                )
              ).to.have.length(1);

              expect(depois.estoque).to.deep.eq(antes.estoque);
              expect(depois.cupons).to.deep.eq(antes.cupons);

              cy.visit('/pedidos');

              cy.contains('[data-cy="notificacao"]', mensagem)
                .should('be.visible');

              // Repetir o POST não pode autorizar ou notificar de novo.
              cy.request({
                method: 'POST',
                url: `/admin/trocas/${trocaId}/analisar`,
                form: true,
                followRedirect: false,
                body: { decisao: 'ACEITAR' }
              }).its('status').should('eq', 302);

              cy.task('trocas:consultar', id).then(repetido => {
                expect(repetido.notificacoes).to.have.length(1);

                expect(repetido.trocas[0].data_autorizacao)
                  .to.eq(depois.trocas[0].data_autorizacao);

                expect(
                  repetido.logsTroca.filter(
                    log => log.operacao === 'UPDATE'
                  )
                ).to.have.length(1);
              });
            });
          });
        });
    });
  });
});