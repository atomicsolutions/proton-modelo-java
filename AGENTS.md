# Regras do projeto para agentes de IA

Projeto de automação em Java 21 com Maven e JUnit 6, com mais de uma plataforma no mesmo
cenário: web (Playwright) e API (`java.net.http` e Gson). Roda pelo JUnit, como robô
(`mvn compile exec:java`) ou pelo Proton. SAP GUI e desktop Windows ficam no projeto modelo
Python. Responda e escreva em português do Brasil.

## Onde fica cada coisa

- `src/main/java/.../componentes/`: uma classe por componente, de qualquer plataforma, que implementa `Componente`.
- `src/main/java/.../web/` e `api/`: um pacote por plataforma, com a sessão (`SessaoWeb`,
  `SessaoApi`) e as páginas e clientes. Seletores e endereços de API ficam só aqui.
- `src/main/java/.../apoio/`: configuração, sessões, evidências e execução dos cenários. Mude só se a tarefa pedir.
- `src/test/java/.../CenariosTest.java`: roda os cenários da pasta `cenarios/`.
- `src/proton/java/.../TestProtonScript.java`: ponto de entrada do Proton. Não mude o nome
  da classe: o runner procura `TestProtonScript`. Só entra no build com o perfil `proton`
  (`-DidDatasetRun`).
- `cenarios/`: cenários em JSON, no formato de um dataset do Proton.

## Contrato do componente

- O nome do componente é o mesmo no Proton, nos cenários e na classe, em PascalCase
  (`FinalizarCompra`).
- A classe implementa `Componente` e tem construtor público sem parâmetros.
- Parâmetros de entrada com prefixo `in_`, saídas com `out_`, em snake_case e minúsculas.
- Todo valor de parâmetro e de saída é texto (`String`).
- Para falhar, lance uma exceção (`AssertionError` para validação) com uma mensagem que
  diga o que se esperava e o que veio. Nunca só registre o erro no log e siga: o passo
  precisa falhar.
- Nada em `src/main` nem em `src/test` importa `br.com.atomicsolutions` (a SDK do Proton).
- Dados passam de um passo para outro só pelas saídas e entradas, nunca por campo estático.
- O componente não abre nem fecha sessão: usa as páginas e os clientes, que pedem a sessão
  da plataforma deles. As sessões abrem na primeira vez e fecham no fim do cenário.
- Log com SLF4J (`LoggerFactory.getLogger(...)`), sem `System.out`.
- Evidência com `SessaoWeb.printDaTela("nome")` ou `SessaoApi.evidenciaDaUltimaResposta("nome")`.
  A evidência da falha é automática.

## Plataforma nova

Crie o pacote da plataforma com uma classe que implemente `Sessao` (`evidencia(nome)` e
`close()`) e métodos estáticos que peçam a sessão com
`Sessoes.obter("<plataforma>", Classe.class, Classe::new)`, como `SessaoWeb` e `SessaoApi`.

## Ao criar ou mudar um componente

1. Escreva as ações na página ou no cliente da plataforma, reaproveitando as que existem.
2. Escreva o componente em `componentes/`.
3. Use-o num cenário de `cenarios/` e rode `mvn test -Dcenario=<cenario>` até passar.
4. Se houver conector MCP do Proton, cadastre ou atualize o componente no sistema da
   plataforma dele (ligado a este repositório), com o mesmo nome e os mesmos parâmetros,
   e documente objetivo e resultado esperado.

## Segredos

Nunca escreva senha, token ou chave em código, cenário ou commit. Nos cenários, use
`${env:NOME}` e documente a variável no `.env.example`. No Proton, use parâmetro
criptografado.

## Comandos

- `mvn test`: roda todos os cenários.
- `mvn test -Dcenario=compra`: roda os cenários com "compra" no nome do arquivo.
- `mvn compile exec:java -Dexec.args="cenarios/<arquivo>.json"`: roda um cenário sem JUnit.
