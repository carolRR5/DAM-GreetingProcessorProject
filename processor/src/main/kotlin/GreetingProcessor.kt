package processor

import annotations.Greeting
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * Annotation Processor que processa a anotação @Greeting
 *
 * Este processador é executado automaticamente durante a compilação do Kotlin e gera classes wrapper para todos
 * os métodos anotados com @Greeting
 *
 * Fluxo de funcionamento:
 *      1. Durante a compilação, o Gradle KAPT deteta métodos com @Greeting
 *      2. Este processador é chamado automaticamente
 *      3. Encontra todos os métodos anotados
 *      4. Para cada classe, gera uma classe wrapper correspondente
 *      5. A classe wrapper "envolve" os métodos originais com mensagem de cumprimento
 */

// Regista automaticamente o processor no Gradle
// Sem isto, o Gradle não saberia que este annotation processor
@AutoService(Processor::class)
// Indica que este processor suporta código compilado com Java 23
@SupportedSourceVersion(SourceVersion.RELEASE_23)
// Diz ao Gradle que este processor processa a anotação "annotations.Greeting"
// Só será chamado quando encontrar métodos com @Greeting
@SupportedAnnotationTypes("annotations.Greeting")
class GreetingProcessor : AbstractProcessor() { // Herda de AbstractProcessor, que fornece métodos para processar anotações
    /**
     * Método principal do processador, chamado durante a compilação.
     *
     * Este método é invocado pelo Gradle KAPT quando encontra anotações a processar.
     *
     * @param annotations Conjunto de tipos de anotações a processar
     * @param roundEnv Ambiente de processamento com informações sobre os elementos (classes, método, etc.)anotados
     * @return true se a notação foi processado com sucesso
     *         false se não foi processado
     */
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Cria um mapa para armazenar: classe -> lista de métodos anotados
        // Chave: TypeElement (representa a classe)
        // Valor: MutableList<ExecutableElement> (representa os métodos da classe)
        // Isto é utilizado porque vários métodos da mesma classe podem ter @Greeting
        val classMethodMap = mutableMapOf<TypeElement, MutableList<ExecutableElement>>()

        // Encontra os métodos/elementos anotados com @Greeting
        // roundEnv.getElementsAnnotatedWith() retorna um conjunto de elementos
        for (element in roundEnv.getElementsAnnotatedWith(Greeting::class.java)) {
            // Verifica se o elemento anotado é realmente um ExecutableElement (método/função)
            if (element is ExecutableElement) {
                // Obtém a classe que contém este método
                val enclosingClass = element.enclosingElement as TypeElement

                // Adiciona o método à lista de métodos da sua classe
                classMethodMap.computeIfAbsent(enclosingClass) {
                    mutableListOf() // Função lambda que cria uma lista vazia se não existir
                }.add(element) // Adiciona o método à lista
            }
        }

        // Para cada classe que tem métodos anotados
        for ((classElement, methods) in classMethodMap) {
            // Gera a classe wrapper correspondente
            generateKotlinWrapperClass(classElement, methods)
        }

        // Retorna true para indicar que processamento foi bem-sucedido
        return true
    }

    /**
     * Gera a classe wrapper para uma classe que contém métodos anotados com @Greeting
     *
     * Este método usa KotlinPoet (biblioteca para gerar código Kotlin) para construir
     * uma classe wrapper que envolve a classe original com mensagens de cumprimento.
     *
     * @param classElement Elemento que representa a classe original
     * @param methods Lista de métodos dessa classe que estão anotados com @Greeting
     */
    private fun generateKotlinWrapperClass(classElement: TypeElement, methods: List<ExecutableElement>) {
        // Passo 1: Obter informações da classe original
        // Obtém o nome do package da classe original
        val packageName = processingEnv.elementUtils.getPackageOf(classElement).toString()

        // Obtém o nome simples da classe original
        val originalClassName = classElement.simpleName.toString()

        // Constrói o nome da classe wrapper
        val wrapperClassName = "${originalClassName}Wrapper"

        // Passo 2: Criar a classe wrapper utilizando KotlinPoet

        // TypeSpec.classBuilder() começa a construir uma classe com KotlinPoet
        val classBuilder = TypeSpec.classBuilder(wrapperClassName)
            .primaryConstructor( // Adiciona um construtor primário que recebe a instância original
                FunSpec.constructorBuilder()
                    // Adiciona um parâmetro chamado "original" do tipo da classe original
                    // ClassName(packageName, originalClassName) cria a referência de tipo
                    .addParameter("original", ClassName(packageName, originalClassName))
                    .build() // Finaliza o construtor
            )
            .addProperty( // Adiciona uma propriedade "original" à classe
                PropertySpec.builder("original", ClassName(packageName, originalClassName))
                    // Inicializa a propriedade com o parâmetro do construtor
                    .initializer("original")
                    .build() // Finaliza a propriedade
            )
            // Adiciona modificadores de visibilidade e características
            // PUBLIC -> a classe é pública
            // FINAL -> a classe não pode ser herdada
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)


        // Passo 3: Gerar métodos wrapper
        // Para cada método anotado, gera um método wrapper correspondente
        for (method in methods) {
            // Obtém o nome do método original
            val methodName = method.simpleName.toString()

            // Extrai os parâmetros do método original
            // Para cada parâmetro, cria um ParameterSpec que será usado no wrapper
            val parameters = method.parameters.map { param ->
                ParameterSpec.builder(
                    param.simpleName.toString(), // Nome do parâmetro
                    param.asType().asTypeName() // Tipo do parâmetro
                ).build()
            }

            // Constrói uma string com os argumentos para chamar o método original
            val arguments = method.parameters.joinToString(", ") {
                it.simpleName.toString()
            }

            // Obtém a messagem de anotação @Greeting
            // method.getAnnotation() encontra a anotação no método
            // ?.message obtém o parâmetro "message" da anotação
            // ?: " Hello !" é um valor padrão se a anotação não tiver mensagem
            val greetingMessage =
                method.getAnnotation(Greeting::class.java)?.message ?: " Hello !"

            // Constrói o método wrapper utilizando KotlinPoet
            val methodBuilder = FunSpec.builder(methodName)
                // Adiciona modificadores (public, final)
                .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
                // Adiciona os mesmos parâmetros que o método original
                .addParameters(parameters)
                // Adiciona a primeira linha: imprimir a mensagem
                .addStatement("println(%S)", greetingMessage) // Print greeting message
                // Adiciona a segunda linha: chamar o método original
                .addStatement("original.$methodName($arguments)")

            // Adiciona este método à classe wrapper
            classBuilder.addFunction(methodBuilder.build())
        }

        // Passo 4: Construir o ficheiro Kotlin
        // Cria um ficheiro Kotlin com a classe wrapper
        // FileSpec representa um ficheiro .kt
        val file = FileSpec.builder(packageName, wrapperClassName)
            // Adiciona a classe construída ao ficheiro
            .addType(classBuilder.build())
            .build() // Finaliza o ficheiro

        // Passo 5: Escrever o ficheiro gerado para o disco
        // Write the generated file
        try {
            // Obtém o diretório onde o Gradle KAPT quer que escrevamos os ficheiros gerados
            // Esta variável é definida automaticamente pelo Gradle KAPT
            val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"]

            // Verifica se o diretório foi definido
            if (kaptKotlinGeneratedDir != null) {
                // Escreve o ficheiro gerado para o diretório
                // File(kaptKotlinGeneratedDir) cria um objeto representando o diretório
                file.writeTo(File(kaptKotlinGeneratedDir)) // Correct way to write Kotlin files
                // Neste ponto, o ficheiro MyClassWrapper.kt foi criado com sucesso
            } else {
                // Se o diretório não está definido, há um problema
                // Imprime uma mensagem de erro
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "kapt.kotlin.generated not found")

            }
        } catch (e: Exception) {
            // Se algo correr mal (problema de ficheiros, permissões, etc.)
            // Imprime uma mensagem de erro detalhada
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Error generating Kotlin file: ${e.message}")
        }

    }
}