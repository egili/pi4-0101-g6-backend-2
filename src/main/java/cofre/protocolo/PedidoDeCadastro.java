package cofre.protocolo;

public class PedidoDeCadastro extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String senhaMestraHash;
    private final String salt;

    public PedidoDeCadastro(String username, String senhaMestraHash, String salt) {
        this.username = username;
        this.senhaMestraHash = senhaMestraHash;
        this.salt = salt;
    }

    public String getUsername() { return username; }
    public String getSenhaMestraHash() { return senhaMestraHash; }
    public String getSalt() { return salt; }
}
