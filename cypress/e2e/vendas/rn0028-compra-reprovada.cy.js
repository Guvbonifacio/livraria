// Cenário 5: compra recusada pela operadora.
// RN0028: preserva o estoque e libera as reservas dos itens.

describe('RN0028 - Compra reprovada mantém o estoque', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'padrao').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('reprova o pagamento, preserva o estoque e desbloqueia os itens', () => {
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

          // Assume ADMIN antes de acessar o pedido administrativo.
          cy.request({
            method: 'POST',
            url: '/sessao/perfil',
            form: true,
            body: { perfil: 'ADMIN' }
          });

          cy.visit(`/admin/pedidos/${id}`);

          cy.get('[data-cy="status-pedido"]')
            .should('have.attr', 'data-status', 'EM PROCESSAMENTO');

          // Confirma que a compra possui reserva antes da recusa.
          cy.task('vendas:estoque', livro.id).then(pendente => {
            expect(pendente.reservas).to.be.greaterThan(0);
          });

          cy.get('[data-cy="simular-recusa"]').click();

          cy.get('[data-cy="sucesso-processamento"]')
            .should('contain.text', 'REPROVADA');

          cy.get('[data-cy="erros-processamento"]')
            .should('not.exist');

          cy.get('[data-cy="status-pedido"]')
            .should('have.attr', 'data-status', 'REPROVADA');

          cy.task('vendas:pedido', id).then(resultado => {
            expect(resultado.pedido.status).to.eq('REPROVADA');

            expect(resultado.pagamentos).to.have.length(1);

            expect(resultado.pagamentos[0].status)
              .to.eq('RECUSADO');
          });

          // Nenhuma unidade sai do estoque e a reserva é liberada.
          cy.task('vendas:estoque', livro.id).then(depois => {
            expect(depois.total).to.eq(antes.total);

            expect(depois.lotes.map(lote => lote.quantidade))
              .to.deep.eq(
                antes.lotes.map(lote => lote.quantidade)
              );

            expect(depois.reservas).to.eq(0);
          });

          // Uma compra processada não pode ser processada novamente.
          cy.get('[data-cy="simular-aceite"]')
            .should('not.exist');

          cy.get('[data-cy="simular-recusa"]')
            .should('not.exist');

          cy.reload();

          cy.get('[data-cy="status-pedido"]')
            .should('have.attr', 'data-status', 'REPROVADA');
        });
    });
  });
});