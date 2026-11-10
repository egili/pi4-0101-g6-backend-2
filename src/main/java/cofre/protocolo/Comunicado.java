package cofre.protocolo;

import java.io.Serializable;

/** Topo da hierarquia de mensagens trocadas por socket entre o Cliente e o Servidor. */
public abstract class Comunicado implements Serializable {
    private static final long serialVersionUID = 1L;
}
