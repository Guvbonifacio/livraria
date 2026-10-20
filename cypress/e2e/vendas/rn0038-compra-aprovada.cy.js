// Cenário 1 da entrega: compra paga com um cartão, aprovada pelo
// administrador, com baixa de estoque.
// Requisitos: RN0037 (validação), RN0038 (APROVADA/REPROVADA),
// RF0053 e RN0028 (baixa somente da compra efetivada).

describe('RN0038 - Compra aprovada com um cartão', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'padrao').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('aprova o pagamento, dá baixa no estoque e libera o bloqueio', () => {
    const livro = c.livros[0];

    // Estoque antes da compra, para comparar ao final.
    cy.task('vendas:estoque', livro.id).then(antes => {
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

      cy.location('pathname').then(caminho => {
        const id = Number(caminho.split('/')[2]);

        // O pedido nasce EM PROCESSAMENTO e o estoque ainda não mudou:
        // a RN0028 só admite baixa da compra efetivada.
        cy.task('vendas:pedido', id).then(resultado => {
          expect(resultado.pedido.status)
            .to.eq('EM PROCESSAMENTO');

          expect(resultado.pagamentos).to.have.length(1);

          expect(resultado.pagamentos[0]).to.include({
            forma: 'CARTAO',
            cartao_id: c.cartaoId,
            valor_centavos: 4700,
            status: 'PENDENTE'
          });
        });

        cy.task('vendas:estoque', livro.id)
          .its('total')
          .should('eq', antes.total);

        // O administrador simula o aceite da operadora.
        cy.visit(`/admin/pedidos/${id}`);

        cy.get('[data-cy="status-pedido"]')
          .should('have.attr', 'data-status', 'EM PROCESSAMENTO');

        cy.get('[data-cy="simular-aceite"]').click();

        cy.get('[data-cy="sucesso-processamento"]')
          .should('contain.text', 'APROVADA');

        cy.get('[data-cy="erros-processamento"]')
          .should('not.exist');

        cy.get('[data-cy="status-pedido"]')
          .should('have.attr', 'data-status', 'APROVADA');

        // RN0038: pedido e pagamento mudam juntos.
        cy.task('vendas:pedido', id).then(resultado => {
          expect(resultado.pedido.status).to.eq('APROVADA');

          expect(resultado.pagamentos[0].status)
            .to.eq('APROVADO');
        });

        // RF0053: exatamente uma unidade saiu do estoque.
        cy.task('vendas:estoque', livro.id).then(depois => {
          expect(depois.total).to.eq(antes.total - 1);

          // RN0044: o bloqueio da compra não sobrevive ao processamento.
          expect(depois.reservas).to.eq(0);
        });
      });
    });
  });
});
