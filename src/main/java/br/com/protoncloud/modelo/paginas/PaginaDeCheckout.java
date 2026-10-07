package br.com.protoncloud.modelo.paginas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;

import br.com.protoncloud.modelo.apoio.Navegador;

public class PaginaDeCheckout {

    private final Page pagina = Navegador.pagina();

    public void preencherEntrega(String nome, String sobrenome, String cep) {
        pagina.locator("[data-test=\"firstName\"]").fill(nome);
        pagina.locator("[data-test=\"lastName\"]").fill(sobrenome);
        pagina.locator("[data-test=\"postalCode\"]").fill(cep);
        pagina.locator("[data-test=\"continue\"]").click();
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
