package cofre.protocolo;

public class PedidoDeCriacao extends PedidoDeCredencial {
    private static final long serialVersionUID = 1L;

    public PedidoDeCriacao(String token, String servico, String usuarioServico, String dadosCifrados, String iv) {
        super(token, servico, usuarioServico, dadosCifrados, iv);
    }
}
