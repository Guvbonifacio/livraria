describe('RF0041 / RN0041 - Solicitar troca', () => {
  [
    { nome: 'um item da compra', integral: false },
    { nome: 'a compra inteira', integral: true }
  ].forEach(caso => {
    it(`solicita troca de ${caso.nome}`, () => {
      cy.task('vendas:preparar', 'padrao').then(cenario => {
        // Dois livros diferentes, uma unidade de cada.
        cenario.livros.forEach(livro => {
          cy.visit('/');

          cy.contains('.card', livro.titulo).within(() => {
            cy.contains('button', 'Adicionar').click();
          });
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

            cy.task('trocas:consultar', id).then(antes => {
              expect(antes.itens).to.have.length(2);

              const escolhidos = caso.integral
                ? antes.itens
                : [antes.itens[0]];

              // A solicitação começa na consulta real do cliente.
              cy.visit('/pedidos');

              cy.get(`a[href="/pedidos/${id}"]`).click();

              cy.get('[data-cy="solicitar-troca"]').click();

              escolhidos.forEach(item => {
                cy.get(`tr[data-item-id="${item.id}"]`)
                  .within(() => {
                    cy.get('[data-cy="selecionar-item-troca"]')
                      .check();

                    cy.get('[data-cy="quantidade-troca"]')
                      .clear()
                      .type(String(item.quantidade));
                  });
              });

              // Motivo é opcional.
              cy.get('#motivo').should('have.value', '');

              cy.get('[data-cy="confirmar-troca"]').click();

              cy.get('[data-cy="sucesso-troca"]')
                .should('contain.text', 'EM TROCA');

              cy.task('trocas:consultar', id).then(depois => {
                expect(depois.trocas).to.have.length(1);
                expect(depois.trocas[0].status).to.eq('EM TROCA');

                expect(depois.pedido.status).to.eq(
                  caso.integral ? 'EM TROCA' : 'ENTREGUE'
                );

                expect(depois.itensTroca)
                  .to.have.length(escolhidos.length);

                escolhidos.forEach(item => {
                  const registrado = depois.itensTroca.find(
                    linha => linha.item_pedido_id === item.id
                  );

                  expect(registrado).to.exist;
                  expect(registrado.quantidade)
                    .to.eq(item.quantidade);
                });

                expect(
                  depois.logsTroca.filter(
                    log => log.operacao === 'INSERT'
                  )
                ).to.have.length(1);

                expect(
                  depois.logsItensTroca.filter(
                    log => log.operacao === 'INSERT'
                  )
                ).to.have.length(escolhidos.length);

                expect(depois.estoque).to.deep.eq(antes.estoque);
                expect(depois.cupons).to.deep.eq(antes.cupons);
              });

              if (!caso.integral) {
                const item = escolhidos[0];

                // Uma segunda solicitação do mesmo item é recusada.
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

                cy.contains('.alert-danger', 'já possui')
                  .should('be.visible');

                cy.task('trocas:consultar', id).then(resultado => {
                  expect(resultado.trocas).to.have.length(1);
                  expect(resultado.itensTroca).to.have.length(1);
                });
              }
            });
          });
      });
    });
  });
});