package cofre;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static cofre.ClienteService.mapa;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClienteServiceTrocaSenhaTest {

    @Test
    public void recifraAsCredenciaisComANovaSenhaEAtualizaASessao() {
        CryptoService crypto = new CryptoService();
        SessaoCliente sessao = new SessaoCliente();
        ClienteServidor servidor = mock(ClienteServidor.class);
        ClienteService cliente = new ClienteService(crypto, servidor, sessao, new ObjectMapper());

        CryptoResponse antiga = crypto.gerarCredenciais("antiga");
        String chaveAntiga = crypto.derivarChaveCifragem("antiga", antiga.getSalt());
        CifraResponse cifra = crypto.cifrar("segredo", chaveAntiga);
        sessao.iniciar("tok", chaveAntiga, 1L, "ana");
        when(servidor.buscarSalt("ana")).thenReturn(antiga.getSalt());
        when(servidor.listar("tok")).thenReturn(List.of(
                mapa("credencialId", 7L, "dadosCifrados", cifra.getDadosCifrados(), "iv", cifra.getIv())));

        cliente.alterarSenhaMestra("antiga", "nova");

        ArgumentCaptor<String> novoSalt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Map<String, Object>>> recifradas = ArgumentCaptor.forClass(List.class);
        verify(servidor).trocarSenhaMestra(eq("tok"), eq(crypto.derivarHashLogin("antiga", antiga.getSalt())),
                anyString(), novoSalt.capture(), recifradas.capture());
        Map<String, Object> item = recifradas.getValue().get(0);
        String chaveNova = crypto.derivarChaveCifragem("nova", novoSalt.getValue());

        assertEquals(7L, item.get("credencialId"));
        assertEquals("segredo", crypto.decifrar((String) item.get("dadosCifrados"), (String) item.get("iv"), chaveNova));
        assertEquals(chaveNova, sessao.getChaveCifragem());
    }
}
