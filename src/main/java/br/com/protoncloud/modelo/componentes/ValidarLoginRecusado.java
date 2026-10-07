package br.com.protoncloud.modelo.componentes;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.protoncloud.modelo.apoio.Componente;
import br.com.protoncloud.modelo.apoio.Evidencias;
import br.com.protoncloud.modelo.paginas.PaginaDeLogin;

/** ValidarLoginRecusado: tenta entrar e confere a mensagem de erro do login recusado. */
public class ValidarLoginRecusado implements Componente {

    private static final Logger log = LoggerFactory.getLogger(ValidarLoginRecusado.class);

    @Override
    public Map<String, String> executar(Map<String, String> parametros) {
        var login = new PaginaDeLogin().abrir();
        login.entrar(parametros.get("in_usuario"), parametros.get("in_senha"));
        String mensagem = login.mensagemDeErro();
        String esperada = parametros.get("in_mensagem");

        if (!mensagem.contains(esperada)) {
            throw new AssertionError("Mensagem \"" + mensagem + "\"; o esperado era conter \"" + esperada + "\"");
        }

        Evidencias.printDaTela("login-recusado");
        log.info("Login recusado, como esperado: {}", mensagem);
        return null;
    }
}
