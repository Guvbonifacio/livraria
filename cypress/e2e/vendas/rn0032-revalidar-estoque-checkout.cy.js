describe('RN0032 - Revalidar estoque na finalização', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'padrao').then(cenario => {
      c = cenario;

      cy.visit('/');

      cy.contains('.card', c.livros[0].titulo).within(() => {
        cy.get('[name="quantidade"]').clear().type('3');
        cy.contains('button', 'Adicionar').click();
      });

      cy.contains('a', 'Finalizar Compra').click();

      cy.get(
        `[name="enderecoId"][value="${c.enderecoId}"]`
      ).check();

      cy.get(
        'button[name="acao"][value="continuar"]'
      ).click();

      cy.get('[data-cy="total-checkout"]')
        .invoke('attr', 'data-valor')
        .then(total => {
          cy.get(
            `[name="valoresCartoes[${c.cartaoId}]"]`
          ).clear().type(total);
        });
    });
  });

  it('reduz a quantidade, notifica e só grava após nova confirmação', () => {
    // O estoque muda depois de o cliente chegar ao pagamento.
    cy.then(() => cy.task('vendas:alterarEstoqueRN0032', {
      livroId: c.livros[0].id,
      quantidade: 1
    }));

    cy.get(
      'button[name="acao"][value="finalizar"]'
    ).click();

    cy.location('pathname').should('eq', '/carrinho');

    cy.get('[data-cy="avisos-estoque"]')
      .should('contain.text', 'de 3 para 1');

    cy.get('[data-cy="item-carrinho"]')
      .should('have.length', 1)
      .within(() => {
        cy.get('[name="quantidade"]')
          .should('have.value', '1');

        cy.get('[data-cy="subtotal-item"]')
          .should('contain.text', '30,00');
      });

    cy.task('vendas:estadoRN0032').then(estado => {
      expect(estado.pedidos).to.eq(0);
      expect(estado.pagamentos).to.eq(0);

      expect(estado.notificacoes).to.have.length(1);
      expect(estado.notificacoes[0].mensagem)
        .to.contain('de 3 para 1');

      expect(estado.logs).to.have.length(1);
    });

    // O cliente revisa e confirma novamente com o novo total.
    cy.contains('a', 'Finalizar Compra').click();

    cy.get(
      'button[name="acao"][value="continuar"]'
    ).click();

    cy.get('[data-cy="total-checkout"]')
      .should('have.attr', 'data-valor', '47.00');

    cy.get(
      `[name="valoresCartoes[${c.cartaoId}]"]`
    ).clear().type('47.00');

    cy.get(
      'button[name="acao"][value="finalizar"]'
    ).click();

    cy.location('pathname')
      .should('match', /^\/pedidos\/\d+\/confirmacao$/);

    cy.task('vendas:estadoRN0032').then(estado => {
      expect(estado.pedidos).to.eq(1);

      // A confirmação seguinte não duplica o aviso.
      expect(estado.notificacoes).to.have.length(1);
    });
  });

  it('remove o item esgotado, alerta e não cria pedido com carrinho vazio', () => {
    cy.then(() => cy.task('vendas:alterarEstoqueRN0032', {
      livroId: c.livros[0].id,
      quantidade: 0
    }));

    cy.get(
      'button[name="acao"][value="finalizar"]'
    ).click();

    cy.location('pathname').should('eq', '/carrinho');

    cy.get('[data-cy="avisos-estoque"]')
      .should('contain.text', 'removido');

    cy.get('[data-cy="item-carrinho"]')
      .should('not.exist');

    cy.get('[data-cy="carrinho-vazio"]')
      .should('be.visible');

    cy.task('vendas:estadoRN0032').then(estado => {
      expect(estado.pedidos).to.eq(0);
      expect(estado.pagamentos).to.eq(0);
      expect(estado.notificacoes).to.have.length(1);
      expect(estado.logs).to.have.length(1);
    });

    // O aviso também permanece na lista real de notificações.
    cy.visit('/pedidos');

    cy.get('[data-cy="notificacao"]')
      .should('contain.text', 'removido');
  });
});