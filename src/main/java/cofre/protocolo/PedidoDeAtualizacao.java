package cofre.protocolo;

public class PedidoDeAtualizacao extends PedidoDeCredencial {
    private static final long serialVersionUID = 1L;
    private final Long credencialId;

    public PedidoDeAtualizacao(String token, Long credencialId, String servico, String usuarioServico, String dadosCifrados, String iv) {
        super(token, servico, usuarioServico, dadosCifrados, iv);
        this.credencialId = credencialId;
    }

    public Long getCredencialId() { return credencialId; }
}
