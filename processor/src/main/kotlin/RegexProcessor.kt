package processor

import annotations.Extract
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * Annotation Processor que processa a anotação @Extract.
 *
 * Gera implementações concretas de classes abstratas que usam
 * expressões regulares para extrair dados de strings.
 *
 * Fluxo de funcionamento:
 * 1. Encontra todos os métodos abstratos anotados com @Extract.
 * 2. Para cada classe que contém esses métodos, gera uma classe concrete.
 * 3. A classe gerada implementa os métodos abstratos usando regex para extrair dados.
 */
// Regista automaticamente este processor no Gradle KAPT
// Sem isto, o Gradle não saberia que esta classe é um annotation processor
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_23) // Indica que este processor suporta código compilado com Java 23
// Diz ao Gradle que este processor processa a anotação "annotations.Extract"
// O Gradle só vai chamar este processor se encontra métodos com @Extract. Se não tiver @Extract, o processor é ignorado
@SupportedAnnotationTypes("annotations.Extract")
class RegexProcessor : AbstractProcessor() {
    /**
     * Método principal do processador, chamado durante a compilação.
     *
     * Este método é invocado pelo Gradle KAPT quando encontra anotações a processar.
     *
     * @param annotations Conjunto de tipos de anotações que o processador deve processar neste round.
     * @param roundEnv Ambiente com informações sobre os elementos anotados encontrados.
     * @return true se a anotação foi processado sucesso. Caso contrário, retorna false.
     *
     */
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Cria um mapa para armazenar uma lista de métodos anotados de uma classe.
        // Chave - TypeElement (representa a classe abstrata, ex: DataProcessor)
        // Valor - MutableList<ExecutableElement> (lista dos métodos anotados)
        val classMethodMap = mutableMapOf<TypeElement, MutableList<ExecutableElement>>()

        // Procurar todos os elementos/métodos anotados com @Extract neste round
        for (element in roundEnv.getElementsAnnotatedWith(Extract::class.java)) {
            // Verifica se o elemento anotado é realmente um ExecutableElement, ou seja, uma função ou um método
            if (element is ExecutableElement) {
                // Obtém a classe que contém este método
                // element.enclosingElement corresponde à classe "pai" do método
                // Exemplo: se o método é DataProcessor.getName(), obtemos DataProcessor
                val enclosingClass = element.enclosingElement as TypeElement

                // Adiciona o método à lista de métodos da sua classe
                // computeIfAbsent: se a classe ainda não está no mapa, cria uma lista vazia { mutableList() } e adiciona essa lista ao mapa
                // Depois adiciona o método a essa lista
                classMethodMap.computeIfAbsent(enclosingClass) { mutableListOf() }.add(element)
            }
        }

        // Para cada classe que tem métodos anotados
        // classElement corresponde à classe em si (ex: DataProcessor)
        // methods corresponde à lista de métodos dessa classe
        for ((classElement, methods) in classMethodMap) {
            // Gera a classe concrete correspondente
            generateKotlinExtractorClass(classElement, methods)
        }

        return true // Retorna true para indicar que o processamento foi bem-sucedido
    }

    /**
     * Gera a classe concrete que implementa os métodos abstratos anotados com @Extract
     *
     * Esta função utiliza KotlinPoet (biblioteca para gerar código Kotlin) para construir uma classe que herda
     * da classe abstrata e implementa cada método abstrato com código que extrai dados usando regex.
     *
     * @param classElement Elemento que representa a classe original
     * @param methods Lista de métodos desta classe anotados com @Extract
     */
    private fun generateKotlinExtractorClass(classElement: TypeElement, methods: List<ExecutableElement>) {
        // Obtém o nome do package da classe original
        // processingEnv.elementUtils = ferramentas do compilador para manipular elementos
        // getPackageOf(classElement) = obtém o package onde a classe está
        // .toString() = converte para String
        // Exemplo: se a classe é "com.example.app.DataProcessor", isto retorna "com.example.app"
        val packageName = processingEnv.elementUtils.getPackageOf(classElement).toString()

        // Obtém o nome simples da classe original (sem package)
        // classElement.simpleName = nome simples
        // .toString() = converte para String
        // Exemplo: se a classe é "com.example.app.DataProcessor", isto retorna "DataProcessor"
        val ogClassName = classElement.simpleName.toString()

        // Constrói o nome da classe gerada
        // Adiciona "Extractor" ao final do nome
        // Exemplo: "DataProcessor" → "DataProcessorExtractor"
        val extractorClassName = "${ogClassName}Extractor"

        // FunSpec.constructorBuilder(), começa a construir um construtor com KotlinPoet
        val constructor = FunSpec.constructorBuilder()
            .addParameter("input", String::class) // Adiciona um parâmetro chamado "input" do tipo String. Este parâmetro será passado à superclasse.
            .build() // Finaliza a construção do construtor e retorna FunSpec

        // TypeSpec.classBuilder(), começa a construir uma classe com KotlinPoet
        val classBuilder = TypeSpec.classBuilder(extractorClassName)
            .primaryConstructor(constructor) // Adiciona o construtor anteriormente criado
            // Define a classe pai (superclasse)
            // ClassName(packageName, ogClassName), cria uma referência de tipo. Exemplo: ClassName("com.example.app", "DataProcessor")
            .superclass(ClassName(packageName, ogClassName))
            // Passa o parâmetro "input" para o construtor da superclasse. Isto garante que quando criamos DataProcessorExtractor(input), o "input" é passado para DataProcessor(input)
            .addSuperclassConstructorParameter("input")
            // Adiciona modificadores de visibilidade e características
            // PUBLIC, a classe é pública (visível de fora do package)
            // FINAL, a classe não pode ser herdada (é final)
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)

        // Para cada método abstrato anotado que precisa ser implementado
        for (method in methods) {
            val methodName = method.simpleName.toString() // Obtém o nome do método

            // Obtém a anotação @Extract do método.
            // method.getAnnotation(), procura a anotação. Se encontrar, retorna a anotação, caso contrário, retorna null.
            val extractAnnotation = method.getAnnotation(Extract::class.java)

            // Obtém o padrão regex da anotação
            // extractAnnotation.regex, acede à propriedade 'regex' da anotação
            // Exemplo: se a anotação é @Extract(regex = "Name : (\\w+)"), isto retorna "Name : (\\w+)"
            val regexPattern = extractAnnotation.regex

            // FunSpec.builder(), começa a construir um método com KotlinPoet
            val funcBuilder = FunSpec.builder(methodName)
                // Adiciona modificadores ao método
                // OVERRIDE, este método sobrescreve o método da superclasse
                // FINAL, este método não pode ser sobrescrito (é final)
                .addModifiers(KModifier.OVERRIDE, KModifier.FINAL)

                // Define o tipo de retorno
                // String::class.asTypeName(), obtém o tipo "String"
                // .copy(nullable = true), torna-o nullable (String?)
                // Isto é importante porque o regex pode não encontrar correspondência
                .returns(String::class.asTypeName().copy(nullable = true))

                // Adiciona a primeira linha do método:
                // "val match = Regex(...).find(input)"
                // %T, placeholder para um tipo (será substituído por Regex)
                // %S, placeholder para uma string (será escapada/quoted)
                // Isto gera: val match = Regex("Name : (\\w+)").find(input)
                .addStatement("val match = %T(%S).find(input)", ClassName("kotlin.text", "Regex"), regexPattern)

                // Adiciona a segunda linha do método:
                // "return match?.groupValues?.get(1)"
                // Esta linha retorna o primeiro grupo capturado pela regex
                // match?.groupValues?.get(1) = acesso seguro (se match é null, retorna null)
                // groupValues = lista dos grupos capturados na regex
                // [1] = obtém o primeiro grupo (o padrão dentro de parênteses)
                // Exemplo: se regex é "Name : (\\w+)" e input é "Name : John",
                // groupValues[1] retorna "John"
                .addStatement("return match?.groupValues?.get(1)")

            classBuilder.addFunction(funcBuilder.build()) // Adiciona o método finalizado à classe
        }

        // Começa a construir um ficheiro .kt usando KotlinPoet
        val file = FileSpec.builder(packageName, extractorClassName)
            .addType(classBuilder.build()) // Adiciona a classe que foi construído ao ficheiro
            .build() // Finaliza o ficheiro

        // Escrever o ficheiro
        try {
            // Obtém o diretório onde o Gradle quer os ficheiros gerados
            // processingEnv.options["kapt.kotlin.generated"] corresponde à variável do Gradle
            val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"]

            // Verifica se o diretório foi definido
            if (kaptKotlinGeneratedDir != null) {
                // Escreve o ficheiro para o disco
                // File(), cria um objeto representando o diretório
                // writeTo(), escreve o ficheiro nesse diretório
                // Resultado: DataProcessorExtractor.kt é criado no diretório
                file.writeTo(File(kaptKotlinGeneratedDir))
            } else {
                // Se o diretório não está definido, há um problema
                // processingEnv.messager, permite imprimir mensagens
                // printMessage(), imprime uma mensagem
                // Diagnostic.Kind.ERROR, mensagem de do tipo: erro
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "kapt.kotlin.generated not found"
                )
            }
        } catch (e: Exception) {
            // Se algo correr mal, lança uma exceção com uma mensagem descritiva do erro
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Erro: ${e.message}"
            )
        }
    }
}