package br.com.protoncloud.modelo.componentes;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.protoncloud.modelo.apoio.Componente;
import br.com.protoncloud.modelo.paginas.PaginaDeLogin;
import br.com.protoncloud.modelo.paginas.PaginaDeProdutos;

/** FazerLogin: abre a loja e entra com usuário e senha. */
public class FazerLogin implements Componente {

    private static final Logger log = LoggerFactory.getLogger(FazerLogin.class);

    @Override
    public Map<String, String> executar(Map<String, String> parametros) {
        new PaginaDeLogin().abrir().entrar(parametros.get("in_usuario"), parametros.get("in_senha"));
        new PaginaDeProdutos().conferirQueAbriu();
        log.info("Login feito com {}", parametros.get("in_usuario"));
        return null;
    }
}
