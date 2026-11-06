package cofre;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

public class ClienteAuthServiceTest {

    private static final String SERVIDOR = "http://servidor";
    private static final String SENHA = "MinhaSenha!123";

    private final CryptoService crypto = new CryptoService();

    @Test
    public void loginComSenhaCertaGuardaTokenEChaveSoNaSessao() {
        CryptoResponse cadastro = crypto.gerarCredenciais(SENHA);
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(rt).build();
        servidor.expect(requestTo(SERVIDOR + "/salt/eliseu")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"salt\":\"" + cadastro.getSalt() + "\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(SERVIDOR + "/login")).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString(cadastro.getHashLogin())))
                .andRespond(withSuccess("{\"token\":\"tok-1\",\"usuarioId\":7}", MediaType.APPLICATION_JSON));
        SessaoCliente sessao = new SessaoCliente();

        ClienteAuthService.UsuarioLogado logado = servico(rt, sessao).login("eliseu", SENHA);

        servidor.verify();
        assertEquals(7L, logado.usuarioId());
        assertEquals("tok-1", sessao.getToken());
        assertEquals(cadastro.getChaveCifra(), sessao.getChaveCifragem());
    }

    @Test
    public void senhaErradaRecusaComSenhaIncorretaESemSessao() {
        CryptoResponse cadastro = crypto.gerarCredenciais(SENHA);
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(rt).build();
        servidor.expect(requestTo(SERVIDOR + "/salt/eliseu"))
                .andRespond(withSuccess("{\"salt\":\"" + cadastro.getSalt() + "\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(SERVIDOR + "/login"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"erro\":\"Usuário ou senha inválidos\"}"));
        SessaoCliente sessao = new SessaoCliente();

        LoginException erro = assertThrows(LoginException.class, () -> servico(rt, sessao).login("eliseu", "errada"));

        assertEquals("SENHA_INCORRETA", erro.getCodigo());
        assertNull(sessao.getToken());
    }

    @Test
    public void usuarioInexistenteRecusaComUsuarioNaoEncontrado() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(rt).build();
        servidor.expect(requestTo(SERVIDOR + "/salt/ninguem"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        LoginException erro = assertThrows(LoginException.class,
                () -> servico(rt, new SessaoCliente()).login("ninguem", SENHA));

        assertEquals("USUARIO_NAO_ENCONTRADO", erro.getCodigo());
    }

    @Test
    public void campoVazioRecusaSemFalarComOServidor() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(rt).build();

        LoginException erro = assertThrows(LoginException.class,
                () -> servico(rt, new SessaoCliente()).login(" ", SENHA));

        assertEquals("CAMPO_OBRIGATORIO_AUSENTE", erro.getCodigo());
        servidor.verify();
    }

    private ClienteAuthService servico(RestTemplate rt, SessaoCliente sessao) {
        return new ClienteAuthService(rt, crypto, sessao, SERVIDOR);
    }
}
