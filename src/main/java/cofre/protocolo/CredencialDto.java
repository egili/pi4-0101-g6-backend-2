package cofre.protocolo;

import java.io.Serializable;

/** Credencial já cifrada, como trafega entre os programas. Nunca tem texto claro. */
public class CredencialDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long credencialId;
    private final String servico;
    private final String usuarioServico;
    private final String dadosCifrados;
    private final String iv;

    public CredencialDto(Long credencialId, String servico, String usuarioServico, String dadosCifrados, String iv) {
        this.credencialId = credencialId;
        this.servico = servico;
        this.usuarioServico = usuarioServico;
        this.dadosCifrados = dadosCifrados;
        this.iv = iv;
    }

    public Long getCredencialId() { return credencialId; }
    public String getServico() { return servico; }
    public String getUsuarioServico() { return usuarioServico; }
    public String getDadosCifrados() { return dadosCifrados; }
    public String getIv() { return iv; }
}
