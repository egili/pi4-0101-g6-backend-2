# Back-end 2 — Módulo de Criptografia

Biblioteca Java usada pelo Cliente CLI para cifrar, decifrar, derivar chaves, gerar salts e gerar IVs. Roda **dentro do processo do cliente**, nunca no servidor. Isso é o que garante o modelo zero-trust: o servidor central nunca vê os dados em texto claro.

## Documentação

- **Visão geral, módulos e convenção de nomes:** [docs/fluxos/visao-geral.md](https://github.com/egili/pi4-0101-g6-frontend/blob/main/docs/fluxos/visao-geral.md)
- **Fluxos por entregável (E1 a E9):** [docs/fluxos/](https://github.com/egili/pi4-0101-g6-frontend/tree/main/docs/fluxos)

> **Observação:** este módulo é uma biblioteca interna do Cliente CLI. Por isso, **não possui contratos próprios** em `docs/contratos/`. Ele aparece no diagrama C4 da visão geral como um bloco dentro do Cliente.

## Funções principais

| Função | O que faz |
|---|---|
| `cifrar(dados, chave)` | Cifra um dado usando a chave derivada da senha mestra |
| `decifrar(dadosCifrados, iv, chave)` | Decifra um dado cifrado |
| `derivarChave(senhaMestra, salt)` | Deriva a chave mestra a partir da senha do usuário |
| `gerarSalt()` | Gera um salt aleatório para o cadastro |
| `gerarIV()` | Gera um vetor de inicialização para a cifragem |
| `gerarSenhaForte(opcoes)` | Gera uma senha aleatória forte conforme as opções |

## Stack

- Java (JDK 17+)
- Criptografia nativa do Java (AES, SHA-256, SecureRandom)

## Como usar

Em breve (dependência Maven/Gradle).