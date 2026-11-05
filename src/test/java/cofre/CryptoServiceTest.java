//Teste JUnit hash e chave diferente
package cofre;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CryptoServiceTest {

    @Autowired
    private CryptoService cryptoService;

    @Test
    public void deveGerarHashEChaveDiferentes() {
        String senhaMestra = "MinhaSenhaSuperSecreta123!";

        // Gera as credenciais
        CryptoResponse resposta = cryptoService.gerarCredenciais(senhaMestra);

        // Verifica se tudo foi gerado
        assertNotNull(resposta.getSalt(), "O salt não pode ser nulo");
        assertNotNull(resposta.getHashLogin(), "O hash de login não pode ser nulo");
        assertNotNull(resposta.getChaveCifra(), "A chave de cifra não pode ser nula");

        // A PROVA: O hash e a chave devem ser DIFERENTES
        assertNotEquals(resposta.getHashLogin(), resposta.getChaveCifra(), 
            "FALHA CRÍTICA: O hash de login e a chave de cifra são iguais!");
            
        System.out.println("Teste passou! O hash e a chave são diferentes.");
    }

    @Test
    public void geraSenhaComTamanhoEOsTiposEscolhidos() {
        String senha = cryptoService.gerarSenhaForte(20, false, true, true, false);

        assertEquals(20, senha.length());
        assertTrue(senha.matches("[a-z0-9]{20}"), "A senha deve ter só minúsculas e números");
    }

    @Test
    public void senhaTemUmCaractereDeCadaTipoEscolhido() {
        for (int i = 0; i < 100; i++) {
            String senha = cryptoService.gerarSenhaForte(4, true, true, true, true);

            assertTrue(senha.matches(".*[A-Z].*"), "Falta maiúscula: " + senha);
            assertTrue(senha.matches(".*[a-z].*"), "Falta minúscula: " + senha);
            assertTrue(senha.matches(".*[0-9].*"), "Falta número: " + senha);
            assertTrue(senha.matches(".*[!@#$%&*()\\-_=+\\[\\]{};:,.?].*"), "Falta símbolo: " + senha);
        }
    }

    @Test
    public void recusaOpcoesInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> cryptoService.gerarSenhaForte(10, false, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> cryptoService.gerarSenhaForte(2, true, true, true, false));
    }
}