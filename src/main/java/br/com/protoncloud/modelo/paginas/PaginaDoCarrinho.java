package br.com.protoncloud.modelo.paginas;

import com.microsoft.playwright.Page;

import br.com.protoncloud.modelo.apoio.Navegador;

public class PaginaDoCarrinho {

    private final Page pagina = Navegador.pagina();

    public void irParaOCheckout() {
        pagina.locator("[data-test=\"checkout\"]").click();
    }
}
