package cofre.protocolo;

import java.util.List;

public class PedidoDeImportacao extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String token;
    private final List<CredencialDto> credenciais;

    public PedidoDeImportacao(String token, List<CredencialDto> credenciais) {
        this.token = token;
        this.credenciais = credenciais;
    }

    public String getToken() { return token; }
    public List<CredencialDto> getCredenciais() { return credenciais; }
}
