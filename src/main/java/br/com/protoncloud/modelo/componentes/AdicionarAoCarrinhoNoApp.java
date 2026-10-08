package br.com.protoncloud.modelo.componentes;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.protoncloud.modelo.apoio.Componente;
import br.com.protoncloud.modelo.mobile.SessaoMobile;
import br.com.protoncloud.modelo.mobile.TelaDeProdutos;

/** AdicionarAoCarrinhoNoApp: no app, abre o produto pelo nome, põe no carrinho e devolve o preço. */
public class AdicionarAoCarrinhoNoApp implements Componente {

    private static final Logger log = LoggerFactory.getLogger(AdicionarAoCarrinhoNoApp.class);

    @Override
    public Map<String, String> executar(Map<String, String> parametros) {
        String produto = parametros.get("in_produto");
        var tela = new TelaDeProdutos().abrirProduto(produto);

        if (!tela.nome().equals(produto)) {
            throw new AssertionError("O app abriu \"" + tela.nome() + "\"; o esperado era \"" + produto + "\"");
        }

        String preco = tela.preco();
        tela.adicionarAoCarrinho();
        SessaoMobile.printDaTela("carrinho-no-app");
        log.info("{} no carrinho do app, por {}", produto, preco);

        return Map.of("out_preco_no_app", preco);
    }
}
