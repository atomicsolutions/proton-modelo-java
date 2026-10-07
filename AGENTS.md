# Regras do projeto para agentes de IA

Projeto de automação em Java 21 com Playwright, Maven e JUnit 6. Roda pelo JUnit, como
robô (`mvn compile exec:java`) ou pelo Proton. Responda e escreva em português do Brasil.

## Onde fica cada coisa

- `src/main/java/.../componentes/`: uma classe por componente, que implementa `Componente`.
- `src/main/java/.../paginas/`: page objects. Seletores e ações de tela ficam só aqui.
- `src/main/java/.../apoio/`: configuração, navegador, prints e execução dos cenários. Mude só se a tarefa pedir.
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
- Log com SLF4J (`LoggerFactory.getLogger(...)`), sem `System.out`.
- Prints de tela com `Evidencias.printDaTela("nome")`. O print da falha é automático.

## Ao criar ou mudar um componente

1. Escreva as ações de tela na página (`paginas/`), reaproveitando as que existem.
2. Escreva o componente em `componentes/`.
3. Use-o num cenário de `cenarios/` e rode `mvn test -Dcenario=<cenario>` até passar.
4. Se houver conector MCP do Proton, cadastre ou atualize o componente no sistema ligado a
   este repositório, com o mesmo nome e os mesmos parâmetros, e documente objetivo e
   resultado esperado.

## Segredos

Nunca escreva senha, token ou chave em código, cenário ou commit. Nos cenários, use
`${env:NOME}` e documente a variável no `.env.example`. No Proton, use parâmetro
criptografado.

## Comandos

- `mvn test`: roda todos os cenários.
- `mvn test -Dcenario=compra`: roda os cenários com "compra" no nome do arquivo.
- `mvn compile exec:java -Dexec.args="cenarios/<arquivo>.json"`: roda um cenário sem JUnit.
