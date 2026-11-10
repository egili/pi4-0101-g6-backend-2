package cofre.protocolo;

public class PedidoDeLogin extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String hashLogin;

    public PedidoDeLogin(String username, String hashLogin) {
        this.username = username;
        this.hashLogin = hashLogin;
    }

    public String getUsername() { return username; }
    public String getHashLogin() { return hashLogin; }
}
