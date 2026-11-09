describe('RF0058 - Exportar dados do gráfico de vendas', () => {
  let c;

  beforeEach(() => {
    cy.task('analise:preparar').then(cenario => {
      c = cenario;

      cy.request({
        method: 'POST',
        url: '/sessao/perfil',
        form: true,
        body: { perfil: 'ADMIN' }
      });
    });
  });

  it('exporta os mesmos filtros e valores com BOM e formato brasileiro', () => {
    const categorias = ['Fantasia', 'Ficção Científica'];

    const filtro = {
      dataInicio: c.dataInicio,
      dataFim: c.dataFim,
      categorias
    };

    const query = new URLSearchParams({
      dataInicio: c.dataInicio,
      dataFim: c.dataFim
    });

    categorias.forEach(nome => {
      query.append('categorias', nome);
    });

    cy.task('analise:totais', filtro).then(esperado => {
      cy.visit(`/admin/analise?${query}`);

      // Permite visualizar a análise com os filtros aplicados.
      cy.wait(1000);

      cy.get('a[data-cy="exportar-analise"]')
        .should('be.visible')
        .and('have.class', 'btn-sm')
        .and('have.class', 'btn-outline-secondary');

      // Permite visualizar o botão de exportação.
      cy.wait(1000);

      cy.get('a[data-cy="exportar-analise"]')
        .invoke('attr', 'href')
        .then(href => {
          const url = new URL(href, Cypress.config('baseUrl'));

          expect(url.searchParams.get('dataInicio'))
            .to.eq(c.dataInicio);

          expect(url.searchParams.get('dataFim'))
            .to.eq(c.dataFim);

          expect(url.searchParams.getAll('categorias'))
            .to.deep.eq(categorias);

          // Pausa após conferir os filtros da exportação.
          cy.wait(1000);

          cy.request({
            url: href,
            encoding: 'binary'
          }).then(resposta => {
            expect(resposta.status).to.eq(200);

            expect(resposta.headers['content-type'])
              .to.match(/^text\/csv;\s*charset=UTF-8$/i);

            expect(resposta.headers['content-disposition'])
              .to.eq(
                'attachment; filename="analise-vendas.csv"'
              );

            expect(
              [...resposta.body.slice(0, 3)]
                .map(caractere => caractere.charCodeAt(0))
            ).to.deep.eq([239, 187, 191]);

            const csv = Cypress.Buffer
              .from(resposta.body, 'binary')
              .toString('utf8');

            const linhas = [
              '\uFEFFperíodo;categoria;valor de venda'
            ];

            esperado.etiquetas.forEach((periodo, indice) => {
              esperado.series.forEach(serie => {
                const valor = (
                  serie.valoresCentavos[indice] / 100
                ).toFixed(2).replace('.', ',');

                linhas.push(
                  `${periodo};${serie.categoria};${valor}`
                );
              });
            });

            expect(csv).to.eq(
              linhas.join('\r\n') + '\r\n'
            );
          });

          // Pausa após validar o conteúdo exportado.
          cy.wait(1000);
        });
    });
  });

  it('exporta o zero real de um período sem vendas', () => {
    cy.visit(
      '/admin/analise?dataInicio=2026-02-01' +
      '&dataFim=2026-02-28&categorias=Autoajuda'
    );

    cy.get('[data-cy="sem-vendas-analise"]')
      .should('be.visible');

    // Permite visualizar a indicação de ausência de vendas.
    cy.wait(1000);

    cy.get('a[data-cy="exportar-analise"]')
      .should('be.visible');

    // Permite visualizar a opção de exportação.
    cy.wait(1000);

    cy.get('a[data-cy="exportar-analise"]')
      .invoke('attr', 'href')
      .then(href => {
        cy.request({
          url: href,
          encoding: 'binary'
        }).then(resposta => {
          const csv = Cypress.Buffer
            .from(resposta.body, 'binary')
            .toString('utf8');

          expect(csv).to.eq(
            '\uFEFFperíodo;categoria;valor de venda\r\n' +
            'fev/2026;Autoajuda;0,00\r\n'
          );
        });

        // Pausa após validar a exportação com valor zero.
        cy.wait(1000);
      });
  });

  it('não disponibiliza download antes de aplicar filtros válidos', () => {
    cy.visit('/admin/analise');

    cy.get('[data-cy="exportar-analise"]')
      .should('be.disabled');

    // Permite visualizar a exportação desabilitada sem filtros.
    cy.wait(1000);

    cy.visit(
      '/admin/analise?dataInicio=2026-03-01&dataFim=2026-02-28'
    );

    cy.get('[data-cy="erros-analise"]')
      .should('be.visible');

    cy.get('[data-cy="exportar-analise"]')
      .should('be.disabled');

    // Permite visualizar os erros e a exportação desabilitada.
    cy.wait(1000);

    cy.request({
      url: '/admin/analise/exportar' +
        '?dataInicio=2026-03-01&dataFim=2026-02-28',
      failOnStatusCode: false
    }).then(resposta => {
      expect(resposta.status).to.eq(400);

      expect(resposta.headers['content-disposition'])
        .to.eq(undefined);
    });

    // Pausa após validar a recusa da exportação.
    cy.wait(1000);
  });

  it('recusa a exportação pelo perfil CLIENTE', () => {
    cy.request({
      method: 'POST',
      url: '/sessao/perfil',
      form: true,
      body: { perfil: 'CLIENTE' }
    });

    // Pausa após alterar o perfil da sessão.
    cy.wait(1000);

    cy.request({
      url: '/admin/analise/exportar',
      failOnStatusCode: false
    }).then(resposta => {
      expect(resposta.status).to.eq(403);

      expect(resposta.headers['content-disposition'])
        .to.eq(undefined);
    });

    // Pausa após validar a recusa ao perfil CLIENTE.
    cy.wait(1000);
  });
});