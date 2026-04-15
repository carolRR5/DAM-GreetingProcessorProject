package annotations

// Esta anotação só pode ser aplicada a funções
@Target(AnnotationTarget.FUNCTION)
// A anotação é descartada após compilação, apenas utilizada em tempo de compilação para processamento
@Retention(AnnotationRetention.SOURCE)
// Parâmetro da anotação: a mensagem de cumprimento a imprimir
annotation class Greeting (val message: String )

