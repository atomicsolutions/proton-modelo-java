package br.com.protoncloud.modelo.paginas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import br.com.protoncloud.modelo.apoio.Config;
import br.com.protoncloud.modelo.apoio.Navegador;

public class PaginaDeLogin {

    private final Page pagina = Navegador.pagina();

    public PaginaDeLogin abrir() {
        Navegador.irPara(Config.urlDaLoja());
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
