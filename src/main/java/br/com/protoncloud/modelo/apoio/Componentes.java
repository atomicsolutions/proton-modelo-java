package br.com.protoncloud.modelo.apoio;

import java.lang.reflect.InvocationTargetException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Do nome do componente à classe que o executa.
 * <p>
 * O nome é o mesmo no Proton e nos cenários ({@code FazerLogin}), e a classe tem esse nome
 * no pacote {@code componentes} ({@code componentes/FazerLogin.java}). Quando o passo
 * falha, fica um print da tela na pasta de evidências, com e sem o Proton.
 * </p>
 */
public final class Componentes {

    public static final String PACOTE = "br.com.protoncloud.modelo.componentes";

    private static final Logger log = LoggerFactory.getLogger(Componentes.class);

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

    /** O componente pelo nome; um erro claro se ele não existe no projeto. */
    public static Componente localizar(String componente) {
        String classe = PACOTE + "." + nomeDaClasse(componente);
        Componente implementacao;

        try {
            implementacao = (Componente) Class.forName(classe).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("O componente \"" + componente + "\" não existe: crie a classe " + classe + ".");
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("O construtor de " + classe + " falhou.", e.getCause());
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException(classe + " precisa implementar Componente e ter um construtor público sem parâmetros.", e);
        }

        return parametros -> {
            try {
                return implementacao.executar(parametros);
            } catch (Exception | AssertionError e) {
                printDaFalha(componente);
                throw e;
            }
        };
    }

    private static void printDaFalha(String componente) {
        try {
            Evidencias.printDaTela("falha-" + nomeDaClasse(componente));
        } catch (RuntimeException e) {
            // Sem print (navegador fechado, por exemplo), o erro do passo é o que importa.
            log.warn("Não deu para gravar o print da falha de {}", componente);
        }
    }
}
