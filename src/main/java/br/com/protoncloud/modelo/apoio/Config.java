package br.com.protoncloud.modelo.apoio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração da automação. Cada valor vem, nesta ordem, da propriedade {@code -DNOME},
 * da variável de ambiente {@code NOME} ou do arquivo {@code .env}.
 * <p>
 * O {@code .env} não vai para o git: copie o {@code .env.example} e ajuste.
 * </p>
 */
public final class Config {

    private static final Map<String, String> ARQUIVO_ENV = lerEnv(Path.of(".env"));

    private Config() {
    }

    public static String urlDaLoja() {
        return valor("URL_DA_LOJA", "https://www.saucedemo.com");
    }

    public static boolean headless() {
        return !"false".equalsIgnoreCase(valor("HEADLESS", "true"));
    }

    /**
     * Vazio usa o Chromium que a Playwright baixa sozinha na primeira execução.
     * "chrome" ou "msedge" usam o navegador já instalado na máquina, sem download.
     */
    public static String canalDoNavegador() {
        return valor("CANAL_DO_NAVEGADOR", null);
    }

    public static double timeoutMs() {
        return Double.parseDouble(valor("TIMEOUT_MS", "15000"));
    }

    public static Path pastaDeEvidencias() {
        return Path.of(valor("PASTA_DE_EVIDENCIAS", "evidencias"));
    }

    public static Path pastaDeCenarios() {
        return Path.of("cenarios");
    }

    /** O valor configurado, ou o padrão quando não há nenhum. */
    public static String valor(String nome, String padrao) {
        String valor = System.getProperty(nome);

        if (vazio(valor)) {
            valor = System.getenv(nome);
        }

        if (vazio(valor)) {
            valor = ARQUIVO_ENV.get(nome);
        }

        return vazio(valor) ? padrao : valor.trim();
    }

    /** O valor configurado; sem ele, um erro que diz onde configurar. */
    public static String obrigatorio(String nome) {
        String valor = valor(nome, null);

        if (valor == null) {
            throw new IllegalStateException("Defina " + nome + " no ambiente ou no .env (veja o .env.example).");
        }

        return valor;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static Map<String, String> lerEnv(Path arquivo) {
        Map<String, String> valores = new HashMap<>();

        if (!Files.isRegularFile(arquivo)) {
            return valores;
        }

        try {
            for (String linha : Files.readAllLines(arquivo)) {
                String limpa = linha.strip();
                int igual = limpa.indexOf('=');

                if (limpa.isEmpty() || limpa.startsWith("#") || igual < 1) {
                    continue;
                }

                valores.put(limpa.substring(0, igual).strip(), limpa.substring(igual + 1).strip());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não deu para ler o " + arquivo, e);
        }

        return valores;
    }
}
