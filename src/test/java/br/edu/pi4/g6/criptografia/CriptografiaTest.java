package br.edu.pi4.g6.criptografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class CriptografiaTest {

    @Test
    void gerarSaltRetorna16BytesEmBase64() {
        byte[] salt = Base64.getDecoder().decode(Criptografia.gerarSalt());
        assertEquals(16, salt.length);
    }

    @Test
    void gerarSaltNaoRepete() {
        assertNotEquals(Criptografia.gerarSalt(), Criptografia.gerarSalt());
    }
}
