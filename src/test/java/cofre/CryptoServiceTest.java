//Testes JUnit do Módulo de Criptografia (derivação PBKDF2 e cifragem AES/GCM)
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
    public void deveCifrarEDecifrarComSucessoIdaEVolta() {
        String senhaMestra = "MinhaSenhaSuperSecreta123!";
        CryptoResponse credenciais = cryptoService.gerarCredenciais(senhaMestra);
        String chave = credenciais.getChaveCifra();
        String textoOriginal = "MinhaSenhaDoServico123!@#";

        // Cifra o texto plano
        CifraResponse resultado = cryptoService.cifrar(textoOriginal, chave);

        assertNotNull(resultado, "O resultado da cifragem não pode ser nulo");
        assertNotNull(resultado.getDadosCifrados(), "Os dados cifrados não podem ser nulos");
        assertNotNull(resultado.getIv(), "O IV não pode ser nulo");
        assertNotEquals(textoOriginal, resultado.getDadosCifrados(), "Os dados cifrados não podem ser iguais ao texto plano");

        // Decifra com a mesma chave e IV
        String textoDecifrado = cryptoService.decifrar(resultado.getDadosCifrados(), resultado.getIv(), chave);

        assertEquals(textoOriginal, textoDecifrado, "O texto decifrado deve ser exatamente igual ao texto original");
        System.out.println("Teste passou! Cifragem e decifração de ida e volta validadas com sucesso.");
    }

    @Test
    public void deveGerarIVsDiferentesParaCifragensConsecutivas() {
        String senhaMestra = "MinhaSenhaSuperSecreta123!";
        CryptoResponse credenciais = cryptoService.gerarCredenciais(senhaMestra);
        String chave = credenciais.getChaveCifra();
        String textoOriginal = "TextoParaValidacaoDeIVUnico";

        // Duas cifragens consecutivas do mesmo texto com a mesma chave
        CifraResponse resultado1 = cryptoService.cifrar(textoOriginal, chave);
        CifraResponse resultado2 = cryptoService.cifrar(textoOriginal, chave);

        assertNotNull(resultado1.getIv(), "O IV 1 não pode ser nulo");
        assertNotNull(resultado2.getIv(), "O IV 2 não pode ser nulo");

        // A PROVA: IVs devem ser diferentes e exclusivos
        assertNotEquals(resultado1.getIv(), resultado2.getIv(), 
            "FALHA CRÍTICA: Duas cifragens consecutivas geraram o mesmo IV!");

        // Como o IV é diferente, o texto cifrado resultante também deve ser diferente
        assertNotEquals(resultado1.getDadosCifrados(), resultado2.getDadosCifrados(),
            "Os dados cifrados devem ser distintos para IVs distintos");
            
        System.out.println("Teste passou! IVs exclusivos gerados a cada cifragem.");
    }

    @Test
    public void deveLancarExcecaoAoDecifrarComChaveErrada() {
        String senhaMestraCorreta = "SenhaCorreta123!";
        String senhaMestraErrada = "SenhaErrada999!";

        CryptoResponse credenciaisCorretas = cryptoService.gerarCredenciais(senhaMestraCorreta);
        CryptoResponse credenciaisErradas = cryptoService.gerarCredenciais(senhaMestraErrada);

        String chaveCorreta = credenciaisCorretas.getChaveCifra();
        String chaveErrada = credenciaisErradas.getChaveCifra();
        String textoOriginal = "DadoAltamenteConfidencial";

        // Cifra com a chave correta
        CifraResponse resultado = cryptoService.cifrar(textoOriginal, chaveCorreta);

        // A PROVA: Decifrar com chave errada deve falhar e lançar exceção
        assertThrows(RuntimeException.class, () -> {
            cryptoService.decifrar(resultado.getDadosCifrados(), resultado.getIv(), chaveErrada);
        }, "Tentar decifrar com chave incorreta deve lançar uma exceção de integridade/segurança!");

        System.out.println("Teste passou! Exceção lançada corretamente ao decifrar com chave incorreta.");
    }
}
