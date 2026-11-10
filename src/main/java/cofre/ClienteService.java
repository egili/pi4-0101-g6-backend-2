package cofre;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso do Cliente. A chave de cifragem nunca sai daqui: o navegador só recebe texto
 * para mostrar e envia texto para cifrar (ADR 0002 e 0005). O .vault é montado aqui (ADR 0006).
 */
@Service
public class ClienteService {

    static final int VERSAO_VAULT = 1;
    static final String ALGORITMO_VAULT = "AES-256-GCM";
    private static final int SALT_LENGTH = 16;

    private final CryptoService crypto;
    private final ClienteServidor servidor;
    private final SessaoCliente sessao;
    private final ObjectMapper mapper;
    private final SecureRandom random = new SecureRandom();

    public ClienteService(CryptoService crypto, ClienteServidor servidor, SessaoCliente sessao, ObjectMapper mapper) {
        this.crypto = crypto;
        this.servidor = servidor;
        this.sessao = sessao;
        this.mapper = mapper;
    }

    public static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put((String) pares[i], pares[i + 1]);
        }
        return m;
    }

    public Map<String, Object> cadastrar(String username, String senha) {
        exigirCampos(username, senha);
        CryptoResponse credenciais = crypto.gerarCredenciais(senha);
        Map<String, Object> resposta = servidor.cadastrar(username, credenciais.getHashLogin(), credenciais.getSalt());
        if (resposta == null || resposta.get("usuarioId") == null) {
            throw new ErroCliente("ERRO_BANCO");
        }
        return mapa("usuarioId", resposta.get("usuarioId"), "mensagem", "Conta criada com sucesso");
    }

    public Map<String, Object> login(String username, String senha) {
        exigirCampos(username, senha);
        String salt = servidor.buscarSalt(username);
        String hashLogin = crypto.derivarHashLogin(senha, salt);
        String chaveCifragem = crypto.derivarChaveCifragem(senha, salt);

        Map<String, Object> resposta = servidor.login(username, hashLogin);
        Long usuarioId = ((Number) resposta.get("usuarioId")).longValue();
        sessao.iniciar((String) resposta.get("token"), chaveCifragem, usuarioId, username);

        return mapa("usuarioId", usuarioId, "username", username, "mensagem", "Login realizado com sucesso");
    }

    public void logout() {
        sessao.encerrar();
    }

    public List<Map<String, Object>> listar() {
        String token = exigirSessao();
        List<Map<String, Object>> itens = new ArrayList<>();
        for (Map<String, Object> item : servidor.listar(token)) {
            itens.add(mapa("credencialId", item.get("credencialId"),
                    "servico", item.get("servico"),
                    "usuarioServico", item.get("usuarioServico")));
        }
        return itens;
    }

    public Map<String, Object> salvar(String servico, String usuarioServico, String senha) {
        String token = exigirSessao();
        exigirCredencial(servico, usuarioServico, senha);
        CifraResponse cifra = crypto.cifrar(senha, sessao.getChaveCifragem());
        return servidor.criar(token, mapa("servico", servico, "usuarioServico", usuarioServico,
                "dadosCifrados", cifra.getDadosCifrados(), "iv", cifra.getIv()));
    }

    public String revelar(String id) {
        String token = exigirSessao();
        Map<String, Object> item = servidor.listar(token).stream()
                .filter(i -> String.valueOf(i.get("credencialId")).equals(id))
                .findFirst()
                .orElseThrow(() -> new ErroCliente("CREDENCIAL_NAO_ENCONTRADA"));
        return decifrar(item);
    }

    public Map<String, Object> editar(String id, String servico, String usuarioServico, String senha) {
        String token = exigirSessao();
        exigirCredencial(servico, usuarioServico, senha);
        CifraResponse cifra = crypto.cifrar(senha, sessao.getChaveCifragem());
        return servidor.atualizar(token, id, mapa("servico", servico, "usuarioServico", usuarioServico,
                "dadosCifrados", cifra.getDadosCifrados(), "iv", cifra.getIv()));
    }

    public Map<String, Object> excluir(String id) {
        return servidor.excluir(exigirSessao(), id);
    }

    public Map<String, Object> exportar(String senhaBackup) {
        String token = exigirSessao();
        exigirSenhaBackup(senhaBackup);

        Map<String, Object> resposta = servidor.exportarBackup(token);
        Object lista = resposta.get("credenciais");
        if (!(lista instanceof List<?> credenciais) || credenciais.isEmpty()) {
            throw new ErroCliente("BACKUP_VAZIO");
        }

        List<Map<String, Object>> emClaro = new ArrayList<>();
        for (Object item : credenciais) {
            @SuppressWarnings("unchecked")
            Map<String, Object> c = (Map<String, Object>) item;
            emClaro.add(mapa("servico", c.get("servico"), "usuarioServico", c.get("usuarioServico"),
                    "senha", decifrar(c)));
        }

        String salt = gerarSalt();
        CifraResponse corpo = crypto.cifrar(serializar(emClaro), crypto.derivarChaveVault(senhaBackup, salt));
        return mapa("versao", VERSAO_VAULT, "algoritmo", ALGORITMO_VAULT,
                "salt", salt, "iv", corpo.getIv(), "corpo", corpo.getDadosCifrados());
    }

    public Map<String, Object> importar(Map<String, Object> vault, String senhaBackup) {
        String token = exigirSessao();
        exigirSenhaBackup(senhaBackup);

        if (!(vault != null && vault.get("versao") instanceof Number versao && versao.intValue() == VERSAO_VAULT)
                || !(vault.get("salt") instanceof String salt && !salt.isBlank())
                || !(vault.get("iv") instanceof String iv && !iv.isBlank())
                || !(vault.get("corpo") instanceof String corpo && !corpo.isBlank())) {
            throw new ErroCliente("DADOS_INVALIDOS");
        }

        String json;
        try {
            json = crypto.decifrar(corpo, iv, crypto.derivarChaveVault(senhaBackup, salt));
        } catch (RuntimeException e) {
            json = null;
        }
        if (json == null) {
            throw new ErroCliente("SENHA_INCORRETA");
        }

        List<Map<String, Object>> emClaro = lerLista(json);
        if (emClaro.isEmpty()) {
            throw new ErroCliente("BACKUP_VAZIO");
        }

        List<Map<String, Object>> reCifradas = new ArrayList<>();
        for (Map<String, Object> c : emClaro) {
            String servico = (String) c.get("servico");
            String usuarioServico = (String) c.get("usuarioServico");
            String senha = (String) c.get("senha");
            exigirCredencial(servico, usuarioServico, senha);
            CifraResponse cifra = crypto.cifrar(senha, sessao.getChaveCifragem());
            reCifradas.add(mapa("servico", servico, "usuarioServico", usuarioServico,
                    "dadosCifrados", cifra.getDadosCifrados(), "iv", cifra.getIv()));
        }

        Map<String, Object> resposta = servidor.importarBackup(token, reCifradas);
        return mapa("credenciaisInseridas", resposta.get("credenciaisInseridas"), "mensagem", "Backup restaurado com sucesso");
    }

    private String decifrar(Map<String, Object> item) {
        String senha;
        try {
            senha = crypto.decifrar((String) item.get("dadosCifrados"), (String) item.get("iv"), sessao.getChaveCifragem());
        } catch (RuntimeException e) {
            senha = null;
        }
        if (senha == null) {
            throw new ErroCliente("FALHA_DECIFRAGEM");
        }
        return senha;
    }

    private String exigirSessao() {
        if (!sessao.ativa()) {
            throw new ErroCliente("SESSAO_INVALIDA");
        }
        return sessao.getToken();
    }

    private void exigirCampos(String username, String senha) {
        if (vazio(username) || vazio(senha)) {
            throw new ErroCliente("CAMPO_OBRIGATORIO_AUSENTE");
        }
    }

    private void exigirCredencial(String servico, String usuarioServico, String senha) {
        if (vazio(servico) || vazio(usuarioServico) || vazio(senha)) {
            throw new ErroCliente("DADOS_INVALIDOS");
        }
    }

    private void exigirSenhaBackup(String senhaBackup) {
        if (vazio(senhaBackup)) {
            throw new ErroCliente("CAMPO_OBRIGATORIO_AUSENTE");
        }
    }

    private String gerarSalt() {
        byte[] bytes = new byte[SALT_LENGTH];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String serializar(Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (Exception e) {
            throw new ErroCliente("ERRO_BANCO");
        }
    }

    private List<Map<String, Object>> lerLista(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new ErroCliente("DADOS_INVALIDOS");
        }
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
