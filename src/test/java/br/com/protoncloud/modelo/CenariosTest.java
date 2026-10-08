package br.com.protoncloud.modelo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.protoncloud.modelo.apoio.Cenarios;
import br.com.protoncloud.modelo.apoio.Config;
import br.com.protoncloud.modelo.apoio.Sessoes;

/**
 * Os cenários da pasta {@code cenarios/}, sem o Proton: um teste por arquivo.
 * {@code mvn test -Dcenario=compra} roda só os que têm "compra" no nome do arquivo.
 */
class CenariosTest {

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
        List<String> faltam = Cenarios.requisitosQueFaltam(Cenarios.carregar(arquivo));
        assumeTrue(faltam.isEmpty(), () -> "falta " + String.join(", ", faltam));

        try (var sessoes = Sessoes.abrir()) {
            Cenarios.executar(arquivo);
        }
    }
}
