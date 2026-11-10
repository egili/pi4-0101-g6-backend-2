package cofre.protocolo;

public class PedidoDeExclusao extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String token;
    private final Long credencialId;

    public PedidoDeExclusao(String token, Long credencialId) {
        this.token = token;
        this.credencialId = credencialId;
    }

    public String getToken() { return token; }
    public Long getCredencialId() { return credencialId; }
}
