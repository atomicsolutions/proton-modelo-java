package br.com.protoncloud.modelo.apoio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evidências: prints e arquivos gravados na pasta de evidências ({@code PASTA_DE_EVIDENCIAS}).
 * <p>
 * Fora do Proton, ficam na pasta. Numa execução do Proton, o que aparece na pasta durante
 * um passo sobe para aquele passo. A automação não precisa saber onde está rodando.
 * </p>
 * <p>
 * Cada plataforma grava a evidência dela ({@code SessaoWeb.printDaTela},
 * {@code SessaoApi.evidencia}). Quando um passo falha, {@link #daFalha} pede a evidência de
 * todas as sessões abertas.
 * </p>
 */
public final class Evidencias {

    private static final Logger log = LoggerFactory.getLogger(Evidencias.class);
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private Evidencias() {
    }

    /** Um caminho novo na pasta de evidências, com data e hora no nome. */
    public static Path arquivo(String nome, String extensao) {
        try {
            Files.createDirectories(Config.pastaDeEvidencias());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Config.pastaDeEvidencias().resolve(LocalDateTime.now().format(CARIMBO) + "-" + nome + "." + extensao);
    }

    public static Path registrar(Path arquivo) {
        log.info("Evidência: {}", arquivo.getFileName());
        return arquivo;
    }

    /** A evidência de cada sessão aberta, quando um passo falha. */
    public static void daFalha(String componente) {
        for (var sessao : Sessoes.todas().entrySet()) {
            try {
                sessao.getValue().evidencia("falha-" + componente + "-" + sessao.getKey());
            } catch (RuntimeException e) {
                // Sem evidência (navegador já fechado, por exemplo), o erro do passo é o que importa.
                log.warn("Não deu para gravar a evidência de {} na falha de {}", sessao.getKey(), componente);
            }
        }
    }
}
