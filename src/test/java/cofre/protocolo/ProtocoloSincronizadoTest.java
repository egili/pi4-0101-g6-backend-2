package cofre.protocolo;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O pacote cofre.protocolo é copiado no Servidor (backend-1). Este teste falha se as duas cópias divergirem. */
public class ProtocoloSincronizadoTest {

    @Test
    public void protocoloIgualAoDoServidor() throws Exception {
        List<Path> arquivos;
        try (Stream<Path> s = Files.list(Path.of("src/main/java/cofre/protocolo"))) {
            arquivos = s.filter(p -> p.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (Path p : arquivos) {
            md.update(p.getFileName().toString().getBytes(StandardCharsets.UTF_8));
            md.update(Files.readAllBytes(p));
        }
        String atual = HexFormat.of().formatHex(md.digest());
        String esperado = Files.readString(Path.of("src/test/resources/protocolo.sha256")).trim();
        assertEquals(esperado, atual,
                "cofre.protocolo mudou: aplique a mesma mudança no Servidor (backend-1) e atualize protocolo.sha256 nos dois");
    }
}
