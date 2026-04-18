package annotations

/**
 * Anotação @Extract para anotar métodos abstratos com padrões regex.
 *
 * Esta anotação é processada em tempo de compilação pelo RegexProcessor para gerar automaticamente a
 * implementação de métodos que extraem dados de uma string usando expressões regulares.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Extract (
    val regex: String
)