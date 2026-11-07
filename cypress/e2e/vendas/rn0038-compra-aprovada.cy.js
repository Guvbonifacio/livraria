// Cenário 1: compra paga com um cartão e aprovada pelo administrador.
// Requisitos: RN0037, RN0038, RF0053 e RN0028.

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

    cy.task('vendas:estoque', livro.id).then(antes => {
      cy.visit('/');

      cy.contains('.card', livro.titulo).within(() => {
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
      ).clear().type('47.00');

      cy.get('button[name="acao"][value="finalizar"]').click();

      cy.location('pathname')
        .should('match', /^\/pedidos\/\d+\/confirmacao$/)
        .then(caminho => {
          const id = Number(caminho.split('/')[2]);

          // O pedido nasce pendente, sem baixa de estoque.
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

          cy.task('vendas:estoque', livro.id).then(pendente => {
            expect(pendente.total).to.eq(antes.total);
            expect(pendente.reservas).to.be.greaterThan(0);
          });

          // Assume ADMIN antes de processar a compra.
          cy.request({
            method: 'POST',
            url: '/sessao/perfil',
            form: true,
            body: { perfil: 'ADMIN' }
          });

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

          // Pedido e pagamento são aprovados juntos.
          cy.task('vendas:pedido', id).then(resultado => {
            expect(resultado.pedido.status).to.eq('APROVADA');

            expect(resultado.pagamentos).to.have.length(1);

            expect(resultado.pagamentos[0].status)
              .to.eq('APROVADO');
          });

          // Exatamente uma unidade sai do estoque.
          cy.task('vendas:estoque', livro.id).then(depois => {
            expect(depois.total).to.eq(antes.total - 1);

            // A reserva é liberada após o processamento.
            expect(depois.reservas).to.eq(0);
          });

          cy.get('[data-cy="simular-aceite"]')
            .should('not.exist');

          cy.get('[data-cy="simular-recusa"]')
            .should('not.exist');

          // Confirma que a aprovação foi persistida.
          cy.reload();

          cy.get('[data-cy="status-pedido"]')
            .should('have.attr', 'data-status', 'APROVADA');
        });
    });
  });
});