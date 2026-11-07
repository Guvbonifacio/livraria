describe('RNF0012 - Log de transação', () => {
  it('exibe o INSERT do cliente no histórico administrativo', () => {
    const n = Date.now();
    const nome = `Cliente Log ${n}`;
    const email = `log${n}@teste.com`;
    const senha = 'Senha@123';

    // O cadastro será registrado com o perfil CLIENTE.
    cy.request({
      method: 'POST',
      url: '/sessao/perfil',
      form: true,
      body: { perfil: 'CLIENTE' }
    });

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
      'enderecos[0].tipoLogradouro': 'Rua',
      'enderecos[0].logradouro': 'Rua das Flores',
      'enderecos[0].numero': '100',
      'enderecos[0].bairro': 'Centro',
      'enderecos[0].cep': '08700000',
      'enderecos[0].cidade': 'Mogi das Cruzes',
      'enderecos[0].estado': 'SP',
      'enderecos[0].pais': 'Brasil',
      'cartoes[0].numero': '4111111111111111',
      'cartoes[0].nomeImpresso': 'CLIENTE LOG',
      'cartoes[0].codigoSeguranca': '123'
    };

    Object.entries(campos).forEach(([campo, valor]) => {
      const sensivel =
        campo.toLowerCase().includes('senha') ||
        campo === 'cartoes[0].numero' ||
        campo === 'cartoes[0].codigoSeguranca';

      cy.get(`[name="${campo}"]`).type(
        valor,
        sensivel ? { log: false } : {}
      );
    });

    cy.get('[name="genero"]').select('Feminino');
    cy.get('[name="enderecos[0].tipo"]').select('ENTREGA');
    cy.get('[name="enderecos[0].tipoResidencia"]').select('Casa');
    cy.get('[name="cartoes[0].bandeira"]').select('Visa');
    cy.get('[name="cartoes[0].preferencial"]').check();

    cy.contains('button', 'Finalizar Cadastro').click();

    cy.location('pathname')
      .should('match', /^\/clientes\/\d+$/)
      .then(pathname => {
        const clienteId = pathname.split('/').pop();

        // Assume ADMIN somente para consultar o histórico.
        cy.request({
          method: 'POST',
          url: '/sessao/perfil',
          form: true,
          body: { perfil: 'ADMIN' }
        });

        cy.visit(`/admin/clientes/${clienteId}`);

        cy.get('[data-cy="historico-alteracoes"]').within(() => {
          cy.contains('Histórico de alterações')
            .should('be.visible');

          cy.contains(
            '[data-cy="log-operacao"]',
            /^\s*INSERT\s*$/
          )
            .closest('[data-cy="log-transacao"]')
            .within(() => {
              cy.get('[data-cy="log-tabela"]')
                .should($elemento => {
                  expect($elemento.text().trim())
                    .to.eq('cliente');
                });

              cy.get('[data-cy="log-registro-id"]')
                .should($elemento => {
                  expect($elemento.text().trim())
                    .to.eq(clienteId);
                });

              // O responsável é o perfil usado no cadastro.
              cy.get('[data-cy="log-usuario"]')
                .should($elemento => {
                  expect($elemento.text().trim())
                    .to.eq('usuario-teste (CLIENTE)');
                });

              cy.get('[data-cy="log-data-hora"]')
                .should($elemento => {
                  expect($elemento.text().trim()).to.match(
                    /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}$/
                  );
                });

              cy.get('[data-cy="log-dados"]')
                .invoke('text')
                .then(texto => {
                  const dados = JSON.parse(texto);

                  expect(dados.nome).to.equal(nome);
                  expect(dados.email).to.equal(email);
                  expect(dados.ativo).to.equal(true);

                  // Confere os campos permitidos na auditoria.
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