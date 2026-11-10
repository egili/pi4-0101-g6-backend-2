package cofre.protocolo;

public class PedidoDeSalt extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String username;

    public PedidoDeSalt(String username) { this.username = username; }

    public String getUsername() { return username; }
}
