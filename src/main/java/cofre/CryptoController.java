//Rota HTTP
package cofre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    @Autowired
    private CryptoService cryptoService;

    @PostMapping("/derivar")
    public CryptoResponse derivarCredenciais(@RequestBody CryptoRequest request) {
        return cryptoService.gerarCredenciais(request.getSenha());
    }
}