# Projeto modelo Java

Automação de exemplo com mais de uma plataforma no mesmo cenário: consulta um CEP numa API
pública ([ViaCEP](https://viacep.com.br)), faz uma compra na loja de testes
[Swag Labs](https://www.saucedemo.com) e põe o mesmo produto no carrinho de um app Android.
Serve de ponto de partida para projetos novos de QA e de RPA.

| Plataforma | Biblioteca | Pacote |
|---|---|---|
| Web | [Playwright](https://playwright.dev/java/) | `web` |
| API | `java.net.http` (JDK) e [Gson](https://github.com/google/gson) | `api` |
| Mobile (Android) | [Appium](https://appium.io) (java-client) | `mobile` |

Para SAP GUI e desktop Windows, use o projeto modelo Python: o SAP GUI Scripting e a
automação de desktop são nativos em Python (pywin32 e pywinauto), e em Java dependeriam de
pontes como o JACOB ou o Appium com um driver de Windows.

O mesmo código roda de três jeitos:

- **pelo JUnit**, a partir dos cenários da pasta `cenarios/`;
- **como robô**, sem JUnit: `mvn compile exec:java`;
- **pelo [Proton](https://protoncloud.com.br)**, que dispara os mesmos componentes a partir
  de um dataset e guarda log, prints, saídas e resultado de cada execução.

O Proton é opcional. Só uma classe do projeto conhece o Proton: o `TestProtonScript` (e o
exemplo da [tabela de dados](#tabela-de-dados), que é um recurso do Proton). Apague essa
classe, o exemplo da tabela, a dependência `protoncloud-sdk` e o repositório `protoncloud`
do `pom.xml`, e o resto continua igual. A automação é sua, com ou sem a ferramenta.

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

As evidências (prints e respostas de API) ficam na pasta `evidencias/`; numa execução do
Proton, na pasta da execução, `evidencias/<id da execução>`.

## Estrutura

```
src/main/java/.../
  componentes/   uma classe por componente, de qualquer plataforma
  web/           sessão do navegador e páginas da loja (page objects)
  api/           sessão HTTP e clientes das APIs
  mobile/        sessão do aparelho (Appium) e telas do app
  apoio/         configuração, sessões, evidências, falhas e a execução dos cenários
src/test/java/.../
  CenariosTest.java       roda os cenários, sem o Proton
  TestProtonScript.java   o ponto de entrada do Proton (o único que importa a SDK)
cenarios/                 os cenários em JSON, no mesmo formato de um dataset do Proton
  tabelas/                as tabelas de dados dos cenários, em .csv
```

Quem escreve a automação mexe em `componentes/`, nos pacotes das plataformas e em
`cenarios/`. O pacote `apoio/` raramente muda.

A automação fica em `src/main`, e não em `src/test`: ela é o produto, e não um teste.
Assim ela vira um jar comum, que roda de um agendador ou de outro projeto, e o JUnit fica
só em `src/test`.

## Sessões

Cada plataforma tem uma sessão: o navegador, o cliente HTTP, o aparelho. A sessão abre na primeira vez
que um passo pede e serve aos passos seguintes do mesmo cenário, mesmo quando eles são de
outra plataforma. No fim do cenário, todas fecham, na ordem inversa, mesmo com erro (no
Proton, quem as fecha é a SDK, antes de gravar o resultado). Um cenário só de API nem abre
navegador.

Os componentes não lidam com sessão: usam as páginas e os clientes, que pedem a sessão da
plataforma deles (`SessaoWeb.pagina()`, `SessaoApi.get(...)`, `SessaoMobile.driver()`).
Quando um passo falha, cada sessão aberta grava a evidência dela: o print da página, a
última resposta da API e o print da tela do aparelho.

Para uma plataforma nova, crie o pacote dela com uma classe que implemente `Sessao`
(`evidencia(nome)` e `close()`), e peça a sessão com
`Sessoes.obter("nome", Classe.class, Classe::new)`. Use `web`, `api` e `mobile` como exemplo.

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
- para falhar, lance uma exceção (veja [Falhas](#falhas)). O print da tela da falha é
  automático;
- seletores, ações e chamadas ficam nas páginas e nos clientes, não no componente;
- nada em `src/main` importa o Proton, a não ser o componente que usa a
  [tabela de dados](#tabela-de-dados).

## Cenários

Cada arquivo da pasta `cenarios/` é um cenário: os passos em ordem, com o componente e os
parâmetros, como um dataset do Proton.

```json
{
  "descricao": "Compra com sucesso",
  "passos": [
    {"componente": "ConsultarCep", "parametros": {"in_cep": "01310-100"}},
    {"componente": "FazerLogin", "parametros": {"in_usuario": "standard_user", "in_senha": "${env:SENHA_DA_LOJA}"}},
    {"componente": "AdicionarAoCarrinho", "parametros": {"in_produto": "Sauce Labs Backpack"}},
    {"componente": "FinalizarCompra", "parametros": {"in_nome": "Ana", "in_sobrenome": "Souza", "in_cep": "${out_cep}", "in_preco_esperado": "${out_preco}"}}
  ]
}
```

- `${out_cep}` usa a saída de um passo anterior, como a referência a saída no Proton.
- `${env:SENHA_DA_LOJA}` lê o valor de `-D`, do ambiente ou do `.env`. Senha não vai para o
  git; no Proton, o equivalente é o parâmetro criptografado.
- `"requer": ["android"]` pula o cenário quando não há aparelho Android conectado.
- `"tabela"` aponta a tabela de dados do cenário, um .csv da pasta `cenarios/tabelas/` (veja
  [Tabela de dados](#tabela-de-dados)).
- `"falha_esperada"` marca o cenário que falha de propósito, com um trecho da mensagem do erro.

O `compra-com-falha-proposital` falha de propósito: o usuário `problem_user` da loja tem um
defeito conhecido no checkout (o sobrenome vai para o campo do nome) e a compra para em
"Error: Last Name is required". Fora do Proton o teste passa quando o cenário falha com essa
mensagem, e a suíte segue verde. No Proton, o mesmo dataset termina em **Failed**, com o
motivo de negócio, o log, o print da tela e o erro do passo: serve para conhecer uma
execução que falhou e o diagnóstico da falha.

## Falhas

O passo falha quando o componente lança uma exceção. O Proton distingue três motivos de
falha: o status continua **Failed**, e o motivo vai junto, para os painéis separarem o
defeito da automação da recusa do sistema e do ambiente fora do ar.

| Motivo | Quando | Exceção |
|---|---|---|
| Negócio | a regra do sistema recusou, e repetir dá o mesmo resultado | `FalhaDeNegocio` |
| Ambiente | o ambiente não deixou rodar: o navegador ou o aparelho que não abre, a API que não responde quando a sessão abre | `FalhaDeAmbiente`, com o erro original como causa |
| Técnica | qualquer outro erro: o elemento que não apareceu, o valor diferente do esperado | as outras (`AssertionError` para validação) |

`FalhaDeNegocio` e `FalhaDeAmbiente` ficam em `apoio/` e não dependem do Proton: fora dele,
são falhas como as outras. Numa execução do Proton, o `TestProtonScript` as entrega à SDK
com o motivo. No exemplo, a recusa do checkout da loja (`PaginaDeCheckout`) é falha de
negócio, e as sessões que não abrem (`SessaoWeb`, `SessaoMobile`) e a API que não responde
ou segue indisponível depois das tentativas (`SessaoApi`) são falha de ambiente.

## Tabela de dados

No Proton, o dataset pode ter uma **tabela de dados** com abas, editável como planilha na
tela do dataset (colar do Excel, importar e exportar .xlsx e .csv). Cada execução recebe uma
cópia: o script lê e grava só nela, e o dataset muda apenas quando alguém leva os valores
de volta pela tela da execução. Vem da Proton Cloud SDK 5.7.0, na classe `ProtonTable`.

O exemplo é o cenário `compra-em-massa-pela-tabela`: o `AdicionarProdutosDaTabela` percorre
a aba `produtos`, põe cada produto no carrinho e grava na mesma linha o status, o preço e a
mensagem. Cria a coluna `processado_em`, inclui uma linha de total e devolve o total dos
itens, que o `FinalizarCompra` confere no checkout. A Caneca do Proton não existe na loja:
a linha dela fica com erro, e as outras seguem.

```java
ProtonTable produtos = ProtonTable.get("produtos");   // sem nome: a primeira aba
produtos.addColumn("processado_em");                  // coluna nova, só nesta execução

for (ProtonRow linha : produtos) {                    // valor é sempre texto
    try {
        String preco = new PaginaDeProdutos().adicionarAoCarrinho(linha.get("produto"));
        produtos.set(linha, "status", "no carrinho");   // linha a linha: grava na hora
        produtos.set(linha, "preco", preco);
    } catch (ProtonTableException e) {
        throw e;                                      // coluna errada, execução terminada: para
    } catch (RuntimeException | AssertionError e) {
        produtos.set(linha, "status", "erro");
        produtos.set(linha, "mensagem", e.getMessage());
    }
}

produtos.addRows(List.of(Map.of("produto", "Total", "preco", "47.97")));   // em lote: uma chamada só
```

| Método | O que faz |
|---|---|
| `ProtonTable.get(nome)` / `ProtonTable.getAll()` | Uma aba (sem nome, a primeira) / todas, por nome. |
| `aba.set(linha, coluna, valor)` | Grava uma célula na hora; `linha` é a `ProtonRow` ou o id dela. |
| `aba.addRow(Map)` / `aba.addRows(List)` | Inclui linhas no fim, numa chamada. |
| `aba.addColumn(nome)` / `aba.addColumns(List)` | Cria colunas no fim, só nesta execução. |
| `aba.getColumn(nome)`, `aba.toJson()` | Os valores de uma coluna; a aba inteira em JSON. |
| `aba.download(ProtonFormat.XLSX)` | Baixa a aba como está na execução (só no Proton). |
| `ProtonTable.create(nome, colunas)` | Cria uma aba só nesta execução. |

Escrita que falha lança `ProtonTableException`, com o motivo em `getReason()`
(`columnNotFound`, `runAlreadyFinished`...). Limites: 20 abas, 100 colunas, 10 mil linhas
por aba.

**Fora do Proton**, a tabela vem de um .csv, e as escritas ficam só na memória. O cenário
aponta o dele (`"tabela": "tabelas/produtos.csv"`; a aba leva o nome do arquivo), e
`PROTON_TABLE_FILE` (ou `-Dproton.table_file`) vale mais, para testar com outro arquivo,
como um .csv exportado pela tela do dataset:

```bash
mvn test -Dcenario=compra-em-massa
mvn test -Dcenario=compra-em-massa -Dproton.table_file=/caminho/produtos.csv
```

Para conferir o que foi gravado, a aba inteira vai em JSON para a pasta de evidências.

No Proton, crie no dataset a aba `produtos` com as colunas `produto`, `status`, `preco` e
`mensagem` (ou importe `cenarios/tabelas/produtos.csv`) e os passos `FazerLogin`,
`AdicionarProdutosDaTabela` e `FinalizarCompra`, com `in_preco_esperado` na saída
`out_total_dos_itens`. O resultado aparece na tabela da execução, na tela dela.

## Mobile

O exemplo mobile usa o [My Demo App](https://github.com/saucelabs/my-demo-app-android), o app de
demonstração da Sauce Labs, com os mesmos produtos da loja web. Os cenários mobile pedem
`"requer": ["android"]` e são pulados quando não há aparelho conectado.

Pré-requisitos na máquina:

- Android SDK, com o `adb` no `PATH`;
- [Appium](https://appium.io) com o driver UiAutomator2: `npm install -g appium` e
  `appium driver install uiautomator2`;
- um aparelho Android com a depuração USB ligada (ou um emulador), visível no `adb devices`.

Para rodar o exemplo, baixe o APK da [página de releases do My Demo
App](https://github.com/saucelabs/my-demo-app-android/releases) e instale no aparelho
(`adb install mda-*.apk`), ou aponte `MOBILE_APP` para o arquivo no `.env`. Depois:

```bash
mvn test -Dcenario=app
```

Se o Appium não estiver no ar no endereço de `APPIUM_URL`, a automação o inicia e o encerra
no fim do cenário. `MOBILE_DISPOSITIVO` escolhe o aparelho quando há mais de um conectado.

No Proton:

1. Cadastre o app como um sistema de plataforma **Mobile** e o APK em **Aplicativos Mobile**,
   ligado a esse sistema. A automação não tem mais a marcação mobile: o que ela pede no
   disparo sai dos sistemas dos componentes.
2. Ao disparar, escolha o aparelho (os que o runner encontra) e o app de cada sistema mobile,
   ou deixe o aparelho padrão do sistema.
3. Antes de cada passo, o ponto de entrada do Proton pede à SDK o aparelho e o app do sistema
   do passo (`ProtonAutomation.getCurrentComponentDevice()`), baixa o app e os entrega à sessão mobile. O código do componente não
   muda.

Uma execução pode ter mais de um sistema mobile (o app do cliente e o app do entregador, por
exemplo), cada um com o seu aparelho. A sessão mobile é uma por sistema (`mobile:<id do
sistema>`): o passo de um app usa o aparelho dele, e o passo seguinte do outro app abre o
outro aparelho. Cada aparelho local tem o seu `systemPort` no UiAutomator2, para os dois não
disputarem a mesma porta. Com um sistema mobile só, nada muda.

## Rodar pelo Proton

1. **Sistemas.** Cadastre um sistema para cada plataforma (por exemplo, o portal, a API e o app)
   e ligue todos a este repositório. O runner reconhece o projeto Java pelo `pom.xml` na
   raiz.
2. **Componentes.** Um componente no Proton para cada classe de `componentes`, no sistema
   da plataforma dele, com o mesmo nome e os parâmetros `in_` e `out_`.
3. **Automação e dataset.** Os passos do dataset são os componentes, em ordem, com os
   valores dos parâmetros. A saída de um passo vira entrada de outro pela referência a
   saída.
4. **Execução.** O runner roda `mvn test -DidDatasetRun=<id> -Dtest=TestProtonScript` e
   entrega no ambiente o endereço do Proton e um token que vale só para aquela
   execução. O projeto não guarda token nem endereço do Proton.

A Proton Cloud SDK roda os passos em ordem, no mesmo processo, enquanto este projeto tiver
o componente: as sessões abertas valem para a execução inteira, de uma plataforma para a
outra. A cada passo, marca início e fim, passa ao componente só os parâmetros dele, grava
as saídas e sobe as evidências que apareceram na pasta da execução. No fim, fecha as
sessões abertas, sobe o que apareceu no fechamento e grava o resultado; quando um passo
falha, grava também o erro com o stack trace e o [motivo](#falhas). Um passo cujo
componente fica em outro repositório (o SAP no projeto Python, por exemplo) volta para o
runner, que chama o projeto daquele sistema; antes disso, a SDK também fecha as sessões.

O runner pode rodar várias execuções ao mesmo tempo, na mesma pasta do projeto. Cada uma
grava as evidências na pasta dela, `evidencias/<id da execução>`, que a SDK dá
(`ProtonComponentRunner.runEvidenceDir()`), e não sobe o que as outras gravam.

A máquina do runner precisa de Java e de Maven. A SDK vem do repositório Maven público da
Atomic (`https://maven.protoncloud.com.br`), sem credencial.

Para rodar o ponto de entrada do Proton na sua máquina, contra uma execução de verdade:
`mvn test -DidDatasetRun=<id> -Dtest=TestProtonScript`, com `PROTON_HOST` e `PROTON_TOKEN`
no ambiente.

## Com IA

O `AGENTS.md` traz as regras do projeto para agentes de IA (Claude Code, Codex, Cursor e
outros). Com o conector MCP do Proton, o agente cadastra no Proton os componentes que
criar aqui, e o caminho é este:

1. `get_project_template`: de onde vem este modelo e as regras do projeto, para começar um
   projeto novo.
2. `generate_component_code`: o arquivo de um componente já cadastrado no Proton, no
   formato daqui, com os parâmetros `in_` e `out_`; falta escrever a lógica.
3. `check_project`, depois de cada push: o repositório confere com o cadastro (um arquivo
   por componente, os parâmetros, a versão mínima da SDK, nenhum segredo no repositório)?
4. `check_automation`, antes da primeira execução: os datasets estão prontos para rodar?

## Licença

[MIT](LICENSE).
