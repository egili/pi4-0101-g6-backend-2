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
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Conexão do Cliente com o Servidor por socket. Uma conexão, um pedido de cada vez.
 * shortcut: pedidos são serializados (um por vez); se o Cliente precisar de pedidos paralelos, correlacionar respostas por id.
 */
@Component
public class ClienteServidor {

    private static final long TIMEOUT_SEGUNDOS = 15;

    private final String host;
    private final int porta;
    private final Object trava = new Object();
    private final BlockingQueue<Resposta> respostas = new LinkedBlockingQueue<>();
    private volatile Parceiro servidor;
    private Socket conexao;

    public ClienteServidor(@Value("${servidor.host:localhost}") String host,
                           @Value("${servidor.porta:8090}") int porta) {
        this.host = host;
        this.porta = porta;
    }

    public String buscarSalt(String username) {
        Object salt = pedir(new PedidoDeSalt(username)).getDados().get("salt");
        if (!(salt instanceof String texto)) throw new ErroCliente("ERRO_CONEXAO");
        return texto;
    }

    public Map<String, Object> login(String username, String hashLogin) {
        return pedir(new PedidoDeLogin(username, hashLogin)).getDados();
    }

    public Map<String, Object> cadastrar(String username, String senhaMestraHash, String salt) {
        return pedir(new PedidoDeCadastro(username, senhaMestraHash, salt)).getDados();
    }

    public List<Map<String, Object>> listar(String token) {
        return paraMapas(pedir(new PedidoDeListagem(token)).getDados());
    }

    public Map<String, Object> criar(String token, Map<String, Object> corpo) {
        return pedir(new PedidoDeCriacao(token, texto(corpo, "servico"), texto(corpo, "usuarioServico"),
                texto(corpo, "dadosCifrados"), texto(corpo, "iv"))).getDados();
    }

    public Map<String, Object> atualizar(String token, String id, Map<String, Object> corpo) {
        return pedir(new PedidoDeAtualizacao(token, Long.valueOf(id), texto(corpo, "servico"),
                texto(corpo, "usuarioServico"), texto(corpo, "dadosCifrados"), texto(corpo, "iv"))).getDados();
    }

    public Map<String, Object> excluir(String token, String id) {
        return pedir(new PedidoDeExclusao(token, Long.valueOf(id))).getDados();
    }

    public Map<String, Object> exportarBackup(String token) {
        Map<String, Object> saida = new HashMap<>();
        saida.put("credenciais", paraMapas(pedir(new PedidoDeExportacao(token)).getDados()));
        return saida;
    }

    public Map<String, Object> importarBackup(String token, List<Map<String, Object>> credenciais) {
        List<CredencialDto> dtos = new ArrayList<>();
        for (Map<String, Object> c : credenciais) {
            dtos.add(new CredencialDto(null, texto(c, "servico"), texto(c, "usuarioServico"),
                    texto(c, "dadosCifrados"), texto(c, "iv")));
        }
        return pedir(new PedidoDeImportacao(token, dtos)).getDados();
    }

    /** Fecha a conexão ao encerrar o Cliente. */
    @PreDestroy
    public void encerrar() {
        synchronized (trava) {
            desconectar();
        }
    }

    private Resposta pedir(Comunicado pedido) {
        synchronized (trava) {
            conectarSeNecessario();
            try {
                servidor.receba(pedido);
                Resposta resposta = respostas.poll(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
                if (resposta == null) {
                    desconectar();
                    throw new ErroCliente("ERRO_CONEXAO");
                }
                if (!resposta.ok()) {
                    throw new ErroCliente(resposta.getErro());
                }
                return resposta;
            } catch (IOException e) {
                desconectar();
                throw new ErroCliente("ERRO_CONEXAO");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ErroCliente("ERRO_CONEXAO");
            }
        }
    }

    private void conectarSeNecessario() {
        if (servidor != null) return;
        try {
            conexao = new Socket(host, porta);
            ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
            transmissor.flush();
            ObjectInputStream receptor = Parceiro.novoReceptor(conexao.getInputStream());
            Parceiro novo = new Parceiro(conexao, receptor, transmissor);
            respostas.clear();
            servidor = novo;
            new LeitorDoServidor(novo, respostas, this::encerrado).start();
        } catch (IOException e) {
            throw new ErroCliente("ERRO_CONEXAO");
        }
    }

    private void desconectar() {
        Parceiro atual = servidor;
        servidor = null;
        if (atual != null) {
            try {
                atual.receba(new PedidoParaSair());
            } catch (IOException ignorado) {
                // já caiu
            }
            atual.adeus();
        }
    }

    /** Chamado pelo leitor quando o Servidor desliga ou a conexão cai. */
    private void encerrado(Parceiro quem) {
        if (servidor == quem) {
            servidor = null;
        }
    }

    private static String texto(Map<String, ?> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor == null ? null : valor.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> paraMapas(Map<String, Object> dados) {
        List<Map<String, Object>> saida = new ArrayList<>();
        Object lista = dados.get("credenciais");
        if (lista instanceof List<?> itens) {
            for (Object item : itens) {
                CredencialDto c = (CredencialDto) item;
                Map<String, Object> m = new HashMap<>();
                m.put("credencialId", c.getCredencialId());
                m.put("servico", c.getServico());
                m.put("usuarioServico", c.getUsuarioServico());
                m.put("dadosCifrados", c.getDadosCifrados());
                m.put("iv", c.getIv());
                saida.add(m);
            }
        }
        return saida;
    }
}
