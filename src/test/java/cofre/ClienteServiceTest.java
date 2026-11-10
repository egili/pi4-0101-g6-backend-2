package cofre;

import cofre.protocolo.CredencialDto;
import cofre.protocolo.PedidoDeCriacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClienteServiceTest {

    private static final String SENHA = "MinhaSenha!123";

    private final CryptoService crypto = new CryptoService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final SessaoCliente sessao = new SessaoCliente();
    private ServidorFalso servidor;
    private ClienteServidor conexao;
    private ClienteService cliente;
    private CryptoResponse cadastro;

    @BeforeEach
    public void sobe() throws Exception {
        servidor = new ServidorFalso();
        conexao = new ClienteServidor("localhost", servidor.porta());
        cliente = new ClienteService(crypto, conexao, sessao, mapper);
        cadastro = crypto.gerarCredenciais(SENHA);
        servidor.cadastrar("eliseu", cadastro.getSalt(), cadastro.getHashLogin());
    }

    @AfterEach
    public void desce() throws Exception {
        conexao.encerrar();
        servidor.close();
    }

    @Test
    public void loginGuardaTokenEChaveSoNaSessao() {
        Map<String, Object> resposta = cliente.login("eliseu", SENHA);

        assertEquals(7L, resposta.get("usuarioId"));
        assertFalse(resposta.containsKey("token"));
        assertFalse(resposta.containsKey("chaveCifra"));
        assertEquals(cadastro.getChaveCifra(), sessao.getChaveCifragem());
    }

    @Test
    public void loginComSenhaErradaNaoAbreSessao() {
        ErroCliente erro = assertThrows(ErroCliente.class, () -> cliente.login("eliseu", "errada"));

        assertEquals("SENHA_INCORRETA", erro.getCodigo());
        assertFalse(sessao.ativa());
    }

    @Test
    public void salvarEnviaSoBlobCifradoEVoltaComOIdParaRevelar() {
        cliente.login("eliseu", SENHA);

        Map<String, Object> salvo = cliente.salvar("Gmail", "eliseu@gmail.com", "senha-gmail");

        PedidoDeCriacao enviado = (PedidoDeCriacao) servidor.recebidos.stream()
                .filter(c -> c instanceof PedidoDeCriacao).findFirst().orElseThrow();
        assertFalse(enviado.getDadosCifrados().contains("senha-gmail"), "texto claro não pode ir ao Servidor");
        assertEquals("senha-gmail", crypto.decifrar(enviado.getDadosCifrados(), enviado.getIv(), cadastro.getChaveCifra()));
        assertEquals("senha-gmail", cliente.revelar(String.valueOf(salvo.get("credencialId"))));
    }

    @Test
    public void exportarGeraVaultSemTextoClaroEImportarRecifra() {
        cliente.login("eliseu", SENHA);
        cliente.salvar("Gmail", "eliseu@gmail.com", "senha-gmail");

        Map<String, Object> vault = cliente.exportar("senha-do-backup");

        assertEquals(1, vault.get("versao"));
        assertFalse(mapper.valueToTree(vault).toString().contains("senha-gmail"), "o .vault não pode ter texto claro");
        Map<String, Object> resultado = cliente.importar(vault, "senha-do-backup");
        assertEquals(1, resultado.get("credenciaisInseridas"));
        assertEquals(2, servidor.cofre().size());
    }

    @Test
    public void importarComSenhaDoBackupErradaNaoEnviaNada() {
        cliente.login("eliseu", SENHA);
        cliente.salvar("Gmail", "eliseu@gmail.com", "senha-gmail");
        Map<String, Object> vault = cliente.exportar("senha-do-backup");
        int antes = servidor.cofre().size();

        ErroCliente erro = assertThrows(ErroCliente.class, () -> cliente.importar(vault, "outra-senha"));

        assertEquals("SENHA_INCORRETA", erro.getCodigo());
        assertEquals(antes, servidor.cofre().size());
    }

    @Test
    public void semSessaoNenhumPedidoDeCredencialSai() {
        ErroCliente erro = assertThrows(ErroCliente.class, () -> cliente.listar());

        assertEquals("SESSAO_INVALIDA", erro.getCodigo());
        assertTrue(servidor.recebidos.stream().noneMatch(c -> c instanceof cofre.protocolo.PedidoDeListagem));
    }
}
