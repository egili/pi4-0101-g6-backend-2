package cofre;

import cofre.protocolo.CredencialDto;
import cofre.protocolo.Comunicado;
import cofre.protocolo.Parceiro;
import cofre.protocolo.PedidoDeAtualizacao;
import cofre.protocolo.PedidoDeCadastro;
import cofre.protocolo.PedidoDeCriacao;
import cofre.protocolo.PedidoDeExclusao;
import cofre.protocolo.PedidoDeExportacao;
import cofre.protocolo.PedidoDeImportacao;
import cofre.protocolo.PedidoDeListagem;
import cofre.protocolo.PedidoDeLogin;
import cofre.protocolo.PedidoDeSalt;
import cofre.protocolo.PedidoParaSair;
import cofre.protocolo.Resposta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Servidor de mentira para testar o Cliente pelo socket, com o mesmo protocolo do Back-end 1. */
class ServidorFalso implements AutoCloseable {

    final List<Comunicado> recebidos = new ArrayList<>();
    private final ServerSocket ss;
    private final Map<String, String> salts = new HashMap<>();
    private final Map<String, String> hashes = new HashMap<>();
    private final List<CredencialDto> cofre = new ArrayList<>();
    private long proximoId = 1;
    private final Thread thread;

    ServidorFalso() throws IOException {
        ss = new ServerSocket(0);
        thread = new Thread(this::atender);
        thread.setDaemon(true);
        thread.start();
    }

    int porta() {
        return ss.getLocalPort();
    }

    void cadastrar(String usuario, String salt, String hash) {
        salts.put(usuario, salt);
        hashes.put(usuario, hash);
    }

    List<CredencialDto> cofre() {
        return cofre;
    }

    private void atender() {
        try (Socket s = ss.accept()) {
            ObjectOutputStream t = new ObjectOutputStream(s.getOutputStream());
            t.flush();
            ObjectInputStream r = Parceiro.novoReceptor(s.getInputStream());
            Parceiro p = new Parceiro(s, r, t);
            for (;;) {
                Comunicado pedido = p.envie();
                if (pedido == null || pedido instanceof PedidoParaSair) break;
                recebidos.add(pedido);
                p.receba(responder(pedido));
            }
            p.adeus();
        } catch (IOException | ClassNotFoundException e) {
            // cliente encerrou
        }
    }

    private Resposta responder(Comunicado pedido) {
        if (pedido instanceof PedidoDeSalt p) {
            return new Resposta(null, mapa("salt", salts.getOrDefault(p.getUsername(), "salt-falso")));
        }
        if (pedido instanceof PedidoDeCadastro p) {
            cadastrar(p.getUsername(), p.getSalt(), p.getSenhaMestraHash());
            return new Resposta(null, mapa("usuarioId", 7L));
        }
        if (pedido instanceof PedidoDeLogin p) {
            if (!p.getHashLogin().equals(hashes.get(p.getUsername()))) return new Resposta("SENHA_INCORRETA", null);
            return new Resposta(null, mapa("token", "tok-" + p.getUsername(), "usuarioId", 7L));
        }
        if (!(pedido instanceof PedidoDeListagem) && !(pedido instanceof PedidoDeCriacao)
                && !(pedido instanceof PedidoDeAtualizacao) && !(pedido instanceof PedidoDeExclusao)
                && !(pedido instanceof PedidoDeExportacao) && !(pedido instanceof PedidoDeImportacao)) {
            return new Resposta("DADOS_INVALIDOS", null);
        }
        String token = tokenDo(pedido);
        if (token == null || !token.startsWith("tok-")) return new Resposta("SESSAO_INVALIDA", null);
        if (pedido instanceof PedidoDeListagem) {
            return new Resposta(null, mapa("credenciais", new ArrayList<>(cofre)));
        }
        if (pedido instanceof PedidoDeCriacao c) {
            long id = proximoId++;
            cofre.add(new CredencialDto(id, c.getServico(), c.getUsuarioServico(), c.getDadosCifrados(), c.getIv()));
            return new Resposta(null, mapa("credencialId", id));
        }
        if (pedido instanceof PedidoDeAtualizacao a) {
            cofre.removeIf(x -> x.getCredencialId().equals(a.getCredencialId()));
            cofre.add(new CredencialDto(a.getCredencialId(), a.getServico(), a.getUsuarioServico(), a.getDadosCifrados(), a.getIv()));
            return new Resposta(null, mapa("credencialId", a.getCredencialId()));
        }
        if (pedido instanceof PedidoDeExclusao e) {
            cofre.removeIf(x -> x.getCredencialId().equals(e.getCredencialId()));
            return new Resposta(null, mapa("mensagem", "Credencial excluída com sucesso"));
        }
        if (pedido instanceof PedidoDeExportacao) {
            if (cofre.isEmpty()) return new Resposta("BACKUP_VAZIO", null);
            return new Resposta(null, mapa("credenciais", new ArrayList<>(cofre)));
        }
        PedidoDeImportacao i = (PedidoDeImportacao) pedido;
        for (CredencialDto c : i.getCredenciais()) {
            cofre.add(new CredencialDto(proximoId++, c.getServico(), c.getUsuarioServico(), c.getDadosCifrados(), c.getIv()));
        }
        return new Resposta(null, mapa("credenciaisInseridas", i.getCredenciais().size()));
    }

    private static String tokenDo(Comunicado pedido) {
        if (pedido instanceof PedidoDeListagem p) return p.getToken();
        if (pedido instanceof PedidoDeCriacao p) return p.getToken();
        if (pedido instanceof PedidoDeAtualizacao p) return p.getToken();
        if (pedido instanceof PedidoDeExclusao p) return p.getToken();
        if (pedido instanceof PedidoDeExportacao p) return p.getToken();
        if (pedido instanceof PedidoDeImportacao p) return p.getToken();
        return null;
    }

    private static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put((String) pares[i], pares[i + 1]);
        return m;
    }

    @Override
    public void close() throws IOException {
        ss.close();
    }
}
