describe('RNF0044 - Preferências declaradas com histórico', () => {
  beforeEach(() => {
    cy.task('chatbot:preparar');
    cy.visit('/clientes/1');
  });
  it('grava novos eventos ao marcar e desmarcar sem alterar os anteriores', () => {
    let anteriores;
    cy.task('chatbot:estado', 0).then(estado => { anteriores = estado.preferencias; });
    cy.get('[data-cy="preferencia-categoria"]').check();
    cy.get('[data-cy="salvar-preferencias"]').click();
    cy.get('[data-cy="sucesso-preferencias"]').should('be.visible');
    cy.get('[data-cy="preferencia-categoria"]').should('be.checked').uncheck();
    cy.get('[data-cy="salvar-preferencias"]').click();
    cy.get('[data-cy="preferencia-categoria"]').should('not.be.checked');
    cy.task('chatbot:estado', 0).then(estado => {
      expect(estado.preferencias.length).to.be.greaterThan(anteriores.length);
      anteriores.forEach(linha => expect(estado.preferencias).to.deep.include(linha));
      const ultimos = new Map();
      estado.preferencias.forEach(p => ultimos.set(p.categoria_id, p));
      [...ultimos.values()].forEach(p => expect(p.peso_declarado).to.equal(0));
    });
  });
});