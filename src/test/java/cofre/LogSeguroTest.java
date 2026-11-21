package cofre;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LogSeguroTest {

    @Test
    public void quebrasDeLinhaNaoChegamAoLog() {
        String limpo = LogSeguro.limpar("eliseu\nWARN falso: login ok");

        assertFalse(limpo.contains("\n"));
        assertFalse(limpo.contains("\r"));
        assertEquals("eliseu_WARN falso: login ok", limpo);
    }

    @Test
    public void caracteresDeControleSaoTrocados() {
        assertEquals("a_b_c", LogSeguro.limpar("a\tb\u0007c"));
    }

    @Test
    public void valorLongoEhCortado() {
        String limpo = LogSeguro.limpar("x".repeat(500));

        assertEquals(103, limpo.length());
        assertEquals("...", limpo.substring(100));
    }

    @Test
    public void nuloVira_null() {
        assertEquals("null", LogSeguro.limpar(null));
    }
}
