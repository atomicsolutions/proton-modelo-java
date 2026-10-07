package br.com.protoncloud.modelo.apoio;

import java.lang.reflect.InvocationTargetException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Do nome do componente à classe que o executa.
 * <p>
 * O nome é o mesmo no Proton e nos cenários ({@code FazerLogin}), e a classe tem esse nome
 * no pacote {@code componentes} ({@code componentes/FazerLogin.java}). Quando o passo
 * falha, fica a evidência de cada sessão aberta, com e sem o Proton.
 * </p>
 */
public final class Componentes {

    public static final String PACOTE = "br.com.protoncloud.modelo.componentes";

    private Componentes() {
    }

    /** {@code FazerLogin}, {@code Fazer login} e {@code fazer_login} viram {@code FazerLogin}. */
    public static String nomeDaClasse(String componente) {
        String semAcento = Normalizer.normalize(componente, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        return Arrays.stream(semAcento.split("[^0-9A-Za-z]+"))
            .filter(parte -> !parte.isEmpty())
            .map(parte -> Character.toUpperCase(parte.charAt(0)) + parte.substring(1))
            .collect(Collectors.joining());
    }

    /** O componente pelo nome, ou {@code null} se ele não é deste projeto. */
    public static Componente localizar(String componente) {
        String classe = PACOTE + "." + nomeDaClasse(componente);
        Componente implementacao;

        try {
            implementacao = (Componente) Class.forName(classe).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("O construtor de " + classe + " falhou.", e.getCause());
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException(classe + " precisa implementar Componente e ter um construtor público sem parâmetros.", e);
        }

        return parametros -> {
            try {
                return implementacao.executar(parametros);
            } catch (Exception | AssertionError e) {
                Evidencias.daFalha(nomeDaClasse(componente));
                throw e;
            }
        };
    }
}
