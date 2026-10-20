// Cenário 5 da entrega: compra recusada pela operadora. O pedido fica
// REPROVADA, nenhum item sai do estoque e os itens são desbloqueados.
// Requisitos: RN0037, RN0038 e RN0028 ("todo item que faça parte de uma
// compra não aprovada deve ser desbloqueado e mantido em estoque").

describe('RN0028 - Compra reprovada mantém o estoque', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'padrao').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('reprova o pagamento, preserva o estoque e desbloqueia os itens', () => {
    const livro = c.livros[0];

    cy.task('vendas:estoque', livro.id).then(antes => {
      cy.visit('/');

      cy.contains('.card', 'O Hobbit').within(() => {
        cy.contains('button', 'Adicionar').click();
      });

      cy.contains('a', 'Finalizar Compra').click();

      cy.get(
        `[name="enderecoId"][value="${c.enderecoId}"]`
      ).check();

      cy.get('button[name="acao"][value="continuar"]').click();

      cy.location('pathname')
        .should('eq', '/carrinho/checkout/pagamento');

      cy.get(
        `[name="valoresCartoes[${c.cartaoId}]"]`
      ).type('47.00');

      cy.get('button[name="acao"][value="finalizar"]').click();

      cy.location('pathname')
        .should('match', /^\/pedidos\/\d+\/confirmacao$/);

      cy.location('pathname').then(caminho => {
        const id = Number(caminho.split('/')[2]);

        // O administrador simula a recusa da operadora.
        cy.visit(`/admin/pedidos/${id}`);

        cy.get('[data-cy="status-pedido"]')
          .should('have.attr', 'data-status', 'EM PROCESSAMENTO');

        cy.get('[data-cy="simular-recusa"]').click();

        cy.get('[data-cy="sucesso-processamento"]')
          .should('contain.text', 'REPROVADA');

        cy.get('[data-cy="erros-processamento"]')
          .should('not.exist');

        cy.get('[data-cy="status-pedido"]')
          .should('have.attr', 'data-status', 'REPROVADA');

        cy.task('vendas:pedido', id).then(resultado => {
          expect(resultado.pedido.status).to.eq('REPROVADA');

          expect(resultado.pagamentos[0].status)
            .to.eq('RECUSADO');
        });

        // RN0028: nenhuma unidade saiu do estoque...
        cy.task('vendas:estoque', livro.id).then(depois => {
          expect(depois.total).to.eq(antes.total);

          expect(depois.lotes.map(l => l.quantidade))
            .to.deep.eq(antes.lotes.map(l => l.quantidade));

          // ...e os itens ficaram livres para outros clientes.
          expect(depois.reservas).to.eq(0);
        });

        // Uma compra já processada não volta a ser processada.
        cy.get('[data-cy="simular-aceite"]').should('not.exist');
        cy.get('[data-cy="simular-recusa"]').should('not.exist');
      });
    });
  });
});
