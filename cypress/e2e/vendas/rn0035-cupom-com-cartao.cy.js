describe('RN0035 - Cupom com saldo no cartão', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'cupomCartao').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('usa cupom de R$ 42 e permite saldo de R$ 5 no cartão', () => {
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
      `input[type="checkbox"][name="cuponsIds"]` +
      `[value="${c.cuponsIds[0]}"]`
    ).check();

    cy.get(
      `[name="valoresCartoes[${c.cartaoId}]"]`
    ).type('5.00');

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

        expect(resultado.pagamentos).to.have.length(2);

        const cupom = resultado.pagamentos.find(
          p => p.forma === 'CUPOM_TROCA'
        );

        expect(cupom).to.include({
          cupom_id: c.cuponsIds[0],
          valor_centavos: 4200
        });

        const cartao = resultado.pagamentos.find(
          p => p.forma === 'CARTAO'
        );

        expect(cartao).to.include({
          cartao_id: c.cartaoId,
          valor_centavos: 500
        });

        const utilizado = resultado.cupons.find(
          p => p.id === c.cuponsIds[0]
        );

        expect(utilizado.ativo).to.eq(0);
        expect(resultado.emitidos).to.have.length(0);
      });
    });
  });
});