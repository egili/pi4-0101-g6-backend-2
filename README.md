# Sistema de Cofre de Senhas Criptografadas -  Módulo de Criptografia

Sistema cliente-servidor desenvolvido em Java para o gerenciamento, armazenamento seguro e cifragem de credenciais pessoais, garantindo que o servidor central não tenha acesso aos dados em texto claro.

Biblioteca Java usada pelo Cliente para cifrar, decifrar, derivar chaves, gerar salts e gerar IVs. Roda **dentro do processo do cliente**, nunca no servidor. Isso é o que garante o modelo zero-trust: o servidor central nunca vê os dados em texto claro.

## Documentação

- **Visão geral, módulos e convenção de nomes:** [docs/fluxos/visao-geral.md](https://github.com/egili/pi4-0101-g6-frontend/blob/main/docs/fluxos/visao-geral.md)
- **Fluxos por entregável (E1 a E9):** [docs/fluxos/](https://github.com/egili/pi4-0101-g6-frontend/tree/main/docs/fluxos)

> **Observação:** este módulo é uma biblioteca interna do Cliente. Por isso, **não possui contratos próprios** em `docs/contratos/`. Ele aparece no diagrama C4 da visão geral como um bloco dentro do Cliente.

## Funções principais

| Função | O que faz |
|---|---|
| `cifrar(dados, chave)` | Cifra um dado usando a chave derivada da senha mestra (AES-GCM / CBC) |
| `decifrar(dadosCifrados, iv, chave)` | Decifra um dado cifrado usando a chave derivada |
| `derivarChaveCifragem(senhaMestra, salt)` | Deriva a chave mestra de cifragem (AES-256) mantida estritamente na memória local |
| `derivarHashAutenticacao(chaveOuSenha, salt)` | Gera o hash de autenticação unidirecional enviado ao Servidor (garantindo Zero-Trust) |
| `gerarSalt()` | Gera um salt aleatório criptograficamente seguro para o cadastro (`SecureRandom`) |
| `gerarIV()` | Gera um vetor de inicialização para a cifragem |
| `gerarSenhaForte(opcoes)` | Gera uma senha aleatória forte conforme as opções |

## Stack

- Java (JDK 17+)
- Criptografia nativa do Java (`javax.crypto`, `java.security`)

## Integrantes do Grupo
* Eliseu Pereira Gili — [@eliseugili](https://github.com/egili)
* Guilherme Heron da Silva — [@GuilhermeHeron](https://github.com/GuilhermeHeron)
* Leonardo Maciel Ferreira de Moraes — [@leomacifs](https://github.com/leomacifs)
* Pietra Façanha Bortolato — [@piborto](https://github.com/piborto)