//Rota HTTP
package cofre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    @Autowired
    private CryptoService cryptoService;

    @PostMapping("/derivar")
    public CryptoResponse derivarCredenciais(@RequestBody CryptoRequest request) {
        return cryptoService.gerarCredenciais(request.getSenha());
    }

    @GetMapping("/generate")
    public Map<String, String> gerarSenha(@RequestParam int tamanho, @RequestParam boolean maiusculas,
                                          @RequestParam boolean minusculas, @RequestParam boolean numeros,
                                          @RequestParam boolean simbolos) {
        try {
            return Map.of("senha", cryptoService.gerarSenhaForte(tamanho, maiusculas, minusculas, numeros, simbolos));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}