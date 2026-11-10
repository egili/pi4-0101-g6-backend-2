package cofre;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/** Chamadas HTTP do Cliente ao Servidor (Back-end 1). Só leva hashes, token e blobs cifrados. */
@Component
public class ClienteServidor {

    private final RestTemplate restTemplate;
    private final String backend1Url;

    public ClienteServidor(RestTemplate restTemplate, @Value("${backend1.url}") String backend1Url) {
        this.restTemplate = restTemplate;
        this.backend1Url = backend1Url;
    }

    public String buscarSalt(String username) {
        try {
            Map<?, ?> resposta = restTemplate.getForObject(backend1Url + "/salt/{user}", Map.class, username);
            if (resposta == null || resposta.get("salt") == null) {
                throw new ErroCliente("ERRO_CONEXAO");
            }
            return (String) resposta.get("salt");
        } catch (RuntimeException e) {
            throw traduzir(e, false);
        }
    }

    public Map<String, Object> login(String username, String hashLogin) {
        try {
            return restTemplate.postForObject(backend1Url + "/login",
                    Map.of("username", username, "hashLogin", hashLogin), Map.class);
        } catch (RuntimeException e) {
            throw traduzir(e, true);
        }
    }

    public Map<String, Object> cadastrar(String username, String senhaMestraHash, String salt) {
        try {
            return restTemplate.postForObject(backend1Url + "/usuarios",
                    Map.of("username", username, "senhaMestraHash", senhaMestraHash, "salt", salt), Map.class);
        } catch (RuntimeException e) {
            throw traduzir(e, false);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listar(String token) {
        Object credenciais = chamar(HttpMethod.GET, token, "/credentials", null).get("credenciais");
        if (!(credenciais instanceof List<?> lista)) {
            throw new ErroCliente("ERRO_BANCO");
        }
        return (List<Map<String, Object>>) lista;
    }

    public Map<String, Object> criar(String token, Map<String, Object> corpo) {
        return chamar(HttpMethod.POST, token, "/credentials", corpo);
    }

    public Map<String, Object> atualizar(String token, String id, Map<String, Object> corpo) {
        return chamar(HttpMethod.PUT, token, "/credentials/" + id, corpo);
    }

    public Map<String, Object> excluir(String token, String id) {
        return chamar(HttpMethod.DELETE, token, "/credentials/" + id, null);
    }

    public Map<String, Object> exportarBackup(String token) {
        return chamar(HttpMethod.GET, token, "/backup", null);
    }

    public Map<String, Object> importarBackup(String token, List<Map<String, Object>> credenciais) {
        return chamar(HttpMethod.POST, token, "/backup", credenciais);
    }

    private Map<String, Object> chamar(HttpMethod metodo, String token, String caminho, Object corpo) {
        try {
            return restTemplate.exchange(backend1Url + caminho, metodo, autorizado(token, corpo), Map.class).getBody();
        } catch (RuntimeException e) {
            throw traduzir(e, false);
        }
    }

    private HttpEntity<Object> autorizado(String token, Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, headers);
    }

    private ErroCliente traduzir(RuntimeException e, boolean login) {
        if (e instanceof ErroCliente erro) return erro;
        if (e instanceof ResourceAccessException) return new ErroCliente("ERRO_CONEXAO");
        if (e instanceof HttpStatusCodeException h) {
            int status = h.getStatusCode().value();
            if (status == 401) return new ErroCliente(login ? "SENHA_INCORRETA" : "SESSAO_INVALIDA");
            if (status == 403) return new ErroCliente("ACESSO_NEGADO");
            if (status == 404) return new ErroCliente("CREDENCIAL_NAO_ENCONTRADA");
            if (status == 409) return new ErroCliente("USUARIO_JA_EXISTE");
            if (status == 429) return new ErroCliente("MUITAS_TENTATIVAS");
            if (status == 400) return new ErroCliente("DADOS_INVALIDOS");
            return new ErroCliente("ERRO_BANCO");
        }
        return new ErroCliente("ERRO_CONEXAO");
    }
}
