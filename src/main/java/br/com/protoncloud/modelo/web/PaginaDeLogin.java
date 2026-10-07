package br.com.protoncloud.modelo.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import br.com.protoncloud.modelo.apoio.Config;

public class PaginaDeLogin {

    private final Page pagina = SessaoWeb.pagina();

    public PaginaDeLogin abrir() {
        SessaoWeb.irPara(Config.urlDaLoja());
        return this;
    }

    public void entrar(String usuario, String senha) {
        pagina.locator("[data-test=\"username\"]").fill(usuario);
        pagina.locator("[data-test=\"password\"]").fill(senha);
        pagina.locator("[data-test=\"login-button\"]").click();
    }

    public String mensagemDeErro() {
        Locator erro = pagina.locator("[data-test=\"error\"]");
        assertThat(erro).isVisible();
        return erro.innerText();
    }
}
