package br.com.protoncloud.modelo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.protoncloud.modelo.apoio.Cenarios;
import br.com.protoncloud.modelo.apoio.Navegador;

/** Os cenários da pasta {@code cenarios/}, sem o Proton: um teste por arquivo. */
class CenariosTest {

    static List<Path> cenarios() throws IOException {
        return Cenarios.listar();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cenarios")
    void cenario(Path arquivo) throws Exception {
        try (var navegador = Navegador.abrir()) {
            Cenarios.executar(arquivo);
        }
    }
}
