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

O Proton é opcional. Só uma classe do projeto conhece o Proton: o `TestProtonScript`.
Apague essa classe, a dependência `protoncloud-sdk` e o repositório `protoncloud` do
`pom.xml`, e o resto continua igual. A automação é sua, com ou sem a ferramenta.

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

As evidências (prints e respostas de API) ficam na pasta `evidencias/`.

## Estrutura

```
src/main/java/.../
  componentes/   uma classe por componente, de qualquer plataforma
  web/           sessão do navegador e páginas da loja (page objects)
  api/           sessão HTTP e clientes das APIs
  mobile/        sessão do aparelho (Appium) e telas do app
  apoio/         configuração, sessões, evidências e a execução dos cenários
src/test/java/.../
  CenariosTest.java       roda os cenários, sem o Proton
  TestProtonScript.java   o ponto de entrada do Proton (o único que importa a SDK)
cenarios/                 os cenários em JSON, no mesmo formato de um dataset do Proton
```

Quem escreve a automação mexe em `componentes/`, nos pacotes das plataformas e em
`cenarios/`. O pacote `apoio/` raramente muda.

A automação fica em `src/main`, e não em `src/test`: ela é o produto, e não um teste.
Assim ela vira um jar comum, que roda de um agendador ou de outro projeto, e o JUnit fica
só em `src/test`.

## Sessões

Cada plataforma tem uma sessão: o navegador, o cliente HTTP, o aparelho. A sessão abre na primeira vez
que um passo pede e serve aos passos seguintes do mesmo cenário, mesmo quando eles são de
outra plataforma. No fim do cenário, todas fecham, na ordem inversa, mesmo com erro. Um
cenário só de API nem abre navegador.

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
- para falhar, lance uma exceção. O print da tela da falha é automático;
- seletores, ações e chamadas ficam nas páginas e nos clientes, não no componente;
- nada em `src/main` importa o Proton. O compilador garante: a SDK está no escopo `test`.

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

1. Marque a automação como mobile e cadastre o APK em **Aplicativos Mobile**.
2. Ao disparar, escolha o aparelho (os que o runner encontra) e o app.
3. O ponto de entrada do Proton lê da execução o aparelho e o app, baixa o app e o entrega à
   sessão mobile. O código do componente não muda.

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
as saídas e sobe as evidências que apareceram na pasta `evidencias/`. No fim, grava o
resultado; quando um passo falha, grava também o erro com o stack trace. Um passo cujo
componente fica em outro repositório (o SAP no projeto Python, por exemplo) volta para o
runner, que chama o projeto daquele sistema.

A máquina do runner precisa de Java e de Maven. A SDK vem do repositório Maven público da
Atomic (`https://maven.protoncloud.com.br`), sem credencial.

Para rodar o ponto de entrada do Proton na sua máquina, contra uma execução de verdade:
`mvn test -DidDatasetRun=<id> -Dtest=TestProtonScript`, com `PROTON_HOST` e `PROTON_TOKEN`
no ambiente.

## Com IA

O `AGENTS.md` traz as regras do projeto para agentes de IA (Claude Code, Codex, Cursor e
outros). Com o conector MCP do Proton, o agente cadastra no Proton os componentes que
criar aqui.

## Licença

[MIT](LICENSE).
