package cofre;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class ClienteServiceTest {

    private static final String SERVIDOR = "http://servidor";
    private static final String SENHA = "MinhaSenha!123";
    private static final String TOKEN = "tok-1";

    private final CryptoService crypto = new CryptoService();
    private final ObjectMapper mapper = new ObjectMapper();

    private final RestTemplate rt = new RestTemplate();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(rt).build();
    private final SessaoCliente sessao = new SessaoCliente();
    private final ClienteService cliente = new ClienteService(crypto,
            new ClienteServidor(rt, SERVIDOR), sessao, mapper);

    @Test
    public void loginGuardaTokenEChaveSoNaSessao() {
        CryptoResponse cadastro = crypto.gerarCredenciais(SENHA);
        esperarSalt("eliseu", cadastro.getSalt());
        esperarLogin(cadastro.getHashLogin());

        Map<String, Object> resposta = cliente.login("eliseu", SENHA);

        servidor.verify();
        assertEquals(7L, resposta.get("usuarioId"));
        assertFalse(resposta.containsKey("token"));
        assertFalse(resposta.containsKey("chaveCifra"));
        assertEquals(cadastro.getChaveCifra(), sessao.getChaveCifragem());
        assertEquals(TOKEN, sessao.getToken());
    }

    @Test
    public void salvarEnviaSoBlobCifradoComAChaveDaSessao() throws Exception {
        CryptoResponse cadastro = crypto.gerarCredenciais(SENHA);
        logar(cadastro);

        AtomicReference<String> corpoEnviado = new AtomicReference<>();
        servidor.expect(requestTo(SERVIDOR + "/credentials")).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(req -> corpoEnviado.set(((MockClientHttpRequest) req).getBodyAsString()))
                .andRespond(withSuccess("{\"credencialId\":1,\"mensagem\":\"ok\"}", MediaType.APPLICATION_JSON));

        cliente.salvar("Gmail", "eliseu@gmail.com", "senha-gmail");

        servidor.verify();
        Map<String, Object> enviado = mapper.readValue(corpoEnviado.get(), Map.class);
        assertFalse(corpoEnviado.get().contains("senha-gmail"), "texto claro não pode ir ao Servidor");
        assertEquals("senha-gmail", crypto.decifrar((String) enviado.get("dadosCifrados"), (String) enviado.get("iv"), cadastro.getChaveCifra()));
    }

    @Test
    public void revelarDecifraComAChaveDaSessao() throws Exception {
        CryptoResponse cadastro = crypto.gerarCredenciais(SENHA);
        logar(cadastro);
        CifraResponse cifra = crypto.cifrar("senha-gmail", cadastro.getChaveCifra());
        esperarListagem(1, cifra);

        assertEquals("senha-gmail", cliente.revelar("1"));
    }

    @Test
    public void exportarGeraVaultSemTextoClaroEImportarRecifraParaASessaoAtual() throws Exception {
        CryptoResponse cadastro = crypto.gerarCredenciais(SENHA);
        logar(cadastro);
        CifraResponse cifra = crypto.cifrar("senha-gmail", cadastro.getChaveCifra());

        servidor.expect(requestTo(SERVIDOR + "/backup")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"credenciais\":[{\"credencialId\":1,\"servico\":\"Gmail\",\"usuarioServico\":\"a@b.c\","
                                + "\"dadosCifrados\":\"" + cifra.getDadosCifrados() + "\",\"iv\":\"" + cifra.getIv() + "\"}]}",
                        MediaType.APPLICATION_JSON));

        AtomicReference<String> corpoImportado = new AtomicReference<>();
        servidor.expect(requestTo(SERVIDOR + "/backup")).andExpect(method(HttpMethod.POST))
                .andExpect(req -> corpoImportado.set(((MockClientHttpRequest) req).getBodyAsString()))
                .andRespond(withSuccess("{\"credenciaisInseridas\":1,\"mensagem\":\"ok\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> vault = cliente.exportar("senha-do-backup");

        Map<String, Object> resultado = cliente.importar(vault, "senha-do-backup");

        servidor.verify();
        assertEquals(1, resultado.get("credenciaisInseridas"));

        assertEquals(1, vault.get("versao"));
        assertEquals("AES-256-GCM", vault.get("algoritmo"));
        assertFalse(mapper.writeValueAsString(vault).contains("senha-gmail"), "o .vault não pode ter texto claro");

        List<Map<String, Object>> enviadas = mapper.readValue(corpoImportado.get(), List.class);
        assertEquals("senha-gmail", crypto.decifrar((String) enviadas.get(0).get("dadosCifrados"),
                (String) enviadas.get(0).get("iv"), cadastro.getChaveCifra()));
    }

    @Test
    public void importarComSenhaDoBackupErradaRecusaSemEnviarNada() throws Exception {
        CryptoResponse cadastro = crypto.gerarCredenciais(SENHA);
        logar(cadastro);
        CifraResponse cifra = crypto.cifrar("senha-gmail", cadastro.getChaveCifra());
        servidor.expect(requestTo(SERVIDOR + "/backup")).andRespond(withSuccess(
                "{\"credenciais\":[{\"servico\":\"Gmail\",\"usuarioServico\":\"a@b.c\",\"dadosCifrados\":\""
                        + cifra.getDadosCifrados() + "\",\"iv\":\"" + cifra.getIv() + "\"}]}", MediaType.APPLICATION_JSON));
        Map<String, Object> vault = cliente.exportar("senha-do-backup");

        ErroCliente erro = assertThrows(ErroCliente.class, () -> cliente.importar(vault, "outra-senha"));

        assertEquals("SENHA_INCORRETA", erro.getCodigo());
    }

    @Test
    public void semSessaoNenhumaOperacaoDeCredencialRodaNoServidor() {
        ErroCliente erro = assertThrows(ErroCliente.class, () -> cliente.listar());

        assertEquals("SESSAO_INVALIDA", erro.getCodigo());
        servidor.verify();
    }

    private void logar(CryptoResponse cadastro) {
        esperarSalt("eliseu", cadastro.getSalt());
        esperarLogin(cadastro.getHashLogin());
        cliente.login("eliseu", SENHA);
        servidor.reset();
    }

    private void esperarSalt(String usuario, String salt) {
        servidor.expect(requestTo(SERVIDOR + "/salt/" + usuario)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"salt\":\"" + salt + "\"}", MediaType.APPLICATION_JSON));
    }

    private void esperarLogin(String hashLogin) {
        servidor.expect(requestTo(SERVIDOR + "/login")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"token\":\"" + TOKEN + "\",\"usuarioId\":7}", MediaType.APPLICATION_JSON));
    }

    private void esperarListagem(long id, CifraResponse cifra) {
        servidor.expect(requestTo(SERVIDOR + "/credentials")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"credenciais\":[{\"credencialId\":" + id + ",\"servico\":\"Gmail\",\"usuarioServico\":\"a@b.c\","
                        + "\"dadosCifrados\":\"" + cifra.getDadosCifrados() + "\",\"iv\":\"" + cifra.getIv() + "\"}]}", MediaType.APPLICATION_JSON));
    }
}
