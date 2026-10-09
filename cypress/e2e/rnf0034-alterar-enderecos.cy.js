function cadastrarClienteTeste(prefixo) {
  const n = `${Date.now()}${Cypress._.random(1000, 9999)}`;
  const nome = `${prefixo} ${n}`;

  return cy.request({
    method: 'POST',
    url: '/clientes/salvar',
    form: true,
    log: false,
    body: {
      nome,
      genero: 'Feminino',
      dataNascimento: '1995-05-20',
      cpf: n.slice(-11),
      telefone: '11999990000',
      email: `${prefixo}${n}@teste.com`,
      senha: 'Senha@123',
      confirmacaoSenha: 'Senha@123',

      'enderecos[0].nomeCurto': 'Casa',
      'enderecos[0].tipo': 'ENTREGA',
      'enderecos[0].tipoResidencia': 'Casa',
      'enderecos[0].logradouro': 'Rua das Flores',
      'enderecos[0].numero': '100',
      'enderecos[0].bairro': 'Centro',
      'enderecos[0].cep': '08700000',
      'enderecos[0].cidade': 'Mogi das Cruzes',
      'enderecos[0].estado': 'SP',

      'cartoes[0].numero': '4111111111111111',
      'cartoes[0].nomeImpresso': 'CLIENTE TESTE',
      'cartoes[0].bandeira': 'Visa',
      'cartoes[0].codigoSeguranca': '123',
      'cartoes[0].preferencial': 'true'
    }
  }).then((resposta) => {
    expect(resposta.status).to.equal(200);
    expect(resposta.redirects).to.have.length(1);

    const match = resposta.redirects[0].match(/\/clientes\/(\d+)$/);

    expect(
      match,
      'cadastro deve redirecionar para o perfil'
    ).not.to.be.null;

    return {
      id: match[1],
      nome
    };
  });
}

describe('RNF0034 / RF0026 - Alterar apenas endereços', () => {
  it('adiciona um segundo endereço sem alterar o nome e protege o último endereço', () => {
    cadastrarClienteTeste('Endereco').then(({ id, nome }) => {
      cy.visit(`/clientes/${id}`);

      cy.get('[data-cy="endereco-item"]').should('have.length', 1);
      cy.get('[name="nome"]').should('have.value', nome);

      cy.get('[data-bs-target="#modalEndereco"]').click();

      cy.get('#modalEndereco')
        .should('be.visible')
        .within(() => {
          const campos = {
            nomeCurto: 'Trabalho',
            tipoResidencia: 'Comercial',
            logradouro: 'Rua do Trabalho',
            numero: '200',
            bairro: 'Centro',
            cep: '08700000',
            cidade: 'Mogi das Cruzes',
            estado: 'SP'
          };

          Object.entries(campos).forEach(([campo, valor]) => {
            cy.get(`[name="${campo}"]`).type(valor);
          });

          cy.get('[name="tipo"]').select('COBRANCA');

          cy.contains('button', 'Salvar Endereço').click();
        });

      cy.get('[data-cy="sucesso-relacionamentos"]')
        .should('be.visible');

      cy.reload();

      cy.get('[data-cy="endereco-item"]').should('have.length', 2);

      cy.contains('[data-cy="endereco-nome"]', /^Trabalho$/)
        .should('be.visible');

      cy.get('[name="nome"]').should('have.value', nome);

      // É permitido excluir o segundo endereço.
      cy.contains('[data-cy="endereco-nome"]', /^Trabalho$/)
        .closest('[data-cy="endereco-item"]')
        .within(() => {
          cy.get('[data-cy="remover-endereco"]').click();
        });

      cy.get('[data-cy="endereco-item"]').should('have.length', 1);

      // O servidor deve impedir a exclusão do último endereço.
      cy.get('[data-cy="remover-endereco"]').click();

      cy.get('[data-cy="erros-relacionamentos"]')
        .should(
          'contain.text',
          'Não é permitido remover o último endereço.'
        );

      cy.get('[data-cy="endereco-item"]').should('have.length', 1);
      cy.get('[name="nome"]').should('have.value', nome);
    });
  });
});