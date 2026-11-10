package cofre.protocolo;

public class PedidoDeListagem extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String token;

    public PedidoDeListagem(String token) { this.token = token; }

    public String getToken() { return token; }
}
