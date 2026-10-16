describe('RF0038 - Compra com dados cadastrados', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'padrao').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('compra com endereço e cartão existentes', () => {
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

    cy.contains('.badge', 'EM PROCESSAMENTO')
      .should('be.visible');

    cy.location('pathname').then(caminho => {
      const id = Number(caminho.split('/')[2]);

      cy.task('vendas:pedido', id).then(resultado => {
        expect(resultado.pedido.status)
          .to.eq('EM PROCESSAMENTO');

        expect(resultado.pedido.total_centavos).to.eq(4700);

        const endereco = JSON.parse(
          resultado.pedido.endereco_entrega_json
        );

        expect(endereco.id).to.eq(c.enderecoId);
        expect(resultado.itens).to.have.length(1);
        expect(resultado.pagamentos).to.have.length(1);

        expect(resultado.pagamentos[0]).to.include({
          cartao_id: c.cartaoId,
          valor_centavos: 4700,
          status: 'PENDENTE'
        });

        cy.reload();

        cy.task('vendas:pedido', id)
          .its('quantidadePedidos')
          .should('eq', 1);
      });
    });
  });
});