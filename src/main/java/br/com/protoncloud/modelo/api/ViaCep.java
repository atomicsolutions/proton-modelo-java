package br.com.protoncloud.modelo.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.protoncloud.modelo.apoio.Config;

/** A API pública de CEP do ViaCEP (viacep.com.br). */
public class ViaCep {

    /** O endereço do CEP; erro se o CEP não existe. */
    public JsonObject consultar(String cep) {
        String digitos = cep.replaceAll("\\D", "");

        if (digitos.length() != 8) {
            throw new AssertionError("CEP \"" + cep + "\" inválido: são 8 dígitos");
        }

        var resposta = SessaoApi.get(Config.urlDaApiDeCep() + "/" + digitos + "/json/");

        if (resposta.statusCode() != 200) {
            throw new AssertionError("A API de CEP respondeu " + resposta.statusCode() + " para o CEP " + cep);
        }

        JsonObject endereco = JsonParser.parseString(resposta.body()).getAsJsonObject();

        if (endereco.has("erro")) {
            throw new AssertionError("CEP " + cep + " não encontrado");
        }

        return endereco;
    }
}
