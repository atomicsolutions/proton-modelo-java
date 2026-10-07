package br.com.protoncloud.modelo.componentes;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.protoncloud.modelo.api.SessaoApi;
import br.com.protoncloud.modelo.api.ViaCep;
import br.com.protoncloud.modelo.apoio.Componente;

/** ConsultarCep: consulta o CEP de entrega na API pública do ViaCEP. */
public class ConsultarCep implements Componente {

    private static final Logger log = LoggerFactory.getLogger(ConsultarCep.class);

    @Override
    public Map<String, String> executar(Map<String, String> parametros) {
        var endereco = new ViaCep().consultar(parametros.get("in_cep"));
        SessaoApi.evidenciaDaUltimaResposta("consulta-do-cep");

        String cep = endereco.get("cep").getAsString();
        String cidade = endereco.get("localidade").getAsString();
        String uf = endereco.get("uf").getAsString();
        log.info("CEP {}: {}/{}", cep, cidade, uf);

        return Map.of("out_cep", cep, "out_cidade", cidade, "out_uf", uf);
    }
}
