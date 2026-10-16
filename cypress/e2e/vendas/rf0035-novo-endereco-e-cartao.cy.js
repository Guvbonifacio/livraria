describe('RF0035 - Novo endereço e cartão durante a compra', () => {
  let c;

  beforeEach(() => {
    cy.task('vendas:preparar', 'novos').then(dados => {
      c = dados;

      cy.visit('/clientes/1');
      cy.get('[name="nome"]').should('have.value', c.nome);
    });
  });

  it('incorpora os dois ao perfil e utiliza ambos no pedido', () => {
    cy.visit('/');

    cy.contains('.card', 'O Hobbit').within(() => {
      cy.contains('button', 'Adicionar').click();
    });

    cy.contains('a', 'Finalizar Compra').click();

    cy.get('[name="origem"][value="NOVO"]').check();

    cy.get('[name="novoEndereco.nomeCurto"]')
      .clear()
      .type('Trabalho E2E');

    cy.get('[name="novoEndereco.tipoResidencia"]')
      .select('Outro');

    const campos = {
      tipoLogradouro: 'Avenida',
      logradouro: 'Nova E2E',
      numero: '200',
      bairro: 'Centro',
      cep: '08700000',
      cidade: 'Mogi das Cruzes',
      estado: 'SP',
      pais: 'Brasil'
    };

    Object.entries(campos).forEach(([campo, valor]) => {
      cy.get(`[name="novoEndereco.${campo}"]`)
        .clear()
        .type(valor);
    });

    cy.get('[name="salvarPerfil"][type="checkbox"]').check();

    cy.get('button[name="acao"][value="continuar"]').click();

    cy.location('pathname')
      .should('eq', '/carrinho/checkout/pagamento');

    // Contrato da funcionalidade ainda pendente.
    cy.get('[data-cy="usar-novo-cartao"]').check();

    cy.get('[name="novoCartao.numero"]')
      .type('5555555555554444', { log: false });

    cy.get('[name="novoCartao.nomeImpresso"]')
      .type('NOVO CARTAO E2E');

    cy.get('[name="novoCartao.bandeira"]')
      .select('Mastercard');

    cy.get('[name="novoCartao.codigoSeguranca"]')
      .type('123', { log: false });

    cy.get('[name="salvarCartaoPerfil"]').check();
    cy.get('[name="valorNovoCartao"]').type('47.00');

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

        const endereco = JSON.parse(
          resultado.pedido.endereco_entrega_json
        );

        expect(endereco).to.include({
          nomeCurto: 'Trabalho E2E',
          logradouro: 'Nova E2E'
        });

        expect(resultado.pagamentos).to.have.length(1);

        const pagamento = resultado.pagamentos[0];

        expect(pagamento).to.include({
          forma: 'CARTAO',
          valor_centavos: 4700,
          bandeira: 'Mastercard'
        });

        expect(pagamento.numero_mascarado)
          .to.contain('4444');

        cy.task('vendas:perfil').then(perfil => {
          expect(perfil.nome).to.eq(c.nome);
          expect(perfil.enderecos).to.have.length(2);
          expect(perfil.cartoes).to.have.length(2);

          const salvo = perfil.enderecos.find(
            e => e.id === endereco.id
          );

          expect(salvo.logradouro).to.eq('Nova E2E');

          const cartao = perfil.cartoes.find(
            p => p.id === pagamento.cartao_id
          );

          expect(cartao.numero_mascarado)
            .to.contain('4444');
        });
      });
    });
  });
});