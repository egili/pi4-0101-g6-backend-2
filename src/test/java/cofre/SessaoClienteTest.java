package cofre;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessaoClienteTest {

    @Test
    public void iniciarGuardaEEncerrarLimpa() {
        SessaoCliente sessao = new SessaoCliente();
        assertFalse(sessao.ativa());

        sessao.iniciar("tok", "chave", 7L, "ana");
        assertTrue(sessao.ativa());
        assertEquals("tok", sessao.getToken());
        assertEquals("chave", sessao.getChaveCifragem());
        assertEquals(7L, sessao.getUsuarioId());
        assertEquals("ana", sessao.getUsername());

        sessao.encerrar();
        assertFalse(sessao.ativa());
        assertNull(sessao.getToken());
        assertNull(sessao.getChaveCifragem());
        assertNull(sessao.getUsuarioId());
        assertNull(sessao.getUsername());
    }

    @Test
    public void cifraResponseExpoeOsDados() {
        CifraResponse cifra = new CifraResponse("DADOS", "IV");
        assertEquals("DADOS", cifra.getDadosCifrados());
        assertEquals("IV", cifra.getIv());
        assertEquals("DADOS", cifra.dadosCifrados());
        assertEquals("IV", cifra.iv());

        CifraResponse vazia = new CifraResponse();
        vazia.setDadosCifrados("X");
        vazia.setIv("Y");
        assertEquals("X", vazia.getDadosCifrados());
        assertEquals("Y", vazia.getIv());
    }
}
