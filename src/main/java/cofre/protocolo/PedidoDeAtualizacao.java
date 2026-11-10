package cofre.protocolo;

public class PedidoDeAtualizacao extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String token;
    private final Long credencialId;
    private final String servico;
    private final String usuarioServico;
    private final String dadosCifrados;
    private final String iv;

    public PedidoDeAtualizacao(String token, Long credencialId, String servico, String usuarioServico, String dadosCifrados, String iv) {
        this.token = token;
        this.credencialId = credencialId;
        this.servico = servico;
        this.usuarioServico = usuarioServico;
        this.dadosCifrados = dadosCifrados;
        this.iv = iv;
    }

    public String getToken() { return token; }
    public Long getCredencialId() { return credencialId; }
    public String getServico() { return servico; }
    public String getUsuarioServico() { return usuarioServico; }
    public String getDadosCifrados() { return dadosCifrados; }
    public String getIv() { return iv; }
}
