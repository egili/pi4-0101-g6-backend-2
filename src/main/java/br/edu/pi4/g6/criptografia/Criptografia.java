package br.edu.pi4.g6.criptografia;

import java.security.SecureRandom;
import java.util.Base64;

public final class Criptografia {

    private static final int TAMANHO_SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Criptografia() {
    }

    public static String gerarSalt() {
        byte[] salt = new byte[TAMANHO_SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
