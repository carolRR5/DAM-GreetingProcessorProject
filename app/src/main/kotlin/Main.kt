package com.example.app

/**
 * Classe principal para testar o annotation processor.
 *
 * Este programa demonstra o funcionamento do GreetingProcessor:
 *      1. Cria uma instância de MyClass (classe original)
 *      2. Envolve-a numa MyClassWrapper (classe gerada)
 *      3. Chama os métodos wrapper, que imprimem a mensagem antes de chamar o original
 *
 * Output esperado:
 * Hello from MyClass!
 * Executing sayHello method
 * Welcome to the compute function!
 * Computing something important...
 */
fun main() {
    val myClass = MyClass() // Cria uma instância da classe original

    // Envolve a instância numa classe wrapper gerada automaticamente
    // O wrapper foi gerado pelo GreetingProcessor durante a compilação
    val wrappedMyClass = MyClassWrapper(myClass) // Use the wrapper class

    // Chama o método sayHello através do wrapper
    // O wrapper vai imprimir "Hello from MyClass!" e vai chamar myCLass..sayHello() que imprime "Executing sayHello method"
    wrappedMyClass.sayHello()

    // Chama o método compute através do wrapper
    // O wrapper vai imprimir "Welcome to the compute function!" e vai chamar myClass.compute() que imprime "Computing something important..."
    wrappedMyClass.compute()
}