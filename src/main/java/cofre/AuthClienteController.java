package cofre;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthClienteController {

    private final ClienteAuthService authService;

    public AuthClienteController(ClienteAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) Map<String, String> body) {
        try {
            ClienteAuthService.UsuarioLogado usuario = authService.login(
                    body == null ? null : body.get("username"),
                    body == null ? null : body.get("senha"));
            return ResponseEntity.ok(Map.of(
                    "usuarioId", usuario.usuarioId(),
                    "username", usuario.username(),
                    "mensagem", "Login realizado com sucesso"
            ));
        } catch (LoginException e) {
            return ResponseEntity.status(statusDe(e.getCodigo())).body(Map.of("erro", e.getCodigo()));
        }
    }

    private HttpStatus statusDe(String codigo) {
        return switch (codigo) {
            case "CAMPO_OBRIGATORIO_AUSENTE" -> HttpStatus.BAD_REQUEST;
            case "SENHA_INCORRETA" -> HttpStatus.UNAUTHORIZED;
            case "USUARIO_NAO_ENCONTRADO" -> HttpStatus.NOT_FOUND;
            case "ERRO_CONEXAO" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
