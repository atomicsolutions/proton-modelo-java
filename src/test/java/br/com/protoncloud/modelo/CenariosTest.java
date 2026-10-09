package br.com.protoncloud.modelo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import br.com.protoncloud.modelo.apoio.Cenarios;
import br.com.protoncloud.modelo.apoio.Config;
import br.com.protoncloud.modelo.apoio.Sessoes;

/**
 * Os cenários da pasta {@code cenarios/}, sem o Proton: um teste por arquivo.
 * {@code mvn test -Dcenario=compra} roda só os que têm "compra" no nome do arquivo.
 */
class CenariosTest {

    private static final Logger log = LoggerFactory.getLogger(CenariosTest.class);

    static List<Path> cenarios() throws IOException {
        String filtro = Config.valor("cenario", "");
        List<Path> escolhidos = Cenarios.listar().stream()
            .filter(arquivo -> arquivo.getFileName().toString().contains(filtro))
            .toList();

        if (escolhidos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum cenário com \"" + filtro + "\" no nome, na pasta cenarios/.");
        }

        return escolhidos;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cenarios")
    void cenario(Path arquivo) throws Exception {
        JsonObject cenario = Cenarios.carregar(arquivo);
        List<String> faltam = Cenarios.requisitosQueFaltam(cenario);
        assumeTrue(faltam.isEmpty(), () -> "falta " + String.join(", ", faltam));

        Throwable erro = null;

        try (var sessoes = Sessoes.abrir()) {
            Cenarios.executar(arquivo);
        } catch (Exception | AssertionError e) {
            erro = e;
        }

        // O cenário com "falha_esperada" passa quando falha com aquela mensagem.
        String falhaEsperada = Cenarios.conferirFalhaEsperada(cenario, erro);

        if (falhaEsperada != null) {
            log.warn("Falhou como esperado (falha proposital): {}", falhaEsperada);
        }
    }
}
