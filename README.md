# Projeto modelo Java

Automação de exemplo na loja de testes [Swag Labs](https://www.saucedemo.com): login,
carrinho e compra, e o login recusado de um usuário bloqueado. Serve de ponto de partida
para projetos novos de QA e de RPA.

O mesmo código roda de três jeitos:

- **pelo JUnit**, a partir dos cenários da pasta `cenarios/`;
- **como robô**, sem JUnit: `mvn compile exec:java`;
- **pelo [Proton](https://protoncloud.com.br)**, que dispara os mesmos componentes a partir
  de um dataset e guarda log, prints, saídas e resultado de cada execução.

O Proton é opcional. Tudo o que é dele fica na pasta `src/proton` e no perfil `proton` do
`pom.xml`, que só liga numa execução do Proton. Fora dele, o projeto nem baixa a SDK.
Apague a pasta e o perfil, e o resto continua igual. A automação é sua, com ou sem a
ferramenta.

## Rodar localmente

Pré-requisitos: Java 21 ou mais novo e Maven 3.9 ou mais novo.

```bash
cp .env.example .env
mvn test
```

Na primeira execução, a Playwright baixa os navegadores. Para usar o Chrome ou o Edge da
máquina, sem download, ponha `CANAL_DO_NAVEGADOR=chrome` (ou `msedge`) no `.env`. Para ver
o navegador trabalhando, `HEADLESS=false`.

Outros jeitos de rodar:

```bash
mvn test -Dcenario=compra                                             # só os cenários com "compra" no nome do arquivo
mvn compile exec:java -Dexec.args="cenarios/compra-com-sucesso.json"  # como robô, sem JUnit
```

Sem argumento, o `exec:java` roda todos os cenários.

Os prints ficam na pasta `evidencias/`.

## Estrutura

```
src/main/java/.../
  componentes/   uma classe por componente, que implementa Componente
  paginas/       as telas da loja: seletores e ações (page objects)
  apoio/         configuração, navegador, prints e a execução dos cenários
src/test/java/.../
  CenariosTest.java       roda os cenários, sem o Proton
src/proton/java/.../
  TestProtonScript.java   o ponto de entrada do Proton (só entra no build no Proton)
cenarios/                 os cenários em JSON, no mesmo formato de um dataset do Proton
```

Quem escreve a automação mexe em `componentes/`, `paginas/` e `cenarios/`. A pasta
`apoio/` raramente muda.

A automação fica em `src/main`, e não em `src/test`: ela é o produto, e não um teste.
Assim ela vira um jar comum, que roda de um agendador ou de outro projeto, e o JUnit fica
só em `src/test`.

## Criar um componente

Um componente é um passo da automação, com o mesmo nome no Proton e nos cenários. A classe
tem esse nome, no pacote `componentes`: o componente `ConsultarPedido` é a classe
`componentes/ConsultarPedido.java`.

```java
/** ConsultarPedido: abre o pedido e devolve o status. */
public class ConsultarPedido implements Componente {

    @Override
    public Map<String, String> executar(Map<String, String> parametros) {
        String status = new PaginaDePedidos().abrir(parametros.get("in_pedido")).status();

        if (status.equals("Cancelado")) {
            throw new AssertionError("O pedido " + parametros.get("in_pedido") + " está cancelado");
        }

        return Map.of("out_status", status);
    }
}
```

As regras:

- os parâmetros chegam num `Map`, com o prefixo `in_`;
- as saídas voltam num `Map`, com o prefixo `out_` (ou `null`, sem saída);
- para falhar, lance uma exceção. O print da tela da falha é automático;
- seletores e ações ficam nas páginas (`paginas`), não no componente;
- nada em `src/main` importa o Proton. O compilador garante: a SDK só existe no perfil
  `proton`.

## Cenários

Cada arquivo da pasta `cenarios/` é um cenário: os passos em ordem, com o componente e os
parâmetros, como um dataset do Proton.

```json
{
  "descricao": "Compra com sucesso",
  "passos": [
    {"componente": "FazerLogin", "parametros": {"in_usuario": "standard_user", "in_senha": "${env:SENHA_DA_LOJA}"}},
    {"componente": "AdicionarAoCarrinho", "parametros": {"in_produto": "Sauce Labs Backpack"}},
    {"componente": "FinalizarCompra", "parametros": {"in_nome": "Ana", "in_sobrenome": "Souza", "in_cep": "01310-100", "in_preco_esperado": "${out_preco}"}}
  ]
}
```

- `${out_preco}` usa a saída de um passo anterior, como a referência a saída no Proton.
- `${env:SENHA_DA_LOJA}` lê o valor de `-D`, do ambiente ou do `.env`. Senha não vai para o
  git; no Proton, o equivalente é o parâmetro criptografado.

## Rodar pelo Proton

1. **Sistema.** Cadastre um sistema e ligue-o a este repositório. O runner reconhece o
   projeto Java pelo `pom.xml` na raiz.
2. **Componentes.** Um componente no Proton para cada classe de `componentes`, com o mesmo
   nome e os parâmetros `in_` e `out_`.
3. **Automação e dataset.** Os passos do dataset são os componentes, em ordem, com os
   valores dos parâmetros. A saída de um passo vira entrada de outro pela referência a
   saída.
4. **Execução.** O runner roda `mvn test -DidDatasetRun=<id> -Dtest=TestProtonScript`. O
   `-DidDatasetRun` liga o perfil `proton`, que traz a SDK e compila o `TestProtonScript`.
   O runner entrega no ambiente o endereço do Proton e um token que vale só para aquela
   execução. O projeto não guarda token nem endereço do Proton.

A cada passo, a Proton Cloud SDK marca início e fim, passa ao componente só os parâmetros
dele, grava as saídas e sobe os prints que apareceram na pasta `evidencias/`. No fim,
grava o resultado; quando um passo falha, grava também o erro com o stack trace.

A máquina do runner precisa de Java, de Maven e de acesso à SDK. Hoje a SDK fica no GitHub
Packages, que pede um token do GitHub com `read:packages` no `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>proton-lib</id>
      <username>SEU_USUARIO_DO_GITHUB</username>
      <password>SEU_TOKEN_COM_READ_PACKAGES</password>
    </server>
  </servers>
</settings>
```

Para rodar o ponto de entrada do Proton na sua máquina, contra uma execução de verdade:
`mvn test -DidDatasetRun=<id> -Dtest=TestProtonScript`, com `PROTON_HOST` e `PROTON_TOKEN`
no ambiente.

## Com IA

O `AGENTS.md` traz as regras do projeto para agentes de IA (Claude Code, Codex, Cursor e
outros). Com o conector MCP do Proton, o agente cadastra no Proton os componentes que
criar aqui.

## Licença

[MIT](LICENSE).
