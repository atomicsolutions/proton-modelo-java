package br.com.protoncloud.modelo.componentes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.atomicsolutions.proton.ProtonRow;
import br.com.atomicsolutions.proton.ProtonTable;
import br.com.atomicsolutions.proton.ProtonTableException;
import br.com.protoncloud.modelo.apoio.Componente;
import br.com.protoncloud.modelo.apoio.Evidencias;
import br.com.protoncloud.modelo.apoio.FalhaDeNegocio;
import br.com.protoncloud.modelo.web.PaginaDeProdutos;
import br.com.protoncloud.modelo.web.SessaoWeb;

/**
 * AdicionarProdutosDaTabela: põe no carrinho cada produto da aba {@code in_aba} da tabela de
 * dados (padrão {@code produtos}) e grava o resultado na mesma linha.
 * <p>
 * A aba tem a coluna {@code produto} e as colunas que o passo preenche: {@code status}
 * ({@code no carrinho} ou {@code erro}), {@code preco} e {@code mensagem}. O passo cria a
 * coluna {@code processado_em} quando ela não existe e, no fim, inclui uma linha de total.
 * Linha com {@code status} preenchido fica de fora: é o resultado de uma execução anterior
 * (levado de volta para o dataset) ou a linha de total.
 * </p>
 * <p>
 * Um produto que não entra no carrinho marca a linha com erro, e as outras seguem. Só falha
 * o passo quando nenhum produto entra, ou quando a própria tabela recusa a escrita.
 * </p>
 * <p>
 * A tabela de dados vem da Proton Cloud SDK ({@link ProtonTable}). Numa execução do Proton,
 * cada execução lê e grava a cópia dela; fora do Proton, a tabela vem do .csv de
 * {@code PROTON_TABLE_FILE} (o {@code "tabela"} do cenário), e as escritas ficam só na memória.
 * </p>
 */
public class AdicionarProdutosDaTabela implements Componente {

    private static final Logger log = LoggerFactory.getLogger(AdicionarProdutosDaTabela.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, String> executar(Map<String, String> parametros) throws IOException {
        String aba = parametros.getOrDefault("in_aba", "");
        ProtonTable produtos = ProtonTable.get(aba.isBlank() ? "produtos" : aba);

        if (produtos.getColumns().stream().noneMatch(coluna -> coluna.equalsIgnoreCase("processado_em"))) {
            produtos.addColumn("processado_em");
        }

        PaginaDeProdutos loja = new PaginaDeProdutos();
        List<ProtonRow> pendentes = produtos.getRows().stream()
            .filter(linha -> !linha.get("produto").isBlank() && linha.get("status").isBlank())
            .toList();
        int noCarrinho = 0;
        int comErro = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (ProtonRow linha : pendentes) {
            String produto = linha.get("produto").strip();

            try {
                if (!loja.temProduto(produto)) {
                    throw new FalhaDeNegocio("O produto " + produto + " não existe na loja");
                }

                String preco = loja.adicionarAoCarrinho(produto);
                noCarrinho++;
                total = total.add(new BigDecimal(preco));
                log.info("{} no carrinho, por {}", produto, preco);
                produtos.set(linha, "status", "no carrinho");
                produtos.set(linha, "preco", preco);
                produtos.set(linha, "mensagem", "");
            } catch (ProtonTableException e) {
                throw e; // a tabela recusou: coluna errada, execução terminada. Não adianta seguir.
            } catch (RuntimeException | AssertionError e) {
                comErro++;
                log.warn("{}: {}", produto, e.getMessage());
                produtos.set(linha, "status", "erro");
                produtos.set(linha, "mensagem", primeiraLinha(e.getMessage()));
            }

            produtos.set(linha, "processado_em", LocalDateTime.now().format(HORA));
        }

        SessaoWeb.printDaTela("produtos-da-tabela-no-carrinho");

        Map<String, String> linhaDeTotal = new LinkedHashMap<>();
        linhaDeTotal.put("produto", "Total");
        linhaDeTotal.put("status", noCarrinho + " de " + pendentes.size() + " no carrinho");
        linhaDeTotal.put("preco", total.toPlainString());
        linhaDeTotal.put("processado_em", LocalDateTime.now().format(HORA));
        produtos.addRow(linhaDeTotal);

        // A aba como ficou, em JSON, também como evidência: fora do Proton, as escritas ficam
        // só na memória, e é por ela que se confere o que o passo gravou.
        Path resultado = Evidencias.arquivo("tabela-" + produtos.getName(), "json");
        Files.writeString(resultado, produtos.toJson());
        Evidencias.registrar(resultado);

        if (noCarrinho == 0) {
            throw new FalhaDeNegocio("Nenhum dos " + pendentes.size() + " produtos da aba " + produtos.getName()
                + " entrou no carrinho");
        }

        return Map.of(
            "out_total_dos_itens", total.toPlainString(),
            "out_no_carrinho", String.valueOf(noCarrinho),
            "out_com_erro", String.valueOf(comErro));
    }

    private static String primeiraLinha(String mensagem) {
        String texto = mensagem == null ? "" : mensagem.lines().findFirst().orElse("");
        return texto.length() > 500 ? texto.substring(0, 500) : texto;
    }
}
