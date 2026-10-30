(() => {
  const aviso = document.getElementById('aviso-reserva');
  const noCarrinho = window.location.pathname === '/carrinho';

  let versao;
  let executando = false;

  async function atualizar() {
    if (executando) return;

    executando = true;

    try {
      const resposta = await fetch('/carrinho/validade', {
        cache: 'no-store'
      });

      if (!resposta.ok) return;

      const estado = await resposta.json();

      if (estado.expirou) {
        window.location.assign('/carrinho');
        return;
      }

      if (
        noCarrinho &&
        versao !== undefined &&
        versao !== estado.versao
      ) {
        window.location.reload();
        return;
      }

      versao = estado.versao;

      if (aviso) {
        aviso.textContent = estado.aviso;
        aviso.hidden = !estado.aviso;
      }

    } catch {
      // Uma falha temporária será verificada na próxima consulta.

    } finally {
      executando = false;
    }
  }

  atualizar();

  window.setInterval(atualizar, 3000);
})();