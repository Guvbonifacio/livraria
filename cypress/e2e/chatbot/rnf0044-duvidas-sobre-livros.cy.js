describe('RNF0044 - Autor e disponibilidade do catálogo', () => {
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
  it('consulta detalhes por ferramenta e informa o autor correto', () => {
    cy.get('[data-cy="texto-chat"]').type('autor de O Poder do Hábito');
    cy.get('[data-cy="enviar-chat"]').click();
    cy.get('[data-cy="mensagem-chat"][data-papel="ASSISTENTE"]').last()
      .should('contain.text', 'Charles Duhigg').and('contain.text', 'ISBN:');
    cy.get('[data-cy="texto-chat"]').type('estoque de Duna');
    cy.get('[data-cy="enviar-chat"]').click();
    cy.get('[data-cy="mensagem-chat"][data-papel="ASSISTENTE"]').last()
      .should('contain.text', 'indisponível');
  });
});