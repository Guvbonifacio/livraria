describe('RF0031 - Carrinho com múltiplos itens', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'padrao').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('adiciona dois livros e recalcula os valores ao alterar quantidade', () => {
    cy.visit('/');

    cy.contains('.card', 'O Hobbit').within(() => {
      cy.contains('button', 'Adicionar').click();
    });

    cy.visit('/');

    cy.contains('.card', 'Dom Casmurro').within(() => {
      cy.contains('button', 'Adicionar').click();
    });

    cy.get('[data-cy="item-carrinho"]')
      .should('have.length', 2);

    cy.get(`[data-livro-id="${c.livros[0].id}"]`).within(() => {
      cy.get('[name="quantidade"]').clear().type('2');
      cy.contains('button', 'Atualizar').click();
    });

    cy.get(`[data-livro-id="${c.livros[0].id}"]`).within(() => {
      cy.get('[name="quantidade"]')
        .should('have.value', '2');

      cy.get('[data-cy="subtotal-item"]')
        .should('contain.text', '60,00');
    });

    cy.get(
      `[data-livro-id="${c.livros[1].id}"] [name="quantidade"]`
    ).should('have.value', '1');

    const total = (
      (6000 + c.livros[1].preco_centavos) / 100
    ).toFixed(2).replace('.', ',');

    cy.get('[data-cy="total-carrinho"]')
      .should('contain.text', total);
  });
});