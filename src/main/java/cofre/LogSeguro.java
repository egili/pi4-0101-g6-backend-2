package cofre;

/** Prepara valores vindos de fora (ex.: nome de usuário) para entrar no log sem forjar linhas. */
final class LogSeguro {

    private static final int TAMANHO_MAXIMO = 100;

    private LogSeguro() {
    }

    static String limpar(Object valor) {
        if (valor == null) {
            return "null";
        }
        String texto = valor.toString().replaceAll("[\\r\\n\\t]", "_").replaceAll("\\p{Cntrl}", "_");
        return texto.length() > TAMANHO_MAXIMO ? texto.substring(0, TAMANHO_MAXIMO) + "..." : texto;
    }
}
