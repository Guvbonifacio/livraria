describe('RF0044 / RN0042 / RF0054 - Receber troca', () => {
  it('recebe com reentrada e impede duplicidade', () => {
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

          cy.get('[data-cy="sucesso-troca"]').should('be.visible');

          cy.task('trocas:consultar', id).then(antes => {
            expect(antes.trocas).to.have.length(1);
            expect(antes.pedido.status).to.eq('EM TROCA');

            const trocaId = antes.trocas[0].id;

            cy.visit('/admin/trocas');

            cy.get(
              `[data-cy="linha-troca"][data-troca-id="${trocaId}"]`
            ).within(() => {
              cy.contains('a', 'Ver detalhes').click();
            });

            cy.get('[data-cy="autorizar-troca"]').click();

            cy.get(
              '[name="retornaEstoque"][value="true"]'
            ).check();

            cy.get('[data-cy="receber-troca"]').click();

            cy.get('[data-cy="sucesso-conclusao"]')
              .should('be.visible');

            cy.get('[data-cy="status-troca"]')
              .should('contain.text', 'TROCADO');

            cy.task('trocas:consultar', id).then(depois => {
              const troca = depois.trocas[0];

              expect(troca.status).to.eq('TROCADO');
              expect(troca.retorna_estoque).to.eq(1);
              expect(troca.data_recebimento)
                .to.be.a('string').and.not.be.empty;

              expect(depois.pedido.status).to.eq('TROCADO');

              for (const anterior of antes.estoque) {
                const atual = depois.estoque.find(
                  e => e.livro_id === anterior.livro_id
                );

                const devolvido = antes.itens
                  .filter(i => i.livro_id === anterior.livro_id)
                  .reduce(
                    (soma, item) => soma + item.quantidade,
                    0
                  );

                expect(atual.quantidade)
                  .to.eq(anterior.quantidade + devolvido);
              }

              const cupom = depois.cupons.find(
                c => c.id === troca.cupom_id
              );

              expect(cupom).to.include({
                tipo: 'TROCA',
                cliente_id: 1,
                pedido_origem_id: id,
                valor_centavos: 3000,
                ativo: 1
              });

              // Repetir o recebimento não pode duplicar a entrada.
              cy.request({
                method: 'POST',
                url: `/admin/trocas/${trocaId}/receber`,
                form: true,
                followRedirect: false,
                body: { retornaEstoque: 'true' }
              }).its('status').should('eq', 302);

              // Tampouco pode emitir outro cupom.
              cy.request({
                method: 'POST',
                url: `/admin/trocas/${trocaId}/finalizar`,
                form: true,
                followRedirect: false
              }).its('status').should('eq', 302);

              cy.task('trocas:consultar', id).then(repetido => {
                expect(repetido.estoque)
                  .to.deep.eq(depois.estoque);

                expect(repetido.cupons)
                  .to.deep.eq(depois.cupons);

                expect(repetido.logsTroca)
                  .to.deep.eq(depois.logsTroca);
              });
            });
          });
        });
    });
  });
});