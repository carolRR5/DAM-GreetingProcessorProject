package com.example.app

/**
 * Classe principal para testar o annotation processor.
 *
 * Este programa testa dois exercícios:
 * - Exercício 1: GreetingProcessor
 * - Exercício 2: RegexProcessor
 */
fun main() {
    // Exercício 1
    // Demonstra como o GreetingProcessor gera uma classe wrapper que imprime
    // uma mensagem antes de chamar o método original.
    /*val myClass = MyClass() // Cria uma instância da classe original

    // Envolve a instância numa classe wrapper gerada automaticamente
    // O wrapper foi gerado pelo GreetingProcessor durante a compilação
    val wrappedMyClass = MyClassWrapper(myClass) // Use the wrapper class

    // Chama o método sayHello através do wrapper
    // O wrapper vai imprimir "Hello from MyClass!" e vai chamar myCLass..sayHello() que imprime "Executing sayHello method"
    wrappedMyClass.sayHello()

    // Chama o método compute através do wrapper
    // O wrapper vai imprimir "Welcome to the compute function!" e vai chamar myClass.compute() que imprime "Computing something important..."
    wrappedMyClass.compute()*/

    // Exercício 2
    // Demonstra como o RegexProcessor gera uma classe que extrai
    // dados de uma string usando expressões regulares.

    // String de entrada com dados estruturados
    val input = "Name : John Address : 123 Street"

    // Instancia a classe gerada pelo RegexProcessor
    val extractor = DataProcessorExtractor(input)

    // Chama getName() que usa regex "Name : (\\w+)"
    println("Name: ${extractor.getName()}")

    // Chama getAddress() que usa regex "Address : (.+)"
    println("Address: ${extractor.getAddress()}")
}