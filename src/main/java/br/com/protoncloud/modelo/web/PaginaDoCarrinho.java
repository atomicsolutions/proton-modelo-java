package br.com.protoncloud.modelo.web;

import com.microsoft.playwright.Page;

public class PaginaDoCarrinho {

    private final Page pagina = SessaoWeb.pagina();

    public void irParaOCheckout() {
        pagina.locator("[data-test=\"checkout\"]").click();
    }
}
