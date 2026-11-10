package cofre;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cofre.ClienteService.mapa;

/** API que o navegador usa. Nenhuma rota devolve chave de cifragem ou token. */
@RestController
@RequestMapping("/api")
public class ClienteController {

    private final ClienteService cliente;
    private final SessaoCliente sessao;

    public ClienteController(ClienteService cliente, SessaoCliente sessao) {
        this.cliente = cliente;
        this.sessao = sessao;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastro(@RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cliente.cadastrar(campo(body, "username"), campo(body, "senha")));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody(required = false) Map<String, String> body) {
        return cliente.login(campo(body, "username"), campo(body, "senha"));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        cliente.logout();
        return mapa("mensagem", "Sessão encerrada");
    }

    @GetMapping("/sessao")
    public Map<String, Object> sessao() {
        return mapa("logado", sessao.ativa(), "usuarioId", sessao.getUsuarioId(), "username", sessao.getUsername());
    }

    @GetMapping("/credenciais")
    public List<Map<String, Object>> listar() {
        return cliente.listar();
    }

    @PostMapping("/credenciais")
    public ResponseEntity<?> salvar(@RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                cliente.salvar(campo(body, "servico"), campo(body, "usuarioServico"), campo(body, "senha")));
    }

    @GetMapping("/credenciais/{id}/senha")
    public Map<String, Object> revelar(@PathVariable String id) {
        return mapa("senha", cliente.revelar(id));
    }

    @PutMapping("/credenciais/{id}")
    public Map<String, Object> editar(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        return cliente.editar(id, campo(body, "servico"), campo(body, "usuarioServico"), campo(body, "senha"));
    }

    @DeleteMapping("/credenciais/{id}")
    public Map<String, Object> excluir(@PathVariable String id) {
        return cliente.excluir(id);
    }

    @PostMapping("/backup/exportar")
    public Map<String, Object> exportar(@RequestBody(required = false) Map<String, String> body) {
        return cliente.exportar(campo(body, "senhaBackup"));
    }

    @PostMapping("/backup/importar")
    public Map<String, Object> importar(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> vault = body == null || !(body.get("vault") instanceof Map<?, ?>) ? null : castMapa(body.get("vault"));
        String senhaBackup = body == null || body.get("senhaBackup") == null ? null : String.valueOf(body.get("senhaBackup"));
        return cliente.importar(vault, senhaBackup);
    }

    @ExceptionHandler(ErroCliente.class)
    public ResponseEntity<Map<String, Object>> erro(ErroCliente e) {
        return ResponseEntity.status(statusDe(e.getCodigo())).body(mapa("erro", e.getCodigo()));
    }

    private static HttpStatus statusDe(String codigo) {
        return switch (codigo) {
            case "SESSAO_INVALIDA", "SENHA_INCORRETA" -> HttpStatus.UNAUTHORIZED;
            case "ACESSO_NEGADO" -> HttpStatus.FORBIDDEN;
            case "CREDENCIAL_NAO_ENCONTRADA", "USUARIO_NAO_ENCONTRADO" -> HttpStatus.NOT_FOUND;
            case "USUARIO_JA_EXISTE" -> HttpStatus.CONFLICT;
            case "MUITAS_TENTATIVAS" -> HttpStatus.TOO_MANY_REQUESTS;
            case "CAMPO_OBRIGATORIO_AUSENTE", "DADOS_INVALIDOS", "BACKUP_VAZIO", "FALHA_DECIFRAGEM" -> HttpStatus.BAD_REQUEST;
            case "ERRO_CONEXAO" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static String campo(Map<String, String> body, String nome) {
        return body == null ? null : body.get(nome);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMapa(Object valor) {
        return (Map<String, Object>) valor;
    }
}
