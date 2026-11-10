package cofre;

import org.springframework.stereotype.Component;

/**
 * Sessão do usuário: token e chave de cifragem ficam só na memória deste processo (ADR 0002 e 0005).
 * shortcut: uma sessão por processo do Cliente; cada usuário roda o próprio Cliente, como no E1.
 */
@Component
public class SessaoCliente {

    private volatile String token;
    private volatile String chaveCifragem;
    private volatile Long usuarioId;
    private volatile String username;

    public synchronized void iniciar(String token, String chaveCifragem, Long usuarioId, String username) {
        this.token = token;
        this.chaveCifragem = chaveCifragem;
        this.usuarioId = usuarioId;
        this.username = username;
    }

    public synchronized void encerrar() {
        this.token = null;
        this.chaveCifragem = null;
        this.usuarioId = null;
        this.username = null;
    }

    public boolean ativa() {
        return token != null && chaveCifragem != null;
    }

    public String getToken() {
        return token;
    }

    public String getChaveCifragem() {
        return chaveCifragem;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsername() {
        return username;
    }
}
