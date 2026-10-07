package br.com.protoncloud.modelo.paginas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import br.com.protoncloud.modelo.apoio.Navegador;

public class PaginaDeProdutos {

    private final Page pagina = Navegador.pagina();

    public void conferirQueAbriu() {
        assertThat(pagina.locator("[data-test=\"title\"]")).hasText("Products");
    }

    /** Adiciona o produto e devolve o preço dele, sem o símbolo da moeda. */
    public String adicionarAoCarrinho(String produto) {
        Locator item = pagina.locator("[data-test=\"inventory-item\"]").filter(new Locator.FilterOptions().setHasText(produto));

        if (item.count() != 1) {
            throw new AssertionError("Produto \"" + produto + "\" não encontrado na loja");
        }

        String preco = item.locator("[data-test=\"inventory-item-price\"]").innerText();
        int antes = itensNoCarrinho();
        item.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add to cart")).click();
        assertThat(pagina.locator("[data-test=\"shopping-cart-badge\"]")).hasText(String.valueOf(antes + 1));

        return preco.replace("$", "").strip();
    }

    public int itensNoCarrinho() {
        Locator contador = pagina.locator("[data-test=\"shopping-cart-badge\"]");
        return contador.count() > 0 ? Integer.parseInt(contador.innerText().strip()) : 0;
    }

    public void abrirCarrinho() {
        pagina.locator("[data-test=\"shopping-cart-link\"]").click();
    }
}
