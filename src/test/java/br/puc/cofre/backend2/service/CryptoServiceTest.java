//Teste JUnit hash e chave diferente
package br.puc.cofre.backend2.service;

import br.puc.cofre.backend2.dto.CryptoResponse;
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
}