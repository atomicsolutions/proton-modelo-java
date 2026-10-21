# Regras do projeto para agentes de IA

Projeto de automação em Java 21 com Maven e JUnit 6, com mais de uma plataforma no mesmo
cenário: web (Playwright), API (`java.net.http` e Gson) e mobile Android (Appium). Roda pelo JUnit, como robô
(`mvn compile exec:java`) ou pelo Proton. SAP GUI e desktop Windows ficam no projeto modelo
Python. Responda e escreva em português do Brasil.

## Onde fica cada coisa

- `src/main/java/.../componentes/`: uma classe por componente, de qualquer plataforma, que implementa `Componente`.
- `src/main/java/.../web/`, `api/` e `mobile/`: um pacote por plataforma, com a sessão
  (`SessaoWeb`, `SessaoApi`, `SessaoMobile`) e as páginas, clientes e telas. Seletores, ids
  de tela e endereços de API ficam só aqui.
- `src/main/java/.../apoio/`: configuração, sessões, evidências e execução dos cenários. Mude só se a tarefa pedir.
- `src/test/java/.../CenariosTest.java`: roda os cenários da pasta `cenarios/`.
- `src/test/java/.../TestProtonScript.java`: ponto de entrada do Proton, o único que importa
  a SDK. Não mude o nome da classe: o runner procura `TestProtonScript`.
- `cenarios/`: cenários em JSON, no formato de um dataset do Proton. O que tem
  `falha_esperada` falha de propósito (o `compra-com-falha-proposital`): não o conserte.

## Contrato do componente

- O nome do componente é o mesmo no Proton, nos cenários e na classe, em PascalCase
  (`FinalizarCompra`).
- A classe implementa `Componente` e tem construtor público sem parâmetros.
- Parâmetros de entrada com prefixo `in_`, saídas com `out_`, em snake_case e minúsculas.
- Todo valor de parâmetro e de saída é texto (`String`).
- Para falhar, lance uma exceção com uma mensagem que diga o que se esperava e o que veio.
  Nunca só registre o erro no log e siga: o passo precisa falhar. A exceção diz o motivo da
  falha, que o Proton grava junto com o Failed:
  - `FalhaDeNegocio` (de `apoio/`): a regra do sistema recusou, e repetir dá o mesmo
    resultado (o cadastro que já existe, os dados que a loja não aceita);
  - `FalhaDeAmbiente` (de `apoio/`): o ambiente não deixou rodar (a sessão que não abre, o
    aparelho fora do ar), sempre com o erro original como causa;
  - qualquer outra (`AssertionError` para validação): falha técnica.
- Nada em `src/main` importa `br.com.atomicsolutions` (a SDK do Proton); em `src/test`, só o
  `TestProtonScript`, que traduz `FalhaDeNegocio` e `FalhaDeAmbiente` para a SDK.
- Dados passam de um passo para outro só pelas saídas e entradas, nunca por campo estático.
- O componente não abre nem fecha sessão: usa as páginas e os clientes, que pedem a sessão
  da plataforma deles. As sessões abrem na primeira vez e fecham no fim do cenário (no
  Proton, a SDK as fecha antes de gravar o resultado).
- Log com SLF4J (`LoggerFactory.getLogger(...)`), sem `System.out`.
- Evidência com `SessaoWeb.printDaTela("nome")`, `SessaoMobile.printDaTela("nome")` ou
  `SessaoApi.evidenciaDaUltimaResposta("nome")`, e arquivo novo com
  `Evidencias.arquivo(nome, extensao)`, nunca num caminho fixo: no Proton, a pasta é a da
  execução (`evidencias/<id da execução>`). A evidência da falha é automática.

## Plataforma nova

Crie o pacote da plataforma com uma classe que implemente `Sessao` (`evidencia(nome)` e
`close()`) e métodos estáticos que peçam a sessão com
`Sessoes.obter("<plataforma>", Classe.class, Classe::new)`, como `SessaoWeb`, `SessaoApi` e
`SessaoMobile`. Se o cenário precisar de algo da máquina, declare em `"requer"`.

## Ao criar ou mudar um componente

1. Escreva as ações na página ou no cliente da plataforma, reaproveitando as que existem.
2. Escreva o componente em `componentes/`.
3. Use-o num cenário de `cenarios/` e rode `mvn test -Dcenario=<cenario>` até passar.
4. Se houver conector MCP do Proton, cadastre ou atualize o componente no sistema da
   plataforma dele (ligado a este repositório), com o mesmo nome e os mesmos parâmetros,
   e documente objetivo e resultado esperado.

## Com o conector MCP do Proton

- `get_project_template` (`language: java`): de onde vem este modelo (o repositório para
  clonar) e as regras do projeto. Comece por ele num projeto novo.
- `generate_component_code`: o arquivo de um componente já cadastrado no Proton, no formato
  daqui (`componentes/<Nome>.java`), com os parâmetros `in_` e `out_`. Escreva a lógica no
  lugar do TODO: até lá, o passo falha de propósito.
- `check_project`, depois de cada push: confere o repositório contra o cadastro (um arquivo
  por componente, os parâmetros, a versão mínima da SDK, segredo no repositório). Corrija
  todo erro.
- `check_automation`, antes da primeira execução: confere se os datasets estão prontos
  (parâmetros com valor, referências, runner no ar).

## Segredos

Nunca escreva senha, token ou chave em código, cenário ou commit. Nos cenários, use
`${env:NOME}` e documente a variável no `.env.example`. No Proton, use parâmetro
criptografado.

## Comandos

- `mvn test`: roda todos os cenários.
- `mvn test -Dcenario=compra`: roda os cenários com "compra" no nome do arquivo.
- `mvn compile exec:java -Dexec.args="cenarios/<arquivo>.json"`: roda um cenário sem JUnit.
