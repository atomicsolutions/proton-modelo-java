package br.com.protoncloud.modelo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import br.com.atomicsolutions.proton.ProtonComponentRunner;
import br.com.protoncloud.modelo.apoio.Componentes;
import br.com.protoncloud.modelo.apoio.Config;
import br.com.protoncloud.modelo.apoio.Navegador;

/**
 * Ponto de entrada do runner do Proton, e a única classe do projeto que conhece o Proton.
 * <p>
 * O runner roda {@code mvn test -DidDatasetRun=<id> -Dtest=TestProtonScript}, e a SDK roda
 * os passos do dataset na ordem: busca os parâmetros de cada um, chama o componente, grava
 * as saídas, sobe os prints e registra o resultado.
 * </p>
 * <p>
 * Esta pasta ({@code src/proton/java}) e a SDK só entram no build com o perfil
 * {@code proton} do pom, que liga sozinho com {@code -DidDatasetRun}. Sem o Proton, apague
 * a pasta e o perfil: o resto do projeto não muda.
 * </p>
 */
class TestProtonScript {

    @Test
    void executar() throws Exception {
        assumeTrue(System.getProperty("idDatasetRun") != null,
            "Só roda numa execução do Proton (-DidDatasetRun). Fora dele, os cenários rodam no CenariosTest.");

        try (var navegador = Navegador.abrir()) {
            ProtonComponentRunner.of(nome -> Componentes.localizar(nome)::executar)
                .evidenceDir(Config.pastaDeEvidencias())
                .execute();
        }
    }
}
