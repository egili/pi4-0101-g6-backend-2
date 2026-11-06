package cofre;

import org.springframework.stereotype.Component;

/**
 * Sessão do usuário logado: token e chave de cifragem ficam só na memória deste processo.
 * shortcut: uma sessão por processo do Cliente, trocar por um mapa por usuário ao atender E1 com vários usuários no mesmo Cliente.
 */
@Component
public class SessaoCliente {

    private volatile String token;
    private volatile String chaveCifragem;
    private volatile Long usuarioId;
    private volatile String username;

    public void iniciar(String token, String chaveCifragem, Long usuarioId, String username) {
        this.token = token;
        this.chaveCifragem = chaveCifragem;
        this.usuarioId = usuarioId;
        this.username = username;
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
