package cofre;

/** Erro de login com o código do contrato E3, que o front mostra ao usuário. */
public class LoginException extends RuntimeException {

    private final String codigo;

    public LoginException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
