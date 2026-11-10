package cofre.protocolo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * O outro lado da conexão. No Cliente representa o Servidor; no Servidor, um Cliente.
 * Só classes de cofre.protocolo, java.lang e java.util são aceitas na leitura (filtro contra desserialização).
 */
public class Parceiro {

    private static final String FILTRO = "cofre.protocolo.*;java.lang.*;java.util.*;maxdepth=8;maxrefs=10000;maxarray=10000;!*";

    private final Socket conexao;
    private final ObjectInputStream receptor;
    private final ObjectOutputStream transmissor;
    private final Semaphore mutEx = new Semaphore(1, true);
    private Comunicado proximoComunicado;

    public Parceiro(Socket conexao, ObjectInputStream receptor, ObjectOutputStream transmissor) {
        if (conexao == null || receptor == null || transmissor == null) {
            throw new IllegalArgumentException("Conexão e streams não podem ser nulos");
        }
        this.conexao = conexao;
        this.receptor = receptor;
        this.transmissor = transmissor;
    }

    /** Cria o receptor com o filtro. O transmissor deve ser criado antes, em ambos os lados. */
    public static ObjectInputStream novoReceptor(InputStream entrada) throws IOException {
        ObjectInputStream receptor = new ObjectInputStream(entrada);
        receptor.setObjectInputFilter(ObjectInputFilter.Config.createFilter(FILTRO));
        return receptor;
    }

    /** O parceiro recebe x: este lado envia. */
    public void receba(Comunicado x) throws IOException {
        synchronized (transmissor) {
            transmissor.writeObject(x);
            transmissor.flush();
        }
    }

    /** Olha o próximo comunicado sem consumi-lo. */
    public Comunicado espie() throws IOException, ClassNotFoundException {
        adquirir();
        try {
            if (proximoComunicado == null) {
                proximoComunicado = lerProximo();
            }
            return proximoComunicado;
        } finally {
            mutEx.release();
        }
    }

    /** O parceiro envia: este lado recebe e consome. Devolve null quando a conexão fecha. */
    public Comunicado envie() throws IOException, ClassNotFoundException {
        adquirir();
        try {
            Comunicado comunicado = proximoComunicado != null ? proximoComunicado : lerProximo();
            proximoComunicado = null;
            return comunicado;
        } finally {
            mutEx.release();
        }
    }

    /** Fecha transmissor, receptor e conexão. */
    public void adeus() {
        fechar(transmissor);
        fechar(receptor);
        fechar(conexao);
    }

    private Comunicado lerProximo() throws IOException, ClassNotFoundException {
        try {
            return (Comunicado) receptor.readObject();
        } catch (EOFException e) {
            return null;
        }
    }

    private void adquirir() throws IOException {
        try {
            mutEx.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Leitura interrompida");
        }
    }

    private static void fechar(AutoCloseable recurso) {
        try {
            recurso.close();
        } catch (Exception ignorado) {
            // já fechado ou conexão caída
        }
    }
}
