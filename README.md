# Assignment TP3 — Annotation Processor and Regex Annotation Processor

Course: Desenvolvimento de Aplicações Móveis <br>
Student: Carolina Ribeiro Raposo (n.º 51568) <br>
Date: 02/05/2026 <br>
Repository URL: https://github.com/carolRR5/GreetingProcessorProjecct

---

## 1. Introduction

### Purpose

Este trabalho tem como objetivo aprender e implementar annotation processors em Kotlin, uma técnica avançada 
para gerar código automaticamente em tempo de compilação.

### Problem Description

- Exercise 1: Criar um annotation processor que gera classes wrapper que imprimem mensagens antes de chamar métodos originais;
- Exercise 2: Criar um annotation processor que gera implementações de métodos abstratos usando expressões regulares 
para extrair dados de strings.

### Objectives

- Compreender como funcionam annotation processors em Kotlin;
-  Implementar dois processadores diferentes (GreetingProcessor e RegexProcessor);
-  Usar KotlinPoet para gerar código Kotlin programaticamente;
-  Organizar um projeto multi-módulo Gradle;
-  Testar e validar o código gerado automaticamente.

## 2. System Overview

### Exercise 1: GreetingProcessor

Fluxo:
```
@Greeting("Hello!") 
fun sayHello() { ... }
        ↓
    GreetingProcessor (durante compilação)
        ↓
MyClassWrapper.kt (gerado automaticamente)
fun sayHello() { 
    println("Hello!") 
    original.sayHello() 
}
```
O primeiro exercício implementa um padrão relativamente simples mas fundamental: o padrão Wrapper (também 
conhecido como Decorator). A ideia central é que quando um programador marca um método com a anotação __`@Greeting("mensagem")`__, 
o annotation processor gera automaticamente uma classe wrapper que encapsula a classe original.

Quando o código é compilado, o processador analisa todos os métodos anotados com __`@Greeting`__ e cria uma 
nova classe (por exemplo, `MyClassWrapper`) que contém uma propriedade `original` referenciando a instância da 
classe original. Para cada método anotado, o wrapper gera um método correspondente que primeiro imprime a mensagem 
especificada na anotação e depois chama o método original através da propriedade `original`.

Este padrão oferece várias vantagens: em primeiro lugar, permite adicionar comportamento sem modificar o código 
original (princípio Open-Closed). Em segundo lugar, usa composição em vez de herança, o que é geralmente preferível 
em design orientado a objetos. Finalmente, demonstra de forma clara e prática como o annotation processor funciona 
passo a passo.

### Exercise 2: RegexProcessor

Fluxo:
```
abstract class DataProcessor(input: String) {
    @Extract(regex = "Name : (\\w+)")
    abstract fun getName(): String?
}
        ↓
    RegexProcessor (durante compilação)
        ↓
DataProcessorExtractor.kt (gerado automaticamente)
override fun getName(): String? {
    val match = Regex("Name : (\\w+)").find(input)
    return match?.groupValues?.get(1)
}
```

O segundo exercício é mais avançado e demonstra a geração de código com lógica real. Neste caso, o objetivo é 
permitir que programadores definam métodos abstratos anotados com __`@Extract(regex = "padrão")`__, e o annotation processor 
gera automaticamente as implementações desses métodos.

A implementação gerada utiliza expressões regulares (regex) para procurar padrões na string de entrada e extrair dados
específicos. Para cada método abstrato anotado, o processor cria uma implementação que: (1) cria um objeto `Regex` com 
o padrão especificado; (2) procura o padrão na string de entrada; (3) extrai e retorna o primeiro grupo capturado (a 
parte entre parênteses na regex), ou retorna `null` se nenhuma correspondência for encontrada.

Este exercício é mais complexo porque envolve herança (a classe gerada herda da classe abstrata), passagem de parâmetros 
para superclasse, e manipulação de tipos genéricos. Também introduz o conceito de expressões regulares, que é uma ferramenta
poderosa para processamento de strings.

## 3. Architecture and Design

### 3.1 Project Structure

```
GreetingProcessorProject/
├── annotations/                    # Módulo com as anotações
│   └── src/main/kotlin/annotations/
│       ├── Greeting.kt            # Exercise 1
│       └── Extract.kt             # Exercise 2
├── processor/                      # Módulo com os processadores
│   └── src/main/kotlin/processor/
│       ├── GreetingProcessor.kt   # Exercise 1
│       └── RegexProcessor.kt      # Exercise 2
├── app/                           # Módulo com testes
│   └── src/main/kotlin/com/example/app/
│       ├── MyClass.kt             # Exercise 1 - classe original
│       ├── DataProcessor.kt        # Exercise 2 - classe abstrata
│       └── Main.kt                # Testes de ambos
└── settings.gradle.kts            # Configuração dos 3 módulos
```

- Módulo __annotations__: Este módulo contém as definições das anotações (`@Greeting` e `@Extract`). Separar as anotações 
num módulo dedicado permite que elas sejam reutilizadas em qualquer lugar do projeto sem criar dependências 
circulares. Anotações são simples ficheiros Kotlin que apenas definem a interface da anotação, sem incluir lógica complexa.

- Módulo __processor__: Este módulo contém os annotation processors (`GreetingProcessor` e `RegexProcessor`). O processor depende
do módulo `annotations` para conhecer as anotações que processa, e também depende de bibliotecas externas como 
KotlinPoet. Isolando o processor num módulo separado evita carregar código de compilação (que é pesado) nas
aplicações que usam as anotações.

- Módulo __app__: Este módulo contém o código de teste e demonstração. Ele depende do módulo `annotations` (para usar as 
anotações) e do módulo `processor` como um `kapt` (Kotlin Annotation Processor Tool), que instrui o Gradle a executar o 
processor durante a compilação. Este módulo é onde vemos o resultado final: as classes originais, as anotações, e 
as classes geradas pelo processor.

### 3.2 Design Patterns

__Padrão Wrapper/Decorator (Exercise 1)__:
O padrão Wrapper é utilizado no primeiro exercício para adicionar comportamento (imprimir mensagem) sem modificar a classe original.
A classe wrapper encapsula a classe original e delega chamadas de método após executar lógica adicional. Este padrão é fundamental 
em muitos frameworks e oferece flexibilidade ao permitir "decorar" qualquer classe com comportamento adicional.

A decisão de usar composição em vez de herança foi deliberada. Embora fosse possível gerar classes que herdam de `MyClass`,
a composição oferece mais flexibilidade (a classe wrapper pode funcionar com qualquer instância de `MyClass`, não apenas com 
subclasses específicas) e segue melhor o princípio SOLID de composição sobre herança.

__Padrão Template Method (Exercise 2)__:
O segundo exercício implementa uma variação do padrão Template Method, onde a classe abstrata define o contrato (quais métodos 
devem existir e que retornam `String?`), e o annotation processor fornece a implementação concreta. Esta abordagem permite que
programadores definam apenas a estrutura dos seus dados (via anotações) e deixem o processor implementar a lógica 
de extração. É um exemplo de inversão de controlo: em vez de o programador escrever o código de extração, o processor gera-o
automaticamente baseado em declarações.

### 3.3 Important Architectural Decisions

1. __Uso de KotlinPoet para Geração de Código__: Em vez de gerar código manipulando strings manualmente (o que seria propenso a erros), utilizamos a 
biblioteca KotlinPoet. Esta biblioteca fornece uma API type-safe para construir código Kotlin programaticamente. As vantagens 
são enormes: código gerado é sempre sintaticamente correto, evitamos erro de indentação, e é muito mais legível do que 
strings concatenadas.
2. __Separação de Compilação (KAPT)__: Utilizamos o Gradle plugin KAPT (Kotlin Annotation Processor Tool) para
executar os processors durante a compilação. Isto garante que o código é gerado antes de qualquer compilação de código 
que o use, evitando problemas de dependências circulares e garantindo sincronização.
3. __Propriedade input em DataProcessor__: No Exercise 2, a classe abstrata `DataProcessor` define uma propriedade `input` como
`val`. A classe gerada `DataProcessorExtractor` herda desta propriedade sem tentar fazer override dela (porque em Kotlin, 
propriedades não podem ser sobrescritas se forem finais). Em vez disso, o construtor da classe gerada recebe `input` 
como parâmetro e passa-o para o construtor da superclasse.

## 4. Implementation

### 4.1 Exercise 1: Greeting Processor

A anotação `@Greeting` é definida da seguinte maneira:

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Greeting(val message: String)
```

A anotação tem dois metadados importantes, sendo que `@Target` especifica onde a anotação pode ser aplicada (apenas em 
funções/ métodos neste caso), e `@Retention` especifica quando a anotação existe (SOURCE significa que existe apenas no 
código fonte e é descartada após compilação). O parâmetro `message` permite que cada anotação customize a mensagem que será impressa.

#### Implementation of GreetingProcessor

O `GreetingProcessor` o componente central onde ocorre a geração automática de código. Implementa a interface 
AbstractProcessor, que faz parte da Java Annotation Processing API, e sobrescreve o método process(), que é invocado 
automaticamente pelo Gradle KAPT (Kotlin Annotation Processing Tool) durante o ciclo de compilação.

Relativamente ao fluxo de funcionamento:

1. __Descoberta de métodos anotados__: O processor itera sobre todos os elementos encontrados com 
`roundEnv.getElementsAnnotatedWith(Greeting::class.java)`. Para cada elemento que é um `ExecutableElement` (método), 
obtém a classe que o contém.

2. __Organização por classe__: Os métodos são organizados num mapa onde a chave é a classe e o valor é a lista de métodos 
anotados nessa classe. Isto permite processar todos os métodos de uma classe juntos.

3. __Geração de código__: Para cada classe, o processor chama `generateKotlinWrapperClass` que utiliza KotlinPoet para 
construir a classe wrapper. Esta função:
    - Cria um construtor que aceita a instância original;
    - Define uma propriedade que guarda a referência original;
    - Para cada método anotado, gera um método wrapper que imprime a mensagem e chama o original;
    - Escreve o código gerado para disco no diretório apropriado.

#### Example of the Generated Code

O código gerado para `MyClass` com dois métodos anotados fica assim:

``` kotlin
public final class MyClassWrapper(
    public val original: MyClass,
) {
    public final fun sayHello() {
        println("Hello from MyClass!")
        original.sayHello()
    }

    public final fun compute() {
        println("Welcome to the compute function!")
        original.compute()
    }
}
```

### 4.2 Exercise 2: RegexProcessor

A anotação `@Greeting` é semelhante a `@Greeting`, mas em vez de uma mensagem, recebe um padrão de expressão regular:

``` kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Extract(val regex: String)
```

Este design permite máxima flexibilidade, sendo que cada método pode ter a sua própria regex para extrair dados específicos.

#### Implementation of RegexProcessor

O `RegexProcessor` é semelhante ao `GreetingProcessor` em estrutura, mas com uma diferença importante, uma vez que, em vez de 
gerar classes wrapper, gera classes que herdam da classe abstrata e implementam os métodos abstratos.

O seu fluxo também é semelhante, sendo que:

1. __Descoberta de métodos anotados__: Encontra todos os métodos anotados com `@Extract`;
2. __Organização por classe__: Agrupa por classe, sendo que pode haver múltiplas classes abstratas;
3. __Geração de código__: Para cada classe abstrata, gera uma classe concreta que a implementa;
4. __Implementação de métodos__: Para cada métodos abstrato, gera a implementação que:
    - Cria um `Regex` com o padrão de anotação;
    - Chama `find(input)` para procurar o padrão;
    - Extrai o primeiro grupo capturado (`groupValues[1]`);
    - Retorna o resultado ou `null` se não encontrar.

A parte crítica é compreender como as expressões regulares funcionam, particularmente o conceito de "grupos de captura". 
Quando temos um padrão regex como `"Name : (\\w+)"`, os parênteses `()` definem uma secção especial chamada grupo de 
captura. Esta secção marca qual parte do texto queremos extrair.

Quando a regex encontra uma correspondência na string, armazena os resultados numa lista chamada `groupValues`. O 
índice `[0]` sempre contém o match completo (a parte inteira que a regex encontrou). Os índices subsequentes `[1]`, `[2]`, 
etc., contêm os grupos de captura em ordem.

Por exemplo, se aplicarmos o padrão `"Name : (\\w+)"` à string `"Name : John Address : 123 Street"`:
- `groupValues[0]` retorna `"Name : John"` (o match completo);
- `groupValues[1]` retorna `"John"` (o conteúdo do primeiro grupo de captura entre parênteses)

No código gerado, usamos sempre `groupValues[1]` porque queremos apenas a informação extraída (neste caso, o nome),
não a string completa. Isto é o que torna o padrão tão útil: permite que a regex encontre e valide a estrutura completa,
mas extrai apenas a parte relevante.

#### Example of the Generated Code

Para uma classe `DataProcessor` com métodos anotados, o processor gera:

```kotlin
public final class DataProcessorExtractor(
    input: String,
) : DataProcessor(input) {
    override fun getName(): String? {
        val match = Regex("Name : (\\w+)").find(input)
        return match?.groupValues?.get(1)
    }

    override fun getAddress(): String? {
        val match = Regex("Address : (.+)").find(input)
        return match?.groupValues?.get(1)
    }
}
```

## 5. Testing and Validation

A validação dos annotation processors foi feita através de testes simples, compilar o projeto foi compilado e executar 
o código gerado para verificar se funcionava corretamente.

Para o Exercise 1, foi criada uma classe MyClass com dois métodos anotados com `@Greeting`. Após compilação, o processor 
deverá gerar uma classe `MyClassWrapper`. Para testar, foi criada uma instância de `MyClas`, envolvendo-a com `MyClassWrapper`, 
e chamamos os métodos. Se as mensagens aparecessem antes da execução dos métodos originais, o teste passa.

Para o Exercise 2, criamos uma classe `DataProcessor` com métodos abstratos anotados. Após compilação, o processor deverá
gerar `DataProcessorExtractor`. Para testar, foi criada uma instância com uma string de entrada contendo dados estruturados,
e chamamos os métodos de extração. Se os valores corretos fossem retornados, o teste passa.

## 6. Usage Instructions

### 6.1 System Requirements

Para executar este projeto com sucesso, é necessário ter instalado:
- __Java Development Kit (JDK) 23 ou superior__: O projeto foi compilado para Java 23 (configurado em `@SupportedSourceVersion(SourceVersion.RELEASE_23`
nos processors);
- __Gradle 8.0 ou superior__: Sistema de build utilizado para compilar o projeto;
- __IntelliJ IDEA 2023.1 ou superior (recomendado)__: IDE com suporte completo para Kotlin e KAPT
- __Git__: Para clonar o repositório (opcional, pode fazer download do ficheiro)

Verificar a instalação:
```
java -version          # Deve mostrar Java 23+
gradle --version       # Deve mostrar Gradle 8.0+
```

### 6.2 Initial Setup

O processo de setup é simples e direto:

#### Passo 1: Clonar o Repositório

```
git clone [repository-url]
cd GreetingProcessorProject
```

#### Passo 2: Sincronizar Gradle 
O Gradle fará download de todas as dependências necessárias (KotlinPoet, AutoService, etc.):

```
gradlew.bat build
```

#### Passo 3: Sincronizar no IntelliJ 
Se usar IntelliJ IDEA, é importante sincronizar o IDE com a configuração Gradle:
1. File → Sync with Gradle (ou File → Sync Now);
2. Aguarde que o IntelliJ faça o index dos ficheiros;
3. Verifique que não há erros de compilação.

#### Passo 4: Ativar Annotation Processing
No IntelliJ, é recomendado verificar que o annotation processing está ativado:
1. File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors;
2. Marque "Enable annotation processing";
3. Clique OK

### 6.3 Executar o Projeto

Executar no IntelliJ:

1. Abrir Main.kt (em app/src/main/kotlin/Main.kt);
2. Clicar na seta verde ao lado da função main;
3. Selecionar "Run 'MainKt'";

Isto compila o projeto (incluindo executar os annotation processors para gerar código) e executa a função main() do 
ficheiro Main.kt. 

---
# Development Process

## 12. Version Control and Commit History

O projeto foi desenvolvido com commits frequentes que reflectem progresso incremental. Esta abordagem oferece várias
vantagens, uma vez que é mais fácil de seguir a evolução do projeto, é possível reverter para versões anteriores se algo correr mal, e 
o histórico serve como documentação de decisões.

## 13. Difficulties and Lessons Learned

__Desafio 1: Compreender a Reflection API do Java/Kotlin__ <br>
No início, não era claro como aceder a informações sobre métodos, classes, e anotações. Os nomes das classes 
(`ExecutableElement`, `TypeElement`, `RoundEnvironment`) eram confusos, e o padrão visitor utilizado pela API era pouco intuitivo.

__Desafio 2: Erros Criptográficos com KotlinPoet e Tipos__ <br>
O erro `'input' is final and cannot be overridden` foi especialmente confuso porque a mensagem não explica claramente 
a solução. Passei algum tempo tentando fazer override da propriedade antes de entender que propriedades finais em Kotlin
não podem ser sobrescritas.

__Desafio 3: Tipos de Retorno e Nullable em Kotlin__
Outro erro foi `Return type mismatch: expected 'java.lang.String', actual 'kotlin.String?'`. Não estava claro como
especificar tipos nullable (String?) em KotlinPoet. A solução foi usar `.copy(nullable = true)` no tipo de retorno.

## 14. Future Improvements

__Suporte a Parâmetros__: Os métodos wrapper poderiam passar parâmetros para o método original.

__Suporte a Tipos de Retorno__: Atualmente presume-se que os métodos retornam void. Seria útil suportar métodos com retorno.

__Suporte a Múltiplos Grupos de Capture__: Permitir especificar qual grupo extrair em vez de sempre usar groupValues[1].

__Conversão Automática de Tipos__: Permitir conversão automática para outros tipos (Int, LocalDate, etc.) em vez de sempre String.

__Validação de Regex em Compilação__: Validar que a regex é válida em tempo de compilação.

---
## 15. AI Usage Disclosure

Em conformidade com as regras do projeto, não foram utilizadas ferramentas de IA na produção de código, 
sendo que todo o desenvolvimento foi realizado de forma manual. A aplicação destas tecnologias restringiu-se 
apenas à revisão e melhoria da redação deste documento informativo. Como autor, assumo total responsabilidade
pela clareza e veracidade das informações aqui apresentadas.