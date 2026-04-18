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
 */
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_23)  // ← CORRIGIDO para 23
@SupportedAnnotationTypes("annotations.Extract")
class RegexProcessor : AbstractProcessor() {

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val classMethodMap = mutableMapOf<TypeElement, MutableList<ExecutableElement>>()

        // Procurar os elementos anotados com @Extract
        for (element in roundEnv.getElementsAnnotatedWith(Extract::class.java)) {
            if (element is ExecutableElement) {
                val enclosingClass = element.enclosingElement as TypeElement
                classMethodMap.computeIfAbsent(enclosingClass) { mutableListOf() }.add(element)
            }
        }

        for ((classElement, methods) in classMethodMap) {
            generateKotlinExtractorClass(classElement, methods)
        }

        return true
    }

    private fun generateKotlinExtractorClass(
        classElement: TypeElement,
        methods: List<ExecutableElement>
    ) {
        val packageName = processingEnv.elementUtils.getPackageOf(classElement).toString()
        val ogClassName = classElement.simpleName.toString()
        val extractorClassName = "${ogClassName}Extractor"

        // Criar o construtor
        val constructor = FunSpec.constructorBuilder()
            .addParameter("input", String::class)
            .build()

        // Criar a classe
        val classBuilder = TypeSpec.classBuilder(extractorClassName)
            .primaryConstructor(constructor)
            .superclass(ClassName(packageName, ogClassName))
            .addSuperclassConstructorParameter("input")
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)

        // Criar as funções que vão substituir as abstratas
        for (method in methods) {
            val methodName = method.simpleName.toString()
            val extractAnnotation = method.getAnnotation(Extract::class.java)
            val regexPattern = extractAnnotation.regex

            val funcBuilder = FunSpec.builder(methodName)
                .addModifiers(KModifier.OVERRIDE, KModifier.FINAL)
                .returns(String::class.asTypeName().copy(nullable = true))
                .addStatement("val match = %T(%S).find(input)",
                    ClassName("kotlin.text", "Regex"),
                    regexPattern)
                .addStatement("return match?.groupValues?.get(1)")

            classBuilder.addFunction(funcBuilder.build())
        }

        // Preparar o ficheiro final
        val file = FileSpec.builder(packageName, extractorClassName)
            .addType(classBuilder.build())
            .build()

        // Escrever o ficheiro
        try {
            val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"]
            if (kaptKotlinGeneratedDir != null) {
                file.writeTo(File(kaptKotlinGeneratedDir))
            } else {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "kapt.kotlin.generated not found"
                )
            }
        } catch (e: Exception) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Erro: ${e.message}"
            )
        }
    }
}