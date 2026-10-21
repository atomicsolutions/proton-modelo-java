package br.com.protoncloud.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Map;

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
import br.com.protoncloud.modelo.apoio.FalhaDeAmbiente;
import br.com.protoncloud.modelo.apoio.FalhaDeNegocio;
import br.com.protoncloud.modelo.apoio.Sessoes;

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

        // Execução mobile: o aparelho e o app escolhidos no disparo do Proton.
        JSONObject execucao = ProtonAutomation.getDatasetRunInfo();
        String aparelho = texto(execucao, "device");
        String app = texto(execucao, "app");

        if (aparelho != null) {
            System.setProperty("MOBILE_DISPOSITIVO", aparelho);
        }

        if (app != null) {
            String caminho = ProtonMobileAutomation.downloadMobileApp(app);

            if (!caminho.isBlank()) {
                System.setProperty("MOBILE_APP", caminho);
            }
        }

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

    private static String texto(JSONObject json, String campo) {
        // O jettison devolve o null do JSON como um objeto cujo toString() é null ou "null".
        Object valor = json == null ? null : json.opt(campo);
        String texto = valor == null ? null : valor.toString();
        return texto == null || texto.isBlank() || texto.equals("null") ? null : texto;
    }

    /** O passo do componente, ou null quando ele não é deste projeto (a SDK devolve ao runner). */
    private static ProtonComponentRunner.Step passo(String nome) {
        Componente componente = Componentes.localizar(nome);
        return componente == null ? null : comMotivo(componente);
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
