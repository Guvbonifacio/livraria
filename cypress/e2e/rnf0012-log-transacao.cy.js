describe('RNF0012 - Log de transação', () => {
  it('exibe o INSERT do cliente no histórico administrativo', () => {
    const n = Date.now();
    const nome = `Cliente Log ${n}`;
    const email = `log${n}@teste.com`;
    const senha = 'Senha@123';

    cy.visit('/clientes/novo');

    const campos = {
      nome,
      dataNascimento: '1995-05-20',
      cpf: String(n).slice(-11),
      telefone: '11999990000',
      email,
      senha,
      confirmacaoSenha: senha,
      'enderecos[0].nomeCurto': 'Casa',
      'enderecos[0].logradouro': 'Rua das Flores',
      'enderecos[0].numero': '100',
      'enderecos[0].bairro': 'Centro',
      'enderecos[0].cep': '08700000',
      'enderecos[0].cidade': 'Mogi das Cruzes',
      'enderecos[0].estado': 'SP',
      'cartoes[0].numero': '4111111111111111',
      'cartoes[0].nomeImpresso': 'CLIENTE LOG',
      'cartoes[0].codigoSeguranca': '123'
    };

    Object.entries(campos).forEach(([campo, valor]) => {
      const opcoes = campo.toLowerCase().includes('senha')
        || campo === 'cartoes[0].codigoSeguranca'
        ? { log: false }
        : {};

      cy.get(`[name="${campo}"]`).type(valor, opcoes);
    });

    cy.get('[name="genero"]').select('Feminino');
    cy.get('[name="enderecos[0].tipo"]').select('ENTREGA');
    cy.get('[name="enderecos[0].tipoResidencia"]').select('Casa');
    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.get('[name="cartoes[0].preferencial"]').check();

    cy.contains('button', 'Finalizar Cadastro').click();

    cy.location('pathname')
      .should('match', /^\/clientes\/\d+$/)
      .then((pathname) => {
        const clienteId = pathname.split('/').pop();

        cy.visit(`/admin/clientes/${clienteId}`);

        cy.get('[data-cy="historico-alteracoes"]').within(() => {
          cy.contains('Histórico de alterações').should('be.visible');

          cy.contains('[data-cy="log-operacao"]', /^INSERT$/)
            .closest('[data-cy="log-transacao"]')
            .within(() => {
              cy.get('[data-cy="log-tabela"]')
                .should('have.text', 'cliente');

              cy.get('[data-cy="log-registro-id"]')
                .should('have.text', clienteId);

              cy.get('[data-cy="log-usuario"]')
                .should('have.text', 'usuario-teste');

              cy.get('[data-cy="log-data-hora"]')
                .invoke('text')
                .should(
                  'match',
                  /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}$/
                );

              cy.get('[data-cy="log-dados"]')
                .invoke('text')
                .then((texto) => {
                  const dados = JSON.parse(texto);

                  expect(dados.nome).to.equal(nome);
                  expect(dados.email).to.equal(email);
                  expect(dados.ativo).to.equal(true);

                  // Confirma os campos permitidos e impede a inclusão
                  // de senha, hash ou outros campos não previstos.
                  expect(dados).to.have.all.keys(
                    'nome',
                    'genero',
                    'data_nascimento',
                    'cpf',
                    'telefone',
                    'email',
                    'ativo'
                  );

                  expect(texto).not.to.include(senha);
                });
            });
        });
      });
  });
});