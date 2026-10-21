package br.com.protoncloud.modelo.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import br.com.protoncloud.modelo.apoio.Config;
import br.com.protoncloud.modelo.apoio.Evidencias;
import br.com.protoncloud.modelo.apoio.FalhaDeAmbiente;
import br.com.protoncloud.modelo.apoio.Sessao;
import br.com.protoncloud.modelo.apoio.Sessoes;

/**
 * A sessão de API: um cliente HTTP (java.net.http) por cenário, compartilhado pelos passos.
 * <p>
 * O cliente tenta de novo, com espera crescente, quando a conexão cai ou o servidor
 * responde 502, 503 ou 504. A API que segue sem responder, ou indisponível, depois das
 * tentativas é {@link FalhaDeAmbiente}, em qualquer chamada do cenário; a mensagem leva só
 * o host, porque o endereço pode ter chave. A evidência de uma chamada é a última resposta,
 * gravada em JSON: método, endereço, status e corpo. Os cabeçalhos ficam de fora, porque
 * levam credenciais.
 * </p>
 */
public final class SessaoApi implements Sessao {

    private static final Logger log = LoggerFactory.getLogger(SessaoApi.class);
    private static final int TENTATIVAS = 4;
    private static final Set<Integer> INDISPONIVEL = Set.of(502, 503, 504);

    private final HttpClient cliente = HttpClient.newBuilder()
        .connectTimeout(Config.timeoutDaApi())
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private HttpResponse<String> ultimaResposta;

    private SessaoApi() {
    }

    private static SessaoApi sessao() {
        return Sessoes.obter("api", SessaoApi.class, SessaoApi::new);
    }

    /** GET no endereço, com as novas tentativas. */
    public static HttpResponse<String> get(String url) {
        return sessao().enviar(HttpRequest.newBuilder(URI.create(url)).timeout(Config.timeoutDaApi()).GET().build());
    }

    /** Grava a última resposta como evidência na pasta de evidências. */
    public static Path evidenciaDaUltimaResposta(String nome) {
        return sessao().evidencia(nome);
    }

    private HttpResponse<String> enviar(HttpRequest pedido) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                ultimaResposta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());

                if (!INDISPONIVEL.contains(ultimaResposta.statusCode())) {
                    return ultimaResposta;
                }

                if (tentativa == TENTATIVAS) {
                    throw new FalhaDeAmbiente("A API ficou indisponível: " + pedido.uri().getHost() + " respondeu "
                        + ultimaResposta.statusCode() + " nas " + TENTATIVAS + " tentativas");
                }

                log.warn("{} respondeu {} (tentativa {} de {})", pedido.uri(), ultimaResposta.statusCode(), tentativa, TENTATIVAS);
            } catch (IOException e) {
                // Sem resposta depois das tentativas, a API está fora do ar ou fora do alcance da
                // máquina (rede, proxy, VPN): é o ambiente, como no modelo Python.
                if (tentativa == TENTATIVAS) {
                    throw new FalhaDeAmbiente("A API não respondeu: " + pedido.uri().getHost()
                        + " (" + TENTATIVAS + " tentativas)", e);
                }

                log.warn("Rede instável ao chamar {} (tentativa {} de {}): {}", pedido.uri(), tentativa, TENTATIVAS, e.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Chamada interrompida: " + pedido.uri(), e);
            }

            esperar(Duration.ofSeconds(1L << (tentativa - 1)));
        }
    }

    private static void esperar(Duration espera) {
        try {
            Thread.sleep(espera);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Path evidencia(String nome) {
        if (ultimaResposta == null) {
            return null;
        }

        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("metodo", ultimaResposta.request().method());
        registro.put("url", ultimaResposta.uri().toString());
        registro.put("status", ultimaResposta.statusCode());

        try {
            registro.put("resposta", JsonParser.parseString(ultimaResposta.body()));
        } catch (JsonSyntaxException e) {
            registro.put("resposta", ultimaResposta.body());
        }

        Path arquivo = Evidencias.arquivo(nome, "json");

        try {
            Files.writeString(arquivo, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(registro));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Evidencias.registrar(arquivo);
    }

    @Override
    public void close() {
        cliente.close();
    }
}
