describe('RNF0011 - Resposta da consulta de análise em até um segundo', {
  retries: 0
}, () => {
  it('mede cinco respostas HTTP completas e exige até 1000 ms em cada uma', () => {
    cy.task('analise:preparar').then(cenario => {
      cy.request({
        method: 'POST',
        url: '/sessao/perfil',
        form: true,
        body: { perfil: 'ADMIN' }
      });

      const periodos = [
        {
          nome: 'Massa de 14 meses - primeira consulta',
          dataInicio: cenario.dataInicio,
          dataFim: cenario.dataFim
        },
        {
          nome: 'Limite de 24 meses',
          ...cenario.filtrosTeste.vinteQuatroMeses
        },
        {
          nome: 'Um mês',
          ...cenario.filtrosTeste.umMes
        },
        {
          nome: 'Massa de 14 meses - segunda consulta',
          dataInicio: cenario.dataInicio,
          dataFim: cenario.dataFim
        },
        {
          nome: 'Limite de 24 meses - segunda consulta',
          ...cenario.filtrosTeste.vinteQuatroMeses
        }
      ];

      const amostras = [];

      periodos.forEach((periodo, indice) => {
        cy.request({
          method: 'GET',
          url: '/admin/analise',

          qs: {
            dataInicio: periodo.dataInicio,
            dataFim: periodo.dataFim
          },

          headers: {
            'Cache-Control': 'no-cache'
          },

          followRedirect: false,
          failOnStatusCode: false,
          retryOnStatusCodeFailure: false,
          retryOnNetworkFailure: false,

          // Permite registrar uma resposta lenta.
          // A aprovação continua exigindo até 1000 ms.
          responseTimeout: 10000
        }).then(resposta => {
          amostras.push({
            amostra: indice + 1,
            periodo: periodo.nome,
            dataInicio: periodo.dataInicio,
            dataFim: periodo.dataFim,
            status: resposta.status,
            duracaoMs: resposta.duration
          });

          cy.log(
            `${periodo.nome}: ${resposta.duration} ms`
          );

          // Grava antes da asserção para preservar
          // também uma resposta recebida acima do limite.
          return cy.writeFile(
            'target/metricas/rnf0011-analise.json',
            {
              requisito: 'RNF0011',
              geradoEm: new Date().toISOString(),
              medicao:
                'Resposta HTTP completa do GET /admin/analise',
              limiteMs: 1000,

              massa: {
                pedidos: cenario.quantidadePedidos,
                itens: cenario.quantidadeItens
              },

              maiorDuracaoMs: Math.max(
                ...amostras.map(a => a.duracaoMs)
              ),

              amostras
            }
          ).then(() => {
            expect(
              resposta.status,
              periodo.nome
            ).to.eq(200);

            expect(
              resposta.headers['content-type']
            ).to.contain('text/html');

            // Evita aprovar uma resposta rápida de erro
            // ou uma página que não executou a análise.
            expect(resposta.body)
              .to.contain('data-cy="grafico-vendas"');

            expect(resposta.duration)
              .to.be.a('number');

            expect(
              resposta.duration,
              `${periodo.nome}: ${resposta.duration} ms`
            ).to.be.at.most(1000);
          });
        });
      });
    });
  });
});