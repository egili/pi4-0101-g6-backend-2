//Rota HTTP
package br.puc.cofre.backend2.controller;

import br.puc.cofre.backend2.dto.CryptoRequest;
import br.puc.cofre.backend2.dto.CryptoResponse;
import br.puc.cofre.backend2.service.CryptoService;
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