package br.com.protoncloud.modelo.web;

import java.nio.file.Path;
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

import br.com.protoncloud.modelo.apoio.Config;
import br.com.protoncloud.modelo.apoio.Evidencias;
import br.com.protoncloud.modelo.apoio.Sessao;
import br.com.protoncloud.modelo.apoio.Sessoes;

/**
 * A sessão web: um navegador (Playwright) por cenário, compartilhado pelos passos dele.
 * As páginas usam {@link #pagina()}; o navegador abre na primeira vez que alguém pede.
 */
public final class SessaoWeb implements Sessao {

    private static final Logger log = LoggerFactory.getLogger(SessaoWeb.class);
    private static final int TENTATIVAS = 4;

    /** Queda de conexão (net::...) e a página de erro do Chrome interrompendo a navegação seguinte. */
    private static final List<String> QUEDAS_DE_REDE = List.of("net::", "interrupted by another navigation");

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext contexto;
    private final Page pagina;

    private SessaoWeb() {
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

    private static SessaoWeb sessao() {
        return Sessoes.obter("web", SessaoWeb.class, SessaoWeb::new);
    }

    /** A página do navegador do cenário. */
    public static Page pagina() {
        return sessao().pagina;
    }

    /** Grava um print da página inteira na pasta de evidências. */
    public static Path printDaTela(String nome) {
        return sessao().evidencia(nome);
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

    @Override
    public Path evidencia(String nome) {
        Path arquivo = Evidencias.arquivo(nome, "png");
        pagina.screenshot(new Page.ScreenshotOptions().setPath(arquivo).setFullPage(true));
        return Evidencias.registrar(arquivo);
    }

    @Override
    public void close() {
        try {
            contexto.close();
            browser.close();
        } finally {
            playwright.close();
        }
    }
}
