describe('RNF0044 - Busca pelo chat', () => {
  beforeEach(() => {
    cy.task('chatbot:preparar');
    cy.request({ method: 'POST', url: '/sessao/perfil', form: true, body: { perfil: 'ADMIN' } });
    cy.request('POST', '/admin/recomendacao/treinar');
    cy.request({ method: 'POST', url: '/sessao/perfil', form: true, body: { perfil: 'CLIENTE' } });
    cy.visit('/');
    // Falha ANTES de enviar mensagens se o servidor puder chamar uma API real.
    cy.get('[data-cy="chat-configuracao"]').should('have.attr', 'data-modo', 'falso');
    cy.get('[data-cy="abrir-chat"]').click();
    cy.get('[data-cy="painel-chat"]').should('have.class', 'show').and('not.have.class', 'collapsing');
  });
  it('consulta o catálogo real e mantém indisponibilidade sem recomendar Duna', () => {
    cy.get('[data-cy="texto-chat"]').type('buscar Duna');
    cy.get('[data-cy="enviar-chat"]').click();
    cy.get('[data-cy="mensagem-chat"][data-papel="ASSISTENTE"]').last()
      .should('contain.text', 'Duna').and('contain.text', 'indisponível');
    cy.get('[data-cy="livro-recomendado"]').should('not.exist');
  });
});