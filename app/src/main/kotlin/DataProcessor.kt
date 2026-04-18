package com.example.app

import annotations.Extract

/**
 * Classe abstrata que define métodos para extrair dados de uma string.
 *
 * Os métodos são anotados com @Extract que especifica o padrão regex para extrair cada campo. O RegexProcessor
 * irá gerar uma classe concrete qye implementa estes métodos.
 */
abstract class DataProcessor(val input: String) {
    /**
     * Extrai o nome utilizado a regex pattern.
     *
     * A regex "Name: (\\w+)" procura por:
     * - "Name : " (literalmente)
     * - Seguido de uma ou mais letras/números (\\w+)
     *
     * @return O nome extraído, ou null se não encontrar
     */
    @Extract(regex = "Name : (\\w+)")
    abstract fun getName(): String?

    /**
     * Extrai o endereço usando a regex pattern.
     *
     * A regex "Address : (.+)" procura por:
     * - "Address : " (literalmente)
     * - Seguido de um caráter de qualquer tipo (.+)
     */
    @Extract(regex = "Address : (.+)")
    abstract fun getAddress(): String?
}