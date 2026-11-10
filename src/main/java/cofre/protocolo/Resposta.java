package cofre.protocolo;

import java.util.HashMap;
import java.util.Map;

/** Resposta do Servidor: ou um código de erro do contrato, ou os dados do pedido. */
public class Resposta extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final String erro;
    private final HashMap<String, Object> dados;

    public Resposta(String erro, Map<String, Object> dados) {
        this.erro = erro;
        this.dados = dados == null ? null : new HashMap<>(dados);
    }

    public String getErro() { return erro; }
    public boolean ok() { return erro == null; }
    public Map<String, Object> getDados() { return dados; }
}
