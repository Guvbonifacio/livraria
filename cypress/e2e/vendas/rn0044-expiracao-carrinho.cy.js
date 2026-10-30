describe('RN0044 / RN0045 / RNF0042 - Reserva e expiração do carrinho', () => {
  it('renova todos os prazos, avisa antes e exige readição dos produtos expirados', () => {
    cy.task('vendas:preparar', 'padrao').then(c => {
      cy.visit('/');

      cy.contains('.card', c.livros[0].titulo).within(() => {
        cy.get('[name="quantidade"]').clear().type('2');
        cy.contains('button', 'Adicionar').click();
      });

      cy.task('reserva:estado').then(primeiro => {
        expect(primeiro.reservas).to.have.length(1);
        expect(primeiro.reservas[0].quantidade).to.eq(2);

        const prazo = primeiro.reservas[0].expira_em;

        cy.visit('/');

        cy.contains('.card', c.livros[1].titulo).within(() => {
          cy.contains('button', 'Adicionar').click();
        });

        cy.task('reserva:estado').then(depois => {
          expect(depois.reservas).to.have.length(2);

          expect(depois.reservas[0].expira_em)
            .to.eq(depois.reservas[1].expira_em);

          expect(Date.parse(depois.reservas[0].expira_em))
            .to.be.greaterThan(Date.parse(prazo));
        });
      });

      // Fora do carrinho: o aviso depende do agendador.
      cy.visit('/');

      cy.task('reserva:tempo', 300);
      cy.task('reserva:aguardar', 'aviso');

      cy.task('reserva:estado').then(estado => {
        expect(estado.notificacoes).to.have.length(1);

        expect(estado.notificacoes[0].mensagem)
          .to.contain('cinco minutos');
      });

      cy.visit('/carrinho');

      cy.get('#aviso-reserva')
        .should('be.visible')
        .and('contain.text', 'cinco minutos');

      cy.request('/carrinho/validade');
      cy.request('/carrinho/validade');

      cy.task('reserva:estado').then(estado => {
        expect(estado.notificacoes).to.have.length(1);
      });

      // Também expira sem a página consultar o carrinho.
      cy.visit('/');

      cy.task('reserva:tempo', -1);
      cy.task('reserva:aguardar', 'expiracao');

      cy.visit('/carrinho');

      cy.get('[data-cy="item-carrinho"]')
        .should('not.exist');

      cy.get('[data-cy="produto-expirado"]')
        .should('have.length', 2);

      cy.get(
        `[data-cy="produto-expirado"]` +
        `[data-livro-id="${c.livros[0].id}"]`
      ).should('contain.text', '2 unidade(s) removida(s)');

      cy.get('[data-cy="comprar-expirado"]').each(botao => {
        cy.wrap(botao).should('be.disabled');
      });

      cy.contains('a', 'Finalizar Compra')
        .should('not.exist');

      // Nem um POST manual pode comprar os itens expirados.
      cy.request({
        method: 'POST',
        url: '/carrinho/checkout/pagamento',
        form: true,
        followRedirect: false,
        body: { acao: 'finalizar' }
      }).its('status').should('eq', 302);

      cy.task('reserva:estado').then(estado => {
        expect(estado.pedidos).to.eq(0);
        expect(estado.notificacoes).to.have.length(2);

        expect(
          estado.reservas.every(r => r.expirado === 1)
        ).to.eq(true);
      });

      cy.visit('/carrinho');

      cy.get(
        `[data-cy="produto-expirado"]` +
        `[data-livro-id="${c.livros[0].id}"]`
      ).find('[data-cy="readicionar-expirado"]').click();

      cy.get('[data-cy="item-carrinho"]')
        .should('have.length', 1)
        .within(() => {
          cy.get('[name="quantidade"]')
            .should('have.value', '2');
        });

      cy.task('reserva:estado').then(estado => {
        const ativas = estado.reservas.filter(
          r => r.expirado === 0
        );

        expect(ativas).to.have.length(1);
        expect(ativas[0].aviso_enviado).to.eq(0);

        expect(Date.parse(ativas[0].expira_em))
          .to.be.greaterThan(Date.now());
      });
    });
  });
});