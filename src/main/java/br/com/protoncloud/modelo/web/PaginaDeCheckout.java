package br.com.protoncloud.modelo.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class PaginaDeCheckout {

    private final Page pagina = SessaoWeb.pagina();

    public void preencherEntrega(String nome, String sobrenome, String cep) {
        pagina.locator("[data-test=\"firstName\"]").fill(nome);
        pagina.locator("[data-test=\"lastName\"]").fill(sobrenome);
        pagina.locator("[data-test=\"postalCode\"]").fill(cep);
        pagina.locator("[data-test=\"continue\"]").click();
        conferirQueAvancou();
    }

    /** Quando recusa os dados de entrega, a loja mostra o erro na mesma página. */
    private void conferirQueAvancou() {
        Locator resumo = pagina.locator("[data-test=\"subtotal-label\"]");
        Locator erro = pagina.locator("[data-test=\"error\"]");
        resumo.or(erro).first().waitFor();

        if (erro.isVisible()) {
            throw new AssertionError("A loja recusou os dados de entrega: " + erro.innerText());
        }
    }

    /** {@code Item total: $29.99} vira {@code 29.99}. */
    public String totalDosItens() {
        return valor("[data-test=\"subtotal-label\"]");
    }

    /** {@code Total: $32.39} vira {@code 32.39}. */
    public String total() {
        return valor("[data-test=\"total-label\"]");
    }

    public void concluir() {
        pagina.locator("[data-test=\"finish\"]").click();
        assertThat(pagina.locator("[data-test=\"complete-header\"]")).hasText("Thank you for your order!");
    }

    private String valor(String seletor) {
        String texto = pagina.locator(seletor).innerText();
        return texto.substring(texto.lastIndexOf('$') + 1).strip();
    }
}
