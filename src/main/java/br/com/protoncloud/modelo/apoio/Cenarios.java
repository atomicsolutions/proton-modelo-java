package br.com.protoncloud.modelo.apoio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Roda um cenário a partir de um arquivo JSON, sem o Proton.
 * <p>
 * O arquivo tem o mesmo formato de um dataset do Proton: os passos em ordem, cada um com o
 * componente e os parâmetros. Os passos podem ser de plataformas diferentes (web, API,
 * mobile): cada sessão abre quando o primeiro passo dela pede.
 * </p>
 * <pre>{@code
 * {
 *   "descricao": "Compra com sucesso",
 *   "passos": [
 *     {"componente": "FazerLogin", "parametros": {"in_usuario": "standard_user"}},
 *     {"componente": "AdicionarAoCarrinho", "parametros": {"in_produto": "Sauce Labs Backpack"}}
 *   ]
 * }
 * }</pre>
 * <p>Dois tipos de referência no valor de um parâmetro:</p>
 * <ul>
 * <li>{@code ${out_preco}}: a saída de um passo anterior, como a referência a saída no Proton;</li>
 * <li>{@code ${env:SENHA_DA_LOJA}}: um valor de configuração ({@link Config}), para segredo
 * não ir para o git (no Proton, o equivalente é o parâmetro criptografado).</li>
 * </ul>
 * <p>
 * {@code "requer"} (opcional) lista o que o cenário precisa da máquina: {@code android} (adb e
 * um aparelho conectado). Sem isso, o cenário é pulado, e não falha.
 * </p>
 */
public final class Cenarios {

    private static final Logger log = LoggerFactory.getLogger(Cenarios.class);
    private static final Pattern REFERENCIA = Pattern.compile("\\$\\{(env:)?([A-Za-z0-9_]+)}");

    private Cenarios() {
    }

    /** Os cenários da pasta {@code cenarios/}. */
    public static List<Path> listar() throws IOException {
        try (Stream<Path> arquivos = Files.list(Config.pastaDeCenarios())) {
            return arquivos.filter(arquivo -> arquivo.toString().endsWith(".json")).sorted().toList();
        }
    }

    public static JsonObject carregar(Path arquivo) throws IOException {
        return JsonParser.parseString(Files.readString(arquivo)).getAsJsonObject();
    }

    /** O que o cenário pede ({@code "requer"}) e esta máquina não tem. */
    public static List<String> requisitosQueFaltam(JsonObject cenario) {
        List<String> faltam = new ArrayList<>();

        if (cenario.has("requer")) {
            for (JsonElement requisito : cenario.getAsJsonArray("requer")) {
                if (requisito.getAsString().equals("android") && !aparelhoAndroidConectado()) {
                    faltam.add("android (adb e um aparelho conectado)");
                }
            }
        }

        return faltam;
    }

    private static boolean aparelhoAndroidConectado() {
        try {
            Process adb = new ProcessBuilder("adb", "devices").redirectErrorStream(true).start();
            String saida = new String(adb.getInputStream().readAllBytes());
            adb.waitFor();
            List<String> aparelhos = saida.lines().skip(1)
                .filter(linha -> linha.strip().endsWith("device"))
                .map(linha -> linha.split("\\s+")[0])
                .toList();
            String escolhido = Config.mobileDispositivo();
            return !aparelhos.isEmpty() && (escolhido == null || aparelhos.contains(escolhido));
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Roda os passos do cenário e devolve as saídas de todos eles. */
    public static Map<String, String> executar(Path arquivo) throws Exception {
        JsonObject cenario = carregar(arquivo);
        log.info("Cenário: {}", cenario.has("descricao") ? cenario.get("descricao").getAsString() : arquivo.getFileName());
        Map<String, String> saidas = new HashMap<>();
        int numero = 0;

        for (JsonElement elemento : cenario.getAsJsonArray("passos")) {
            JsonObject passo = elemento.getAsJsonObject();
            String componente = passo.get("componente").getAsString();
            Map<String, String> parametros = new LinkedHashMap<>();

            if (passo.has("parametros")) {
                for (var parametro : passo.getAsJsonObject("parametros").entrySet()) {
                    String valor = parametro.getValue().isJsonNull() ? "" : parametro.getValue().getAsString();
                    parametros.put(parametro.getKey(), resolver(valor, saidas));
                }
            }

            Componente implementacao = Componentes.localizar(componente);

            if (implementacao == null) {
                throw new IllegalArgumentException("O componente \"" + componente + "\" não existe: crie a classe "
                    + Componentes.PACOTE + "." + Componentes.nomeDaClasse(componente) + ".");
            }

            log.info("Passo {}: {}", ++numero, componente);
            Map<String, String> geradas = implementacao.executar(parametros);

            if (geradas != null) {
                saidas.putAll(geradas);
            }
        }

        return saidas;
    }

    /**
     * Roda cenários sem JUnit e sem Proton, como um robô comum. Sem argumento, roda todos
     * os da pasta {@code cenarios/}.
     */
    public static void main(String[] args) throws IOException {
        List<Path> arquivos = args.length > 0 ? Stream.of(args).map(Path::of).toList() : listar();
        int falhas = 0;

        for (Path arquivo : arquivos) {
            List<String> faltam = requisitosQueFaltam(carregar(arquivo));

            if (!faltam.isEmpty()) {
                log.warn("Pulado: {} (falta {})", arquivo, String.join(", ", faltam));
                continue;
            }

            try (var sessoes = Sessoes.abrir()) {
                executar(arquivo);
            } catch (Exception | AssertionError e) {
                log.error("Falhou: {}", arquivo, e);
                falhas++;
            }
        }

        System.exit(falhas > 0 ? 1 : 0);
    }

    private static String resolver(String valor, Map<String, String> saidas) {
        Matcher referencia = REFERENCIA.matcher(valor);
        StringBuilder resolvido = new StringBuilder();

        while (referencia.find()) {
            String nome = referencia.group(2);
            String substituto;

            if (referencia.group(1) != null) {
                substituto = Config.obrigatorio(nome);
            } else if (saidas.containsKey(nome)) {
                substituto = saidas.get(nome);
            } else {
                throw new IllegalStateException("A saída \"" + nome + "\" ainda não foi gerada por nenhum passo anterior.");
            }

            referencia.appendReplacement(resolvido, Matcher.quoteReplacement(substituto));
        }

        referencia.appendTail(resolvido);
        return resolvido.toString();
    }
}
