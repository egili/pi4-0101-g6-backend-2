# Sistema de Cofre de Senhas Criptografadas - Cliente (Back-end 2)

Sistema cliente-servidor desenvolvido em Java para o gerenciamento, armazenamento seguro e cifragem de credenciais pessoais, garantindo que o servidor central não tenha acesso aos dados em texto claro.

Este módulo é o **Cliente**. Roda na máquina do usuário e faz todo o trabalho criptográfico: deriva as chaves, cifra e decifra as credenciais e monta o arquivo de backup `.vault`. A chave de cifragem e o token da sessão ficam apenas na memória deste processo. O navegador recebe texto para mostrar, nunca chave nem token.

Ele fala com dois lados:

- **Com o navegador (front-end):** API REST HTTP na porta **8081**.
- **Com o Servidor (Back-end 1):** socket TCP na porta **8090**, com objetos Serializable do pacote `cofre.protocolo`.

## Documentação

- **Visão geral, módulos e convenção de nomes:** [docs/fluxos/VisaoGeral.md](https://github.com/egili/pi4-0101-g6-frontend/blob/main/docs/fluxos/VisaoGeral.md)
- **Fluxos por entregável (E1 a E9):** [docs/fluxos/](https://github.com/egili/pi4-0101-g6-frontend/tree/main/docs/fluxos)
- **Contratos do sistema:** no repositório do [Back-end 1](https://github.com/egili/pi4-0101-g6-backend-1/tree/main/docs/contratos)
- **Decisões de arquitetura (ADRs):** no repositório [principal](https://github.com/egili/pi4-2026-2-0101-grupo06/tree/main/docs/adr)
- **Explicação dos logs:** [docs/logs.md](https://github.com/egili/pi4-2026-2-0101-grupo06/blob/main/docs/logs.md)

## Rotas da API (navegador → Cliente)

| Método e rota | O que faz |
|---|---|
| `POST /api/cadastro` | Cria uma conta (`username`, `senha`). Responde 201. |
| `POST /api/login` | Autentica e abre a sessão no Cliente. |
| `POST /api/logout` | Encerra a sessão e apaga a chave da memória. |
| `GET /api/sessao` | Diz se há sessão ativa, sem expor token ou chave. |
| `GET /api/credenciais` | Lista serviço e login de cada credencial (sem a senha). |
| `POST /api/credenciais` | Cifra e salva uma credencial (`servico`, `usuarioServico`, `senha`). |
| `GET /api/credenciais/{id}/senha` | Decifra e devolve a senha de uma credencial. |
| `PUT /api/credenciais/{id}` | Recifra e atualiza uma credencial. |
| `DELETE /api/credenciais/{id}` | Exclui uma credencial. |
| `POST /api/backup/exportar` | Monta o `.vault` cifrado com a senha do backup. |
| `POST /api/backup/importar` | Abre um `.vault`, recifra cada credencial com a chave atual e grava no Servidor. |
| `GET /api/crypto/generate` | Gera uma senha forte (`tamanho`, `maiusculas`, `minusculas`, `numeros`, `simbolos`). |
| `GET /status` | Verificação de saúde do serviço. |

Erros voltam como `{"erro": "CODIGO"}`, com o status HTTP correspondente. Os códigos estão nas fichas CT em `docs/contratos/` do Back-end 1.

## Funções de criptografia

Todas ficam em `CryptoService`:

| Função | O que faz |
|---|---|
| `gerarCredenciais(senhaMestra)` | Gera salt novo e deriva o hash de login e a chave de cifragem (cadastro). |
| `derivarHashLogin(senhaMestra, salt)` | Deriva o hash enviado ao Servidor, a partir do salt já cadastrado (login). |
| `derivarChaveCifragem(senhaMestra, salt)` | Deriva a chave AES-256, que só fica na memória (login). |
| `derivarChaveVault(senhaBackup, salt)` | Deriva a chave do arquivo `.vault`. |
| `cifrar(texto, chave)` / `decifrar(...)` | AES-256 em modo GCM, com IV novo a cada cifragem. |
| `gerarSenhaForte(...)` | Gera senha aleatória com `SecureRandom`, respeitando as opções escolhidas. |

PBKDF2-HMAC-SHA256 com 120.000 iterações deriva as chaves. O salt tem 16 bytes.

## Configuração

| Variável | Padrão | Para que serve |
|---|---|---|
| `SERVIDOR_HOST` | `localhost` | Endereço do Servidor (no Docker, `backend-1`). |
| `SERVIDOR_PORTA` | `8090` | Porta do socket do Servidor. |
| `LOG_NIVEL` | `INFO` | Nível dos logs (`DEBUG`, `INFO`, `WARN`, `ERROR`). |

Os logs ficam em `logs/cliente.log`, com um arquivo por dia e sem limite de tempo.

## Stack

- Java (JDK 17+), Spring Boot 3.2.5, Maven
- Criptografia nativa do Java (`javax.crypto`, `java.security`)
- Socket TCP com threads: `ClienteServidor` (conexão e pedidos) e `LeitorDoServidor` (a única thread que lê do Servidor)

## Como rodar

Pela raiz do repositório principal, com Docker (recomendado):

```bash
cd pi4-2026-2-0101-grupo06
cp .env.example .env   # preencha DB_PASSWORD e JWT_SECRET
docker compose up --build -d
```

Isolado, para desenvolvimento (precisa do Servidor no ar, senão as rotas de conta e credenciais respondem `ERRO_CONEXAO`):

```bash
mvn spring-boot:run
```

## Testes

```bash
mvn verify
```

`verify` roda os testes e gera o relatório de cobertura JaCoCo em `target/site/jacoco/`. Os testes de conexão usam um servidor falso com socket real, então não precisam do Servidor de verdade.

A análise estática roda no SonarQube Cloud: https://sonarcloud.io/dashboard?id=egili_pi4-0101-g6-backend-2

## Integrantes do Grupo
* Eliseu Pereira Gili — [@eliseugili](https://github.com/egili)
* Guilherme Heron da Silva — [@GuilhermeHeron](https://github.com/GuilhermeHeron)
* Leonardo Maciel Ferreira de Moraes — [@leomacifs](https://github.com/leomacifs)
* Pietra Façanha Bortolato — [@piborto](https://github.com/piborto)
