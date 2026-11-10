package cofre.protocolo;

public class PedidoDeExportacao extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String token;

    public PedidoDeExportacao(String token) { this.token = token; }

    public String getToken() { return token; }
}
