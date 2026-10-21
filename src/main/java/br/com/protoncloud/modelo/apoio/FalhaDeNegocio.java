package br.com.protoncloud.modelo.apoio;

/**
 * Falha de negócio: a regra do sistema não deixou seguir, e repetir dá o mesmo resultado (o
 * cadastro que já existe, o pedido recusado, os dados de entrega que a loja não aceita).
 * <p>
 * Lance onde a recusa aparece: no componente, na página ou no cliente. Fora do Proton, é uma
 * falha como outra qualquer. Numa execução do Proton, o {@code TestProtonScript} a entrega à
 * SDK como falha de negócio: a execução fica Failed, com o motivo de negócio.
 * </p>
 */
public class FalhaDeNegocio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaDeNegocio(String mensagem) {
        super(mensagem);
    }

    public FalhaDeNegocio(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
