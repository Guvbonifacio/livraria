describe('RN0034 - Pagamento com múltiplos cartões', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'multiplosCartoes').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('recusa parcela abaixo do mínimo e aceita R$ 10 mais R$ 37', () => {
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
    ).type('9.99');

    cy.get(
      `[name="valoresCartoes[${c.segundoCartaoId}]"]`
    ).type('37.01');

    cy.get('button[name="acao"][value="validar"]').click();

    cy.get('[role="alert"]').should(
      'contain.text',
      'O valor mínimo por cartão é R$ 10,00.'
    );

    cy.task('vendas:pedido', -1)
      .its('quantidadePedidos')
      .should('eq', 0);

    cy.get(
      `[name="valoresCartoes[${c.cartaoId}]"]`
    ).clear().type('10.00');

    cy.get(
      `[name="valoresCartoes[${c.segundoCartaoId}]"]`
    ).clear().type('37.00');

    cy.get('button[name="acao"][value="finalizar"]').click();

    cy.location('pathname')
      .should('match', /^\/pedidos\/\d+\/confirmacao$/);

    cy.contains('.badge', 'EM PROCESSAMENTO')
      .should('be.visible');

    cy.location('pathname').then(caminho => {
      const id = Number(caminho.split('/')[2]);

      cy.task('vendas:pedido', id).then(resultado => {
        expect(resultado.pedido.status)
          .to.eq('EM PROCESSAMENTO');

        const cartoes = resultado.pagamentos.filter(
          p => p.forma === 'CARTAO'
        );

        expect(cartoes).to.have.length(2);

        expect(cartoes.map(p => p.cartao_id))
          .to.have.members([
            c.cartaoId,
            c.segundoCartaoId
          ]);

        expect(cartoes.map(p => p.valor_centavos))
          .to.have.members([1000, 3700]);

        expect(
          cartoes.every(p => p.valor_centavos >= 1000)
        ).to.eq(true);

        expect(
          cartoes.reduce(
            (soma, p) => soma + p.valor_centavos,
            0
          )
        ).to.eq(4700);
      });
    });
  });
});