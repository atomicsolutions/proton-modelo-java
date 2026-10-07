package br.com.protoncloud.modelo.apoio;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Page;

/**
 * Evidências: prints gravados na pasta de evidências ({@code PASTA_DE_EVIDENCIAS}).
 * <p>
 * Fora do Proton, ficam na pasta. Numa execução do Proton, o que aparece na pasta durante
 * um passo sobe para aquele passo. A automação não precisa saber onde está rodando.
 * </p>
 */
public final class Evidencias {

    private static final Logger log = LoggerFactory.getLogger(Evidencias.class);
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private Evidencias() {
    }

    /** Grava um print da página inteira e devolve o caminho do arquivo. */
    public static Path printDaTela(String nome) {
        Path arquivo = Config.pastaDeEvidencias().resolve(LocalDateTime.now().format(CARIMBO) + "-" + nome + ".png");
        Navegador.pagina().screenshot(new Page.ScreenshotOptions().setPath(arquivo).setFullPage(true));
        log.info("Print: {}", arquivo.getFileName());
        return arquivo;
    }
}
