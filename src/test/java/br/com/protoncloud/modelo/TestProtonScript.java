package br.com.protoncloud.modelo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.codehaus.jettison.json.JSONObject;

import br.com.atomicsolutions.proton.ProtonAutomation;
import br.com.atomicsolutions.proton.ProtonComponentRunner;
import br.com.atomicsolutions.proton.ProtonMobileAutomation;
import br.com.protoncloud.modelo.apoio.Componente;
import br.com.protoncloud.modelo.apoio.Componentes;
import br.com.protoncloud.modelo.apoio.Config;
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
 * Sem o Proton, apague esta classe, a dependência {@code protoncloud-sdk} e o repositório
 * {@code protoncloud} do pom: o resto do projeto não muda.
 * </p>
 */
class TestProtonScript {

    @Test
    void executar() throws Exception {
        assumeTrue(System.getProperty("idDatasetRun") != null,
            "Só roda numa execução do Proton (-DidDatasetRun). Fora dele, os cenários rodam no CenariosTest.");

        // O runner pode rodar várias execuções ao mesmo tempo, na mesma pasta do projeto: cada
        // uma grava as evidências numa subpasta própria, senão um passo sobe o print da outra.
        Path evidencias = Config.pastaDeEvidencias().resolve(System.getProperty("idDatasetRun"));
        System.setProperty("PASTA_DE_EVIDENCIAS", evidencias.toString());

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

        try (var sessoes = Sessoes.abrir()) {
            ProtonComponentRunner.of(TestProtonScript::passo)
                .evidenceDir(Config.pastaDeEvidencias())
                .execute();
        }
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
        return componente == null ? null : componente::executar;
    }
}
