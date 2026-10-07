package br.com.protoncloud.modelo.apoio;

import java.nio.file.Path;

/**
 * A sessão de uma plataforma: o navegador, o cliente HTTP, o aplicativo. Abre na primeira
 * vez que um passo pede ({@link Sessoes#obter}) e fecha no fim do cenário.
 */
public interface Sessao extends AutoCloseable {

    /** Grava a evidência da tela (ou da última resposta) e devolve o arquivo, ou {@code null}. */
    Path evidencia(String nome);

    @Override
    void close();
}
