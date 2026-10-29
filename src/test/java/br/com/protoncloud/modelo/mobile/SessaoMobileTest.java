package br.com.protoncloud.modelo.mobile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.protoncloud.modelo.apoio.Sessao;
import br.com.protoncloud.modelo.apoio.Sessoes;
import br.com.protoncloud.modelo.mobile.SessaoMobile.Aparelho;

/**
 * A sessão mobile por sistema: dois apps no mesmo cenário (o do cliente e o do entregador,
 * por exemplo) abrem dois aparelhos, cada um na sua sessão. Rodam sem aparelho e sem Appium.
 */
class SessaoMobileTest {

    private static final class SessaoFalsa implements Sessao {
        final Aparelho aparelho;
        boolean fechada;

        SessaoFalsa(Aparelho aparelho) {
            this.aparelho = aparelho;
        }

        @Override
        public Path evidencia(String nome) {
            return null;
        }

        @Override
        public void close() {
            fechada = true;
        }
    }

    @AfterEach
    void limpar() {
        SessaoMobile.usarAparelho(null);
    }

    private static SessaoFalsa abrir(Aparelho aparelho) {
        return Sessoes.obter(aparelho.chave(), SessaoFalsa.class, () -> new SessaoFalsa(aparelho));
    }

    @Test
    void cadaSistemaTemASuaSessao() {
        var cliente = new Aparelho("12", "R58M123", "cliente.apk");
        var entregador = new Aparelho("15", "R58M999", "entregador.apk");

        try (var sessoes = Sessoes.abrir()) {
            SessaoFalsa primeira = abrir(cliente);
            SessaoFalsa segunda = abrir(entregador);

            assertEquals("R58M123", primeira.aparelho.serial());
            assertEquals("R58M999", segunda.aparelho.serial());
            // O passo seguinte do primeiro sistema volta ao aparelho dele, sem abrir outro.
            assertSame(primeira, abrir(cliente));
            assertEquals(List.of("mobile:12", "mobile:15"), List.copyOf(Sessoes.todas().keySet()));
        }
    }

    @Test
    void asSessoesDosAparelhosFechamNoFimDoCenario() {
        SessaoFalsa primeira;
        SessaoFalsa segunda;

        try (var sessoes = Sessoes.abrir()) {
            primeira = abrir(new Aparelho("12", "R58M123", null));
            segunda = abrir(new Aparelho("15", "R58M999", null));
        }

        assertTrue(primeira.fechada && segunda.fechada);
        assertTrue(Sessoes.todas().isEmpty());
    }

    @Test
    void foraDoProtonValeOAparelhoDaConfiguracaoNumaSessaoSo() {
        Aparelho aparelho = SessaoMobile.aparelhoDoPasso();

        assertNull(aparelho.sistema());
        assertEquals("mobile", aparelho.chave());
    }

    @Test
    void oAparelhoDoPassoEOInformado() {
        var cliente = new Aparelho("12", "R58M123", "cliente.apk");
        SessaoMobile.usarAparelho(cliente);

        assertSame(cliente, SessaoMobile.aparelhoDoPasso());
        assertEquals("mobile:12", SessaoMobile.aparelhoDoPasso().chave());
    }

    @Test
    void cadaAparelhoLocalTemASuaPorta() throws Exception {
        int porta = SessaoMobile.portaLivre();

        // A porta devolvida está livre: dá para abrir um servidor nela.
        try (var servidor = new java.net.ServerSocket(porta)) {
            assertEquals(porta, servidor.getLocalPort());
        }
    }
}
