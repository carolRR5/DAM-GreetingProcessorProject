package com.example.app

import annotations.Greeting

/**
 * Classe de exemplo para demonstrar o uso da anotação @Greeting
 *
 * Os métodos anotados com @Greeting serão processados em tempo de compilação para gerar uma classe
 * wrapper (MyClassWrapper) que imprime a mensagem antes de chamar cada método.
 */
open class MyClass {
    /**
     * Método anotado com @Greeting.
     * O processador vai gerar um wrapper que imprime "Hello from MyClass!" antes de chamar este método.
     */
    @Greeting("Hello from MyClass!")
    open fun sayHello() {
        println("Executing sayHello method")
    }

    /**
     * Segundo método anotado com @Greeting.
     * O processador vai gerar um wrapper que imprime "Welcome to the compute function!"
     * antes de chamar este método.
     */
    @Greeting("Welcome to the compute function")
    open fun compute() {
        println("Computing something important...")
    }
}