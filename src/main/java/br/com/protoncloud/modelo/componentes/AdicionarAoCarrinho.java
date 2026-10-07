package br.com.protoncloud.modelo.componentes;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.protoncloud.modelo.apoio.Componente;
import br.com.protoncloud.modelo.web.PaginaDeProdutos;

/** AdicionarAoCarrinho: adiciona o produto pelo nome e devolve o preço dele. */
public class AdicionarAoCarrinho implements Componente {

    private static final Logger log = LoggerFactory.getLogger(AdicionarAoCarrinho.class);

    @Override
    public Map<String, String> executar(Map<String, String> parametros) {
        String produto = parametros.get("in_produto");
        String preco = new PaginaDeProdutos().adicionarAoCarrinho(produto);
        log.info("{} no carrinho, por {}", produto, preco);
        return Map.of("out_preco", preco);
    }
}
