package cofre;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Login de 2 fases do E3: busca o salt no Servidor, deriva localmente e envia só o hash. */
@Service
public class ClienteAuthService {

    public record UsuarioLogado(Long usuarioId, String username) {}

    private final RestTemplate restTemplate;
    private final CryptoService cryptoService;
    private final SessaoCliente sessao;
    private final String backend1Url;

    public ClienteAuthService(RestTemplate restTemplate, CryptoService cryptoService, SessaoCliente sessao,
                              @Value("${backend1.url}") String backend1Url) {
        this.restTemplate = restTemplate;
        this.cryptoService = cryptoService;
        this.sessao = sessao;
        this.backend1Url = backend1Url;
    }

    public UsuarioLogado login(String username, String senhaMestra) {
        if (vazio(username) || vazio(senhaMestra)) {
            throw new LoginException("CAMPO_OBRIGATORIO_AUSENTE");
        }

        String salt = buscarSalt(username);
        String hashLogin = cryptoService.derivarHashLogin(senhaMestra, salt);
        String chaveCifragem = cryptoService.derivarChaveCifragem(senhaMestra, salt);

        Map<String, Object> resposta = autenticar(username, hashLogin);
        Long usuarioId = ((Number) resposta.get("usuarioId")).longValue();
        sessao.iniciar((String) resposta.get("token"), chaveCifragem, usuarioId, username);

        return new UsuarioLogado(usuarioId, username);
    }

    private String buscarSalt(String username) {
        try {
            Map<?, ?> resposta = restTemplate.getForObject(backend1Url + "/salt/{user}", Map.class, username);
            if (resposta == null || resposta.get("salt") == null) {
                throw new LoginException("ERRO_CONEXAO");
            }
            return (String) resposta.get("salt");
        } catch (HttpClientErrorException.NotFound e) {
            throw new LoginException("USUARIO_NAO_ENCONTRADO");
        } catch (ResourceAccessException e) {
            throw new LoginException("ERRO_CONEXAO");
        }
    }

    private Map<String, Object> autenticar(String username, String hashLogin) {
        try {
            return restTemplate.postForObject(backend1Url + "/login",
                    Map.of("username", username, "hashLogin", hashLogin), Map.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new LoginException("SENHA_INCORRETA");
        } catch (ResourceAccessException e) {
            throw new LoginException("ERRO_CONEXAO");
        } catch (HttpServerErrorException e) {
            throw new LoginException("ERRO_BANCO");
        }
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
