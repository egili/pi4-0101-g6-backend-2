package cofre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        String senha = request.getSenha();
        String salt = request.getSalt();

        // Se o salt foi enviado, é LOGIN
        if (salt != null && !salt.isEmpty()) {
            String hashLogin = cryptoService.derivarHashLogin(senha, salt);
            String chaveCifra = cryptoService.derivarChaveCifragem(senha, salt);
            return new CryptoResponse(salt, hashLogin, chaveCifra);
        } 
        
        // Se não tem salt, é CADASTRO
        return cryptoService.gerarCredenciais(senha);
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

    @PostMapping("/decifrar")
    public ResponseEntity<?> decifrar(@RequestBody(required = false) DecifrarRequest request) {
        if (request == null || request.isInvalido()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "DADOS_INVALIDOS"));
        }

        try {
            String senha = cryptoService.decifrar(
                    request.getDadosCifrados(),
                    request.getIv(),
                    request.getChaveCifra());
            if (senha == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("erro", "FALHA_DECIFRAGEM"));
            }
            return ResponseEntity.ok(Map.of("senha", senha));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "FALHA_DECIFRAGEM"));
        }
    }
}