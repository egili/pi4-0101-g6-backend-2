package cofre.protocolo;

import java.util.List;

public class PedidoDeTrocaSenha extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String token;
    private final String hashLoginAtual;
    private final String novoSenhaMestraHash;
    private final String novoSalt;
    private final List<CredencialDto> credenciais;

    public PedidoDeTrocaSenha(String token, String hashLoginAtual, String novoSenhaMestraHash, String novoSalt, List<CredencialDto> credenciais) {
        this.token = token;
        this.hashLoginAtual = hashLoginAtual;
        this.novoSenhaMestraHash = novoSenhaMestraHash;
        this.novoSalt = novoSalt;
        this.credenciais = credenciais;
    }

    public String getToken() { return token; }
    public String getHashLoginAtual() { return hashLoginAtual; }
    public String getNovoSenhaMestraHash() { return novoSenhaMestraHash; }
    public String getNovoSalt() { return novoSalt; }
    public List<CredencialDto> getCredenciais() { return credenciais; }
}
