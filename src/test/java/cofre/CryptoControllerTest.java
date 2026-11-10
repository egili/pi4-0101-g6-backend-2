//Testes JUnit da rota de geração de senha forte
package cofre;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CryptoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CryptoService cryptoService;

    @Test
    public void geraSenhaComTamanhoEOsTiposEscolhidos() throws Exception {
        mvc.perform(get("/api/crypto/generate")
                .param("tamanho", "16").param("maiusculas", "true").param("minusculas", "true")
                .param("numeros", "true").param("simbolos", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.senha").value(matchesPattern("[A-Za-z0-9]{16}")));
    }

    @Test
    public void recusaOpcoesInvalidasComBadRequest() throws Exception {
        mvc.perform(get("/api/crypto/generate")
                .param("tamanho", "10").param("maiusculas", "false").param("minusculas", "false")
                .param("numeros", "false").param("simbolos", "false"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void rotasQueDevolviamOuRecebiamChaveForamRemovidas() throws Exception {
        mvc.perform(post("/api/crypto/decifrar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/crypto/derivar").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"x\"}"))
                .andExpect(status().isNotFound());
    }
}
