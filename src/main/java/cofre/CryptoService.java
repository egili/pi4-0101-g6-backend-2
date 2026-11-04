//Lógica matemática
package cofre;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 12 bytes conforme padrão GCM e requisito H05.1
    private static final int GCM_TAG_LENGTH = 128; // 128 bits de autenticação

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

    /**
     * Cifra texto plano utilizando AES/GCM/NoPadding.
     * Gera obrigatoriamente um IV novo, aleatório e exclusivo (12 bytes via SecureRandom) a cada cifragem.
     *
     * @param textoPlano  Texto em claro a ser cifrado
     * @param chaveBase64 Chave simétrica AES em formato Base64
     * @return CifraResponse contendo os dados cifrados e o IV (ambos em Base64)
     */
    public CifraResponse cifrar(String textoPlano, String chaveBase64) {
        try {
            // Gera obrigatoriamente um IV novo, aleatório e exclusivo (12 bytes via SecureRandom) a cada cifragem
            byte[] ivBytes = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(ivBytes);

            byte[] chaveBytes = Base64.getDecoder().decode(chaveBase64);
            SecretKeySpec secretKey = new SecretKeySpec(chaveBytes, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] dadosCifrados = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            String dadosCifradosBase64 = Base64.getEncoder().encodeToString(dadosCifrados);
            String ivBase64 = Base64.getEncoder().encodeToString(ivBytes);

            return new CifraResponse(dadosCifradosBase64, ivBase64);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao cifrar dados com AES/GCM", e);
        }
    }

    /**
     * Decifra dados cifrados utilizando AES/GCM/NoPadding e o IV correspondente.
     *
     * @param dadosCifradosBase64 Dados cifrados codificados em Base64
     * @param ivBase64           IV de 12 bytes codificado em Base64
     * @param chaveBase64        Chave simétrica AES em formato Base64
     * @return Texto plano decifrado em UTF-8
     */
    public String decifrar(String dadosCifradosBase64, String ivBase64, String chaveBase64) {
        try {
            byte[] dadosCifradosBytes = Base64.getDecoder().decode(dadosCifradosBase64);
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
            byte[] chaveBytes = Base64.getDecoder().decode(chaveBase64);

            SecretKeySpec secretKey = new SecretKeySpec(chaveBytes, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] textoPlanoBytes = cipher.doFinal(dadosCifradosBytes);
            return new String(textoPlanoBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao decifrar dados com AES/GCM", e);
        }
    }
}
