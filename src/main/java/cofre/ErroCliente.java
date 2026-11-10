package cofre;

/** Erro do Cliente com o código do contrato. O controller traduz o código em status HTTP. */
public class ErroCliente extends RuntimeException {

    private final String codigo;

    public ErroCliente(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
