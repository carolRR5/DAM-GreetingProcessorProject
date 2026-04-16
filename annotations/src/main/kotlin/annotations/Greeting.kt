package annotations

/**
 * Anotação @Greeting para anotar funções com mensagens de cumprimento
 *
 * Esta anotação é processada em tempo de compilação pelo GreetingProcessor para gerar automaticamente classes wrapper
 * que imprimem uma mensagem antes de chamar o método original.
 *
 * O processor irá gerar uma classe wrapper que faz:
 *      1. Imprime "Olá mundo!"
 *      2. Chama o método original SayHello()
 */
// Esta anotação só pode ser aplicada a funções
@Target(AnnotationTarget.FUNCTION)
// A anotação é descartada após compilação, apenas utilizada em tempo de compilação para processamento
@Retention(AnnotationRetention.SOURCE)
// Parâmetro da anotação: a mensagem de cumprimento a imprimir
annotation class Greeting (val message: String )

