describe('RNF0044 - Recomendação pelo modelo treinado', () => {
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
  it('usa o recomendador, preserva o histórico e registra o voto uma única vez', () => {
    cy.get('[data-cy="texto-chat"]').type('Recomende livros para mim');
    cy.get('[data-cy="enviar-chat"]').click();
    cy.get('[data-cy="livro-recomendado"]').should('have.length', 3);
    cy.get('[data-cy="provedor-chat"]').should('contain.text', 'FALSO');
    cy.get('[data-cy="mensagem-chat"][data-papel="ASSISTENTE"]').last().then($mensagem => {
      const id = Number($mensagem.attr('data-mensagem-id'));
      cy.task('chatbot:estado', id).then(estado => {
        expect(estado.recomendados).to.have.length(3);
        expect(estado.feedback).to.have.length(0);
      });
      cy.wrap($mensagem).find('[data-cy="feedback-util"]').click();
      cy.get('[data-cy="feedback-registrado"]').should('contain.text', 'útil');
      cy.request({ method: 'POST', url: `/chat/${id}/feedback`, form: true, body: { util: true } });
      cy.task('chatbot:estado', id).its('feedback').should('deep.equal', [{ util: 1 }]);
    });
    cy.reload();
    cy.get('[data-cy="livro-recomendado"]').should('have.length', 3);
    cy.get('[data-cy="mensagem-chat"]').should('have.length', 2);
  });
});