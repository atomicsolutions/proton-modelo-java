package br.com.protoncloud.modelo.apoio;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

/**
 * O navegador da automação: um por cenário, compartilhado pelos passos dele.
 *
 * <pre>{@code
 * try (var navegador = Navegador.abrir()) {
 *     ... // os passos usam Navegador.pagina()
 * }
 * }</pre>
 */
public final class Navegador implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Navegador.class);
    private static final int TENTATIVAS = 4;

    /** Queda de conexão (net::...) e a página de erro do Chrome interrompendo a navegação seguinte. */
    private static final List<String> QUEDAS_DE_REDE = List.of("net::", "interrupted by another navigation");

    private static Page pagina;

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext contexto;

    private Navegador() {
        var criacao = new Playwright.CreateOptions();
        var opcoes = new BrowserType.LaunchOptions().setHeadless(Config.headless());

        if (Config.canalDoNavegador() != null) {
            // Com o navegador da máquina, a Playwright não precisa baixar nenhum. Sem isso,
            // a primeira execução baixa Chromium, Firefox e WebKit.
            criacao.setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"));
            opcoes.setChannel(Config.canalDoNavegador());
        }

        playwright = Playwright.create(criacao);
        browser = playwright.chromium().launch(opcoes);
        contexto = browser.newContext(new Browser.NewContextOptions().setViewportSize(1366, 768));
        pagina = contexto.newPage();
        pagina.setDefaultTimeout(Config.timeoutMs());
    }

    /** Abre o navegador para os passos de um cenário; feche com try-with-resources. */
    public static Navegador abrir() {
        return new Navegador();
    }

    /** Abre o endereço, tentando de novo, com espera crescente, quando a rede cai. */
    public static void irPara(String url) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                pagina().navigate(url);
                return;
            } catch (PlaywrightException erro) {
                String mensagem = String.valueOf(erro.getMessage());

                if (QUEDAS_DE_REDE.stream().noneMatch(mensagem::contains) || tentativa == TENTATIVAS) {
                    throw erro;
                }

                long espera = 1L << tentativa;
                log.warn("Rede instável ao abrir {} (tentativa {} de {}), nova tentativa em {} s", url, tentativa, TENTATIVAS, espera);
                pagina().waitForTimeout(espera * 1000);
            }
        }
    }

    /** A página aberta, para as páginas da automação ({@code paginas}). */
    public static Page pagina() {
        if (pagina == null) {
            throw new IllegalStateException("Navegador fechado: rode os passos dentro de Navegador.abrir().");
        }

        return pagina;
    }

    @Override
    public void close() {
        pagina = null;
        contexto.close();
        browser.close();
        playwright.close();
    }
}
