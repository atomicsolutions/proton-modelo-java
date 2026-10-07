package br.com.protoncloud.modelo.apoio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * As sessões das plataformas da automação: navegador, API e o que mais o projeto usar.
 * <p>
 * Cada sessão abre na primeira vez que um passo pede e serve aos passos seguintes do mesmo
 * cenário, mesmo quando eles são de plataformas diferentes. No fim, todas fecham, na ordem
 * inversa, mesmo com erro. Um cenário só de API nem abre navegador.
 * </p>
 * <pre>{@code
 * try (var sessoes = Sessoes.abrir()) {
 *     ... // os passos usam SessaoWeb.pagina(), SessaoApi.get(...)
 * }
 * }</pre>
 * <p>
 * Para uma plataforma nova, crie o pacote dela (como {@code web}) com uma classe que
 * implemente {@link Sessao}, e peça a sessão com {@link #obter}.
 * </p>
 */
public final class Sessoes implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Sessoes.class);
    private static final Map<String, Sessao> ABERTAS = new LinkedHashMap<>();

    private Sessoes() {
    }

    /** O escopo de um cenário: no {@code close}, fecha todas as sessões que os passos abriram. */
    public static Sessoes abrir() {
        return new Sessoes();
    }

    /** A sessão da plataforma: abre na primeira vez, reaproveita nas seguintes. */
    public static <S extends Sessao> S obter(String plataforma, Class<S> tipo, Supplier<S> abrir) {
        Sessao sessao = ABERTAS.get(plataforma);

        if (sessao == null) {
            log.info("Abrindo a sessão {}", plataforma);
            sessao = abrir.get();
            ABERTAS.put(plataforma, sessao);
        }

        return tipo.cast(sessao);
    }

    /** As sessões abertas no cenário, na ordem em que abriram. */
    public static Map<String, Sessao> todas() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(ABERTAS));
    }

    @Override
    public void close() {
        List<Map.Entry<String, Sessao>> abertas = new ArrayList<>(ABERTAS.entrySet());
        Collections.reverse(abertas);

        for (var aberta : abertas) {
            try {
                aberta.getValue().close();
            } catch (RuntimeException e) {
                log.warn("Não deu para fechar a sessão {}", aberta.getKey(), e);
            }
        }

        ABERTAS.clear();
    }
}
