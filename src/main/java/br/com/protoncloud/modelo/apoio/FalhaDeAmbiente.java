package br.com.protoncloud.modelo.apoio;

/**
 * Falha de ambiente: o ambiente não deixou a automação rodar, e o código dela não tem culpa (o
 * navegador que não abre, o Appium ou o aparelho fora do ar, a API que não responde quando a
 * sessão abre).
 * <p>
 * Lance com o erro original como causa, para o stack trace dele não se perder. Fora do Proton,
 * é uma falha como outra qualquer. Numa execução do Proton, o {@code TestProtonScript} a
 * entrega à SDK como falha de ambiente: a execução fica Failed, com o motivo de ambiente.
 * </p>
 */
public class FalhaDeAmbiente extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaDeAmbiente(String mensagem) {
        super(mensagem);
    }

    public FalhaDeAmbiente(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
