package br.com.protoncloud.modelo.componentes;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.protoncloud.modelo.apoio.Componente;
import br.com.protoncloud.modelo.apoio.Evidencias;
import br.com.protoncloud.modelo.paginas.PaginaDeCheckout;
import br.com.protoncloud.modelo.paginas.PaginaDeProdutos;
import br.com.protoncloud.modelo.paginas.PaginaDoCarrinho;

/** FinalizarCompra: preenche a entrega, confere o total dos itens e conclui a compra. */
public class FinalizarCompra implements Componente {

    private static final Logger log = LoggerFactory.getLogger(FinalizarCompra.class);

    @Override
    public Map<String, String> executar(Map<String, String> parametros) {
        new PaginaDeProdutos().abrirCarrinho();
        new PaginaDoCarrinho().irParaOCheckout();

        var checkout = new PaginaDeCheckout();
        checkout.preencherEntrega(parametros.get("in_nome"), parametros.get("in_sobrenome"), parametros.get("in_cep"));

        String esperado = parametros.getOrDefault("in_preco_esperado", "");
        String totalDosItens = checkout.totalDosItens();

        if (!esperado.isBlank() && !totalDosItens.equals(esperado)) {
            throw new AssertionError("Total dos itens " + totalDosItens + "; o esperado era " + esperado);
        }

        String total = checkout.total();
        Evidencias.printDaTela("resumo-da-compra");
        checkout.concluir();
        log.info("Compra concluída: total {}", total);

        return Map.of("out_total", total);
    }
}
