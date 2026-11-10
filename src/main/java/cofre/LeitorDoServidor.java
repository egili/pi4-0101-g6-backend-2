package cofre;

import cofre.protocolo.Comunicado;
import cofre.protocolo.ComunicadoDeDesligamento;
import cofre.protocolo.Parceiro;
import cofre.protocolo.Resposta;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Thread única que lê tudo que chega do Servidor: respostas vão para a fila de quem pediu,
 * e o aviso de desligamento encerra a conexão. Ter um só leitor evita dois leitores no mesmo stream.
 */
public class LeitorDoServidor extends Thread {

    private final Parceiro servidor;
    private final BlockingQueue<Resposta> respostas;
    private final Consumer<Parceiro> aoEncerrar;

    public LeitorDoServidor(Parceiro servidor, BlockingQueue<Resposta> respostas, Consumer<Parceiro> aoEncerrar) {
        this.servidor = servidor;
        this.respostas = respostas;
        this.aoEncerrar = aoEncerrar;
        setName("leitor-do-servidor");
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            for (;;) {
                Comunicado recebido = servidor.envie();
                if (recebido == null || recebido instanceof ComunicadoDeDesligamento) {
                    break;
                }
                if (recebido instanceof Resposta resposta) {
                    respostas.add(resposta);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // conexão caiu
        } finally {
            servidor.adeus();
            aoEncerrar.accept(servidor);
        }
    }
}
