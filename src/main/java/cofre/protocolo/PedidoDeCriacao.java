package cofre.protocolo;

public class PedidoDeCriacao extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String token;
    private final String servico;
    private final String usuarioServico;
    private final String dadosCifrados;
    private final String iv;

    public PedidoDeCriacao(String token, String servico, String usuarioServico, String dadosCifrados, String iv) {
        this.token = token;
        this.servico = servico;
        this.usuarioServico = usuarioServico;
        this.dadosCifrados = dadosCifrados;
        this.iv = iv;
    }

    public String getToken() { return token; }
    public String getServico() { return servico; }
    public String getUsuarioServico() { return usuarioServico; }
    public String getDadosCifrados() { return dadosCifrados; }
    public String getIv() { return iv; }
}
