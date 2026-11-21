package cofre.protocolo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocoloDtosTest {

    @Test
    public void pedidosDeCredencialGuardamOsDados() {
        PedidoDeCriacao criacao = new PedidoDeCriacao("tok", "Gmail", "a@b.c", "DADOS", "IV");
        assertEquals("tok", criacao.getToken());
        assertEquals("Gmail", criacao.getServico());
        assertEquals("a@b.c", criacao.getUsuarioServico());
        assertEquals("DADOS", criacao.getDadosCifrados());
        assertEquals("IV", criacao.getIv());

        PedidoDeAtualizacao atualizacao = new PedidoDeAtualizacao("tok", 9L, "Gmail", "a@b.c", "DADOS", "IV");
        assertEquals(9L, atualizacao.getCredencialId());
        assertEquals("DADOS", atualizacao.getDadosCifrados());
        assertEquals("IV", atualizacao.getIv());
        assertEquals("tok", atualizacao.getToken());
        assertEquals("Gmail", atualizacao.getServico());
        assertEquals("a@b.c", atualizacao.getUsuarioServico());
    }

    @Test
    public void pedidosDeAutenticacaoEConsultaGuardamOsDados() {
        assertEquals("ana", new PedidoDeSalt("ana").getUsername());
        PedidoDeCadastro cadastro = new PedidoDeCadastro("ana", "hash", "salt");
        assertEquals("hash", cadastro.getSenhaMestraHash());
        assertEquals("salt", cadastro.getSalt());
        assertEquals("ana", cadastro.getUsername());
        PedidoDeLogin login = new PedidoDeLogin("ana", "hash");
        assertEquals("hash", login.getHashLogin());
        assertEquals("ana", login.getUsername());
        assertEquals("tok", new PedidoDeListagem("tok").getToken());
        assertEquals("tok", new PedidoDeExportacao("tok").getToken());
        assertEquals(3L, new PedidoDeExclusao("tok", 3L).getCredencialId());
        assertEquals("tok", new PedidoDeExclusao("tok", 3L).getToken());
    }

    @Test
    public void importacaoEResultadoGuardamLista() {
        CredencialDto c = new CredencialDto(1L, "Gmail", "a@b.c", "DADOS", "IV");
        assertEquals(1L, c.getCredencialId());
        assertEquals("Gmail", c.getServico());
        assertEquals("a@b.c", c.getUsuarioServico());
        assertEquals("DADOS", c.getDadosCifrados());
        assertEquals("IV", c.getIv());
        assertEquals("tok", new PedidoDeImportacao("tok", List.of(c)).getToken());
        assertEquals(1, new PedidoDeImportacao("tok", List.of(c)).getCredenciais().size());

        Resposta ok = new Resposta(null, Map.of("salt", "s"));
        assertTrue(ok.ok());
        assertEquals("s", ok.getDados().get("salt"));
        Resposta erro = new Resposta("SESSAO_INVALIDA", null);
        assertFalse(erro.ok());
        assertEquals("SESSAO_INVALIDA", erro.getErro());
        assertNull(erro.getDados());
        assertNotNull(new ComunicadoDeDesligamento());
        assertNotNull(new PedidoParaSair());
    }
}
