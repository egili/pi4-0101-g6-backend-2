package cofre;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
public class ClienteControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ClienteService cliente;

    @MockBean
    private SessaoCliente sessao;

    @Test
    public void cadastroDevolve201() throws Exception {
        when(cliente.cadastrar("ana", "senha")).thenReturn(ClienteService.mapa("usuarioId", 1L, "mensagem", "ok"));

        mvc.perform(post("/api/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"senha\":\"senha\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(1));
    }

    @Test
    public void loginEDevolveUsuario() throws Exception {
        when(cliente.login("ana", "senha")).thenReturn(ClienteService.mapa("usuarioId", 1L, "username", "ana"));

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"senha\":\"senha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"));
    }

    @Test
    public void logoutEncerraASessao() throws Exception {
        mvc.perform(post("/api/logout")).andExpect(status().isOk());

        org.mockito.Mockito.verify(cliente).logout();
    }

    @Test
    public void sessaoNaoExpoeTokenNemChave() throws Exception {
        when(sessao.ativa()).thenReturn(true);
        when(sessao.getUsuarioId()).thenReturn(1L);
        when(sessao.getUsername()).thenReturn("ana");

        mvc.perform(get("/api/sessao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logado").value(true))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.chaveCifragem").doesNotExist());
    }

    @Test
    public void listarDevolveAsCredenciais() throws Exception {
        when(cliente.listar()).thenReturn(List.of(ClienteService.mapa("credencialId", 3L, "servico", "Gmail")));

        mvc.perform(get("/api/credenciais")).andExpect(status().isOk()).andExpect(jsonPath("$[0].servico").value("Gmail"));
    }

    @Test
    public void salvarDevolve201() throws Exception {
        when(cliente.salvar(anyString(), anyString(), anyString())).thenReturn(ClienteService.mapa("credencialId", 3L));

        mvc.perform(post("/api/credenciais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servico\":\"Gmail\",\"usuarioServico\":\"a@b.c\",\"senha\":\"x\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    public void revelarDevolveASenha() throws Exception {
        when(cliente.revelar("3")).thenReturn("segredo");

        mvc.perform(get("/api/credenciais/3/senha")).andExpect(status().isOk()).andExpect(jsonPath("$.senha").value("segredo"));
    }

    @Test
    public void editarEExcluirChamamOServico() throws Exception {
        when(cliente.editar(anyString(), anyString(), anyString(), anyString())).thenReturn(Map.of("credencialId", 3L));
        when(cliente.excluir("3")).thenReturn(Map.of("mensagem", "ok"));

        mvc.perform(put("/api/credenciais/3").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servico\":\"Gmail\",\"usuarioServico\":\"a@b.c\",\"senha\":\"x\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/credenciais/3")).andExpect(status().isOk());
    }

    @Test
    public void exportarEImportarPassamASenhaDoBackup() throws Exception {
        when(cliente.exportar("bk")).thenReturn(Map.of("versao", 1));
        when(cliente.importar(any(), anyString())).thenReturn(Map.of("credenciaisInseridas", 2));

        mvc.perform(post("/api/backup/exportar").contentType(MediaType.APPLICATION_JSON).content("{\"senhaBackup\":\"bk\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/backup/importar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vault\":{\"versao\":1},\"senhaBackup\":\"bk\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void codigosDeErroViramStatusHttpDoContrato() throws Exception {
        Map<String, Integer> esperado = new java.util.LinkedHashMap<>();
        esperado.put("SESSAO_INVALIDA", 401);
        esperado.put("SENHA_INCORRETA", 401);
        esperado.put("ACESSO_NEGADO", 403);
        esperado.put("CREDENCIAL_NAO_ENCONTRADA", 404);
        esperado.put("USUARIO_NAO_ENCONTRADO", 404);
        esperado.put("USUARIO_JA_EXISTE", 409);
        esperado.put("MUITAS_TENTATIVAS", 429);
        esperado.put("CAMPO_OBRIGATORIO_AUSENTE", 400);
        esperado.put("BACKUP_VAZIO", 400);
        esperado.put("FALHA_DECIFRAGEM", 400);
        esperado.put("ERRO_CONEXAO", 503);
        esperado.put("ERRO_BANCO", 500);
        for (Map.Entry<String, Integer> caso : esperado.entrySet()) {
            org.mockito.Mockito.doThrow(new ErroCliente(caso.getKey())).when(cliente).login(anyString(), anyString());
            mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"a\",\"senha\":\"b\"}"))
                    .andExpect(status().is(caso.getValue()))
                    .andExpect(jsonPath("$.erro").value(caso.getKey()));
        }
    }

    @Test
    public void trocaDeSenhaMestraPassaAsSenhasAoServico() throws Exception {
        when(cliente.alterarSenhaMestra("atual", "nova")).thenReturn(Map.of("mensagem", "ok"));

        mvc.perform(post("/api/senha-mestra").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"atual\",\"novaSenha\":\"nova\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void credenciaisDesatualizadasViramConflito() throws Exception {
        when(cliente.alterarSenhaMestra("a", "b")).thenThrow(new ErroCliente("CREDENCIAIS_DESATUALIZADAS"));

        mvc.perform(post("/api/senha-mestra").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"a\",\"novaSenha\":\"b\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("CREDENCIAIS_DESATUALIZADAS"));
    }
}
