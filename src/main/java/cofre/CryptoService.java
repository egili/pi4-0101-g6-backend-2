//Lógica matemática
package cofre;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class CryptoService {

    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String MAIUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMEROS = "0123456789";
    private static final String SIMBOLOS = "!@#$%&*()-_=+[]{};:,.?";

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

    public String gerarSenhaForte(int tamanho, boolean maiusculas, boolean minusculas, boolean numeros, boolean simbolos) {
        List<String> grupos = new ArrayList<>();
        if (maiusculas) grupos.add(MAIUSCULAS);
        if (minusculas) grupos.add(MINUSCULAS);
        if (numeros) grupos.add(NUMEROS);
        if (simbolos) grupos.add(SIMBOLOS);
        if (grupos.isEmpty() || tamanho < grupos.size()) {
            throw new IllegalArgumentException("Escolha ao menos um tipo de caractere e um tamanho que caiba um caractere de cada tipo");
        }

        // Um caractere de cada tipo escolhido, o resto do conjunto inteiro, depois embaralha
        SecureRandom random = new SecureRandom();
        String todos = String.join("", grupos);
        List<Character> senha = new ArrayList<>();
        for (String grupo : grupos) senha.add(grupo.charAt(random.nextInt(grupo.length())));
        while (senha.size() < tamanho) senha.add(todos.charAt(random.nextInt(todos.length())));
        Collections.shuffle(senha, random);

        StringBuilder resultado = new StringBuilder(tamanho);
        senha.forEach(resultado::append);
        return resultado.toString();
    }

    private String derivarChave(String senha, String saltComContexto) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), saltComContexto.getBytes(), ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}