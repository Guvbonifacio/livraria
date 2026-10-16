describe('RN0031 - Estoque insuficiente', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'estoque').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('recusa excesso na adição e na alteração', () => {
    const excesso = c.livros[0].estoque + 1;

    cy.request({
      method: 'POST',
      url: '/carrinho/adicionar',
      form: true,
      followRedirect: false,
      body: {
        livroId: c.livros[0].id,
        quantidade: excesso
      }
    }).its('status').should('eq', 302);

    cy.visit('/carrinho');

    cy.get('[data-cy="erros-carrinho"]')
      .should('contain.text', 'acima do estoque');

    cy.get('[data-cy="carrinho-vazio"]')
      .should('be.visible');

    cy.visit('/');

    cy.contains('.card', 'O Hobbit').within(() => {
      cy.contains('button', 'Adicionar').click();
    });

    cy.get('[data-cy="item-carrinho"]').within(() => {
      cy.get('[name="quantidade"]')
        .invoke('removeAttr', 'max')
        .clear()
        .type(String(excesso));

      cy.contains('button', 'Atualizar').click();
    });

    cy.get('[data-cy="erros-carrinho"]')
      .should('contain.text', 'acima do estoque');

    cy.get('[data-cy="item-carrinho"] [name="quantidade"]')
      .should('have.value', '1');
  });
});