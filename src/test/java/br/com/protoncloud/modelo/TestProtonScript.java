package br.com.protoncloud.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import org.codehaus.jettison.json.JSONObject;

import br.com.atomicsolutions.proton.FailureReason;
import br.com.atomicsolutions.proton.ProtonAutomation;
import br.com.atomicsolutions.proton.ProtonBusinessError;
import br.com.atomicsolutions.proton.ProtonComponentRunner;
import br.com.atomicsolutions.proton.ProtonEnvironmentError;
import br.com.atomicsolutions.proton.ProtonMobileAutomation;
import br.com.protoncloud.modelo.apoio.Componente;
import br.com.protoncloud.modelo.apoio.Componentes;
import br.com.protoncloud.modelo.apoio.Config;
import br.com.protoncloud.modelo.apoio.FalhaDeAmbiente;
import br.com.protoncloud.modelo.apoio.FalhaDeNegocio;
import br.com.protoncloud.modelo.apoio.Sessoes;
import br.com.protoncloud.modelo.mobile.SessaoMobile;
import br.com.protoncloud.modelo.mobile.SessaoMobile.Aparelho;

/**
 * Ponto de entrada do runner do Proton, e a única classe do projeto que conhece o Proton.
 * <p>
 * O runner roda {@code mvn test -DidDatasetRun=<id> -Dtest=TestProtonScript}, e a SDK roda
 * os passos do dataset na ordem: busca os parâmetros de cada um, chama o componente, grava
 * as saídas, sobe as evidências e registra o resultado. Os passos podem ser de plataformas
 * diferentes (cada uma um sistema no Proton, todos ligados a este repositório): a SDK segue
 * enquanto este projeto tiver o componente, e as sessões abertas valem para a execução
 * inteira.
 * </p>
 * <p>
 * O que muda numa execução do Proton fica todo aqui: a pasta de evidências da execução, as
 * sessões fechadas pela SDK antes do status e o motivo da falha ({@link FalhaDeNegocio} e
 * {@link FalhaDeAmbiente} viram as exceções da SDK).
 * </p>
 * <p>
 * Sem o Proton, apague esta classe, a dependência {@code protoncloud-sdk} e o repositório
 * {@code protoncloud} do pom: o resto do projeto não muda.
 * </p>
 */
class TestProtonScript {

    @Test
    void executar() throws Exception {
        assumeTrue(System.getProperty("idDatasetRun") != null,
            "Só roda numa execução do Proton (-DidDatasetRun). Fora dele, os cenários rodam no CenariosTest.");

        // O runner pode rodar várias execuções ao mesmo tempo, na mesma pasta do projeto. A SDK
        // dá a cada uma a pasta dela (evidencias/<id da execução>) e não sobe o que as outras
        // gravam nas delas.
        System.setProperty("PASTA_DE_EVIDENCIAS", ProtonComponentRunner.runEvidenceDir().toString());

        // As sessões abrem na primeira vez que um passo pede. A SDK as fecha antes de gravar o
        // status (e antes de devolver ao runner o passo de outro projeto), e sobe o que sair
        // do fechamento. O try fecha o que sobrar: fechar de novo não faz nada.
        try (var sessoes = Sessoes.abrir()) {
            ProtonComponentRunner.of(TestProtonScript::passo)
                .closeAtEnd(sessoes)
                .execute();
        }
    }

    /** O motivo que o Proton grava junto com o Failed, para cada tipo de falha do projeto. */
    @Test
    void motivoDaFalha() {
        var negocio = new FalhaDeNegocio("A loja recusou os dados de entrega: Error: Last Name is required");
        var ambiente = new FalhaDeAmbiente("O Chromium da Playwright não abriu", new IllegalStateException("sem navegador"));

        assertEquals("business_error", FailureReason.of(falhaNoProton(negocio)));
        assertEquals("environment_error", FailureReason.of(falhaNoProton(ambiente)));
        assertEquals("technical_error", FailureReason.of(falhaNoProton(new IllegalStateException("O elemento não apareceu"))));
        assertEquals("technical_error", FailureReason.of(falhaNoProton(new AssertionError("Total dos itens 7.99; o esperado era 29.99"))));

        // A falha do projeto segue como causa, com a mensagem e o stack trace dela.
        assertSame(negocio, falhaNoProton(negocio).getCause());
        assertSame(ambiente, falhaNoProton(ambiente).getCause());
    }

    /**
     * O passo do componente, ou null quando ele não é deste projeto (a SDK devolve ao runner).
     * Cada passo leva o aparelho do sistema dele: uma execução pode ter vários sistemas mobile.
     */
    private static ProtonComponentRunner.Step passo(String nome) {
        Componente componente = Componentes.localizar(nome);

        if (componente == null) {
            return null;
        }

        // Lido agora, antes de o passo começar: o Proton responde pelo passo que ainda não começou.
        Aparelho aparelho = aparelho(ProtonAutomation.getCurrentComponentDevice(), TestProtonScript::baixarApp);
        return comAparelho(aparelho, comMotivo(componente));
    }

    private static final Map<String, String> APPS_BAIXADOS = new ConcurrentHashMap<>();

    /** O app de cada sistema, baixado uma vez por execução. */
    private static String baixarApp(String idDoApp) {
        return APPS_BAIXADOS.computeIfAbsent(idDoApp, ProtonMobileAutomation::downloadMobileApp);
    }

    /**
     * O aparelho e o app que o disparo escolheu para o sistema do passo, ou null (passo de outra
     * plataforma: vale o da configuração).
     */
    static Aparelho aparelho(JSONObject doProton, UnaryOperator<String> baixar) {
        if (doProton == null) {
            return null;
        }

        String serial = doProton.optString("device", "");
        String idDoApp = doProton.optString("app", "");
        String sistema = doProton.optString("idSystem", "");
        String app = idDoApp.isEmpty() ? "" : baixar.apply(idDoApp);

        return new Aparelho(sistema.isEmpty() ? null : sistema,
            serial.isEmpty() ? Config.mobileDispositivo() : serial,
            app == null || app.isBlank() ? Config.mobileApp() : app);
    }

    /** O passo com o aparelho do sistema dele. */
    static ProtonComponentRunner.Step comAparelho(Aparelho aparelho, ProtonComponentRunner.Step passo) {
        return parametros -> {
            SessaoMobile.usarAparelho(aparelho);
            return passo.run(parametros);
        };
    }

    /**
     * O componente como passo da SDK, com o motivo da falha: {@link FalhaDeNegocio} vira
     * {@link ProtonBusinessError} e {@link FalhaDeAmbiente} vira {@link ProtonEnvironmentError},
     * com a original como causa. Qualquer outra exceção segue como está, e o Proton a conta
     * como falha técnica.
     */
    private static ProtonComponentRunner.Step comMotivo(Componente componente) {
        return parametros -> {
            try {
                return componente.executar(parametros);
            } catch (FalhaDeNegocio e) {
                throw new ProtonBusinessError(e.getMessage(), e);
            } catch (FalhaDeAmbiente e) {
                throw new ProtonEnvironmentError(e.getMessage(), e);
            }
        };
    }

    /** Cada passo leva o aparelho e o app do sistema dele: uma execução pode ter vários sistemas mobile. */
    @Test
    void cadaPassoUsaOAparelhoDoSistemaDele() throws Exception {
        var doCliente = new JSONObject("{\"idSystem\": 12, \"deviceFarm\": \"local\", \"device\": \"R58M123\", \"app\": \"301\"}");
        var doEntregador = new JSONObject("{\"idSystem\": 15, \"deviceFarm\": \"local\", \"device\": \"R58M999\", \"app\": \"302\"}");
        UnaryOperator<String> baixar = id -> "/apps/" + id + ".apk";

        assertEquals(new Aparelho("12", "R58M123", "/apps/301.apk"), aparelho(doCliente, baixar));
        assertEquals(new Aparelho("15", "R58M999", "/apps/302.apk"), aparelho(doEntregador, baixar));
        assertEquals("mobile:15", aparelho(doEntregador, baixar).chave());

        try {
            var cliente = aparelho(doCliente, baixar);
            comAparelho(cliente, parametros -> {
                assertSame(cliente, SessaoMobile.aparelhoDoPasso());
                return Map.of();
            }).run(Map.of());

            // Passo de outra plataforma: volta ao aparelho da configuração.
            comAparelho(aparelho(null, baixar), parametros -> {
                assertEquals("mobile", SessaoMobile.aparelhoDoPasso().chave());
                return Map.of();
            }).run(Map.of());
        } finally {
            SessaoMobile.usarAparelho(null);
        }
    }

    /** Sem app escolhido no disparo, vale o app da configuração (ou o já instalado). */
    @Test
    void aparelhoSemAppNaoBaixaNada() throws Exception {
        var semApp = new JSONObject("{\"idSystem\": 12, \"device\": \"R58M123\"}");

        Aparelho aparelho = aparelho(semApp, id -> {
            throw new AssertionError("não devia baixar");
        });

        assertEquals("R58M123", aparelho.serial());
        assertEquals(Config.mobileApp(), aparelho.app());
    }

    /** O que a SDK recebe do passo quando o componente lança o erro. */
    private static Throwable falhaNoProton(Throwable erro) {
        Componente componente = parametros -> {
            if (erro instanceof Error grave) {
                throw grave;
            }

            throw (Exception) erro;
        };

        return assertThrows(Throwable.class, () -> comMotivo(componente).run(Map.of()));
    }
}
