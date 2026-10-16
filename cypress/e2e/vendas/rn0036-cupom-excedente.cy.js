describe('RN0036 - Cupom do excedente', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'excedente').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('paga R$ 47 com R$ 55 em cupons e emite R$ 8 de troca', () => {
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

    c.cuponsIds.forEach(id => {
      cy.get(
        `input[type="checkbox"][name="cuponsIds"][value="${id}"]`
      ).check();
    });

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

        expect(resultado.pagamentos).to.have.length(2);

        expect(
          resultado.pagamentos.every(
            p => p.forma === 'CUPOM_TROCA'
          )
        ).to.eq(true);

        expect(
          resultado.pagamentos.map(p => p.cupom_id)
        ).to.have.members(c.cuponsIds);

        expect(
          resultado.pagamentos.reduce(
            (soma, p) => soma + p.valor_centavos,
            0
          )
        ).to.eq(5500);

        expect(resultado.emitidos).to.have.length(1);

        expect(resultado.emitidos[0]).to.include({
          tipo: 'TROCA',
          cliente_id: 1,
          pedido_origem_id: id,
          valor_centavos: 800,
          ativo: 1
        });

        const validade = new Date(
          `${resultado.pedido.data_pedido.slice(0, 10)}T12:00:00Z`
        );

        validade.setUTCDate(validade.getUTCDate() + 30);

        expect(resultado.emitidos[0].validade)
          .to.eq(validade.toISOString().slice(0, 10));

        expect(
          c.cuponsIds.every(idCupom =>
            resultado.cupons.find(
              p => p.id === idCupom
            ).ativo === 0
          )
        ).to.eq(true);

        cy.task('vendas:perfil')
          .its('nome')
          .should('eq', c.nome);
      });
    });
  });
});