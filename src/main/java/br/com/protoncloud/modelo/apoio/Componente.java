package br.com.protoncloud.modelo.apoio;

import java.util.Map;

/**
 * Um componente: um passo da automação, com o mesmo nome no Proton e nos cenários.
 * <p>
 * Recebe os parâmetros ({@code in_...}) e devolve as saídas ({@code out_...}), ou
 * {@code null} quando não tem saída. Para falhar, lance uma exceção.
 * </p>
 */
@FunctionalInterface
public interface Componente {

    Map<String, String> executar(Map<String, String> parametros) throws Exception;
}
