//Lógica matemática
package br.puc.cofre.backend2.service;

import br.puc.cofre.backend2.dto.CryptoResponse;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    public CryptoResponse gerarCredenciais(String senhaMestra) {
        try {
            // 1. Gera um Salt aleatório
            byte[] saltBytes = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(saltBytes);
            String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);

            // 2. Deriva o Hash de Login (usando "auth" como contexto)
            String contextAuth = "auth" + saltBase64;
            String hashLogin = derivarChave(senhaMestra, contextAuth);

            // 3. Deriva a Chave de Cifra (usando "encrypt" como contexto)
            String contextEncrypt = "encrypt" + saltBase64;
            String chaveCifra = derivarChave(senhaMestra, contextEncrypt);

            return new CryptoResponse(saltBase64, hashLogin, chaveCifra);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao derivar chaves criptográficas", e);
        }
    }

    private String derivarChave(String senha, String saltComContexto) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), saltComContexto.getBytes(), ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}