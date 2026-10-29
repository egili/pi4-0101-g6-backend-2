# CI — Passo a passo do que falta

Objetivo: CI nos repositórios do Servidor, do Cliente e do Módulo de Criptografia rodando os testes JUnit a cada push, com o build do Cliente usando o Módulo de Criptografia do repositório dele.

## Onde estamos

| Repositório | Situação |
|---|---|
| `pi4-0101-g6-backend-1` (Servidor) | CI pronto na branch `ciJunit` (verde). Falta PR + merge. |
| `pi4-0101-g6-backend-2` (Módulo de Criptografia) | CI pronto na branch `ciJunit` (verde). Virou biblioteca Maven `br.edu.pi4.g6:modulo-criptografia:1.0-SNAPSHOT`. Falta PR + merge. |
| `pi4-0101-g6-frontend` (Cliente) | Falta ajustar o CI para baixar e instalar o Módulo. |
| `pi4-2026-2-0101-grupo06` (Documentos) | Falta remover o serviço `backend-2` do `docker-compose.yml`. |

**A ordem importa:** o CI do Cliente baixa a branch `main` do Módulo. Faça os passos na ordem abaixo.

Os comandos assumem os 4 repositórios clonados lado a lado na mesma pasta, como já estão.

---

## Passo 1 — Avisar o grupo

Antes do merge, avise o Leonardo (responsável pelo Módulo de Criptografia):
- O Spring Boot foi removido; o módulo é uma biblioteca Java pura, sem servidor e sem `Dockerfile`.
- O pacote agora é `br.edu.pi4.g6.criptografia`; a classe principal é `Criptografia`, já com `gerarSalt()`.
- As próximas funções (`cifrar`, `decifrar`, `derivarChaveCifragem`, etc.) devem entrar nessa classe, com testes em `src/test/java/br/edu/pi4/g6/criptografia/`.

---

## Passo 2 — PR e merge do Módulo de Criptografia (backend-2)

1. Abra https://github.com/egili/pi4-0101-g6-backend-2
2. Vai aparecer um aviso amarelo **"ciJunit had recent pushes"** → clique em **Compare & pull request**.
   - Se não aparecer: aba **Pull requests** → **New pull request** → `base: main` ← `compare: ciJunit`.
3. Título sugerido: `Converte módulo de criptografia em biblioteca e ajusta CI`.
4. Clique em **Create pull request**.
5. Espere o check **build** ficar verde no PR.
6. Clique em **Merge pull request** → **Confirm merge**.
7. (Opcional) **Delete branch**.
8. Atualize sua cópia local:
   ```bash
   cd pi4-0101-g6-backend-2
   git switch main
   git pull
   ```

---

## Passo 3 — PR e merge do Servidor (backend-1)

Mesmo processo do passo 2, em https://github.com/egili/pi4-0101-g6-backend-1:

1. **Compare & pull request** da branch `ciJunit` para `main`.
2. Título sugerido: `CI roda testes JUnit a cada push`.
3. Espere o check **build** ficar verde → **Merge pull request** → **Confirm merge**.
4. Atualize localmente:
   ```bash
   cd pi4-0101-g6-backend-1
   git switch main
   git pull
   ```

> O teste `BuildSmokeTest` só existe para provar que o JUnit roda no CI. Quando o servidor tiver código, substitua por testes reais.

---

## Passo 4 — CI do Cliente (frontend)

Só faça depois do passo 2 (o Módulo precisa estar na `main`).

### 4.1 Criar a branch

```bash
cd pi4-0101-g6-frontend
git switch main
git pull
git switch -c ciJunit
```

### 4.2 Trocar o workflow

Substitua **todo** o conteúdo de `.github/workflows/ci.yml` por:

```yaml
name: Java CI with Maven

on:
  push:
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Checkout do Módulo de Criptografia
      uses: actions/checkout@v4
      with:
        repository: egili/pi4-0101-g6-backend-2
        ref: main
        path: modulo-criptografia

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven

    - name: Instalar o Módulo de Criptografia no Maven local
      run: mvn -B install -DskipTests -f modulo-criptografia/pom.xml

    - name: Build e testes do Cliente
      run: mvn -B test
```

O `pom.xml` do Cliente **não precisa mudar**: ele já depende de `br.edu.pi4.g6:modulo-criptografia:1.0-SNAPSHOT`.

### 4.3 Criar um teste que usa o Módulo

Crie o arquivo `src/test/java/br/edu/pi4/g6/cliente/ModuloCriptografiaTest.java`:

```java
package br.edu.pi4.g6.cliente;

import static org.junit.jupiter.api.Assertions.assertFalse;

import br.edu.pi4.g6.criptografia.Criptografia;
import org.junit.jupiter.api.Test;

class ModuloCriptografiaTest {

    @Test
    void usaModuloDeCriptografia() {
        assertFalse(Criptografia.gerarSalt().isBlank());
    }
}
```

### 4.4 Ignorar a pasta de build

```bash
echo "target/" >> .gitignore
```

### 4.5 Testar localmente

```bash
cd ../pi4-0101-g6-backend-2
mvn install -DskipTests
cd ../pi4-0101-g6-frontend
mvn test
```

Tem que aparecer `Tests run: 1, Failures: 0` e `BUILD SUCCESS`.

### 4.6 Commit, push e PR

```bash
git add .github/workflows/ci.yml .gitignore src/test
git commit -m "CI: roda testes JUnit usando o Módulo de Criptografia"
git push -u origin ciJunit
```

Depois, no GitHub (https://github.com/egili/pi4-0101-g6-frontend): **Compare & pull request** → espere o **build** verde → **Merge pull request**.

> `ref: main` faz o Cliente usar sempre a versão mais nova do Módulo. Se uma mudança no Módulo quebrar o Cliente, o CI do Cliente falha. Para travar uma versão, crie uma tag no Módulo (`git tag v0.1.0 && git push origin v0.1.0`) e use `ref: v0.1.0`.

---

## Passo 5 — Docker Compose (repositório de documentos)

O Módulo não tem mais `Dockerfile`, então o serviço `backend-2` do compose vai quebrar o `docker compose up`.

```bash
cd pi4-2026-2-0101-grupo06
git switch main
git pull
git switch -c removeBackend2Compose
```

Em `docker-compose.yml`:

1. Apague o bloco inteiro do serviço `backend-2`:
   ```yaml
     backend-2:
       build: 
         context: ../pi4-0101-g6-backend-2
       container_name: vault-backend-2
       ports:
         - "8081:8081"
       networks:
         - vault-network
   ```
2. No serviço `backend-1`, apague a linha:
   ```yaml
         BACKEND2_URL: http://backend-2:8081
   ```

Depois:

```bash
git add docker-compose.yml
git commit -m "Remove backend-2 do compose: módulo de criptografia agora é biblioteca do Cliente"
git push -u origin removeBackend2Compose
```

E abra o PR no GitHub → merge.

---

## Passo 6 — Bloquear merge com CI quebrado (opcional, recomendado)

Faça nos 3 repositórios (Servidor, Cliente, Módulo), **depois** que o CI já tiver rodado pelo menos uma vez:

1. Repositório no GitHub → **Settings** → **Branches** (ou **Rules → Rulesets**).
2. **Add branch protection rule** (ou **New ruleset**), padrão de branch: `main`.
3. Marque **Require a pull request before merging**.
4. Marque **Require status checks to pass before merging**.
5. Na busca de checks, digite e selecione **`build`**.
6. **Save changes** / **Create**.

Precisa ser administrador do repositório (dono da conta `egili`).

---

## Passo 7 — Conferência final

Em cada um dos 3 repositórios:

1. Aba **Actions** → workflow **Java CI with Maven** → último run da `main` em verde.
2. Abra o run → job **build** → passo de testes → procure `Tests run: N, Failures: 0`.
3. Faça um push qualquer em outra branch e confira que o CI dispara também.

Checklist:

- [ ] Grupo avisado sobre a mudança no Módulo (passo 1)
- [ ] backend-2 `ciJunit` mergeado na `main` (passo 2)
- [ ] backend-1 `ciJunit` mergeado na `main` (passo 3)
- [ ] CI do Cliente ajustado e mergeado (passo 4)
- [ ] `backend-2` removido do `docker-compose.yml` (passo 5)
- [ ] Proteção de branch configurada (passo 6)
- [ ] Os 3 CIs verdes na `main` (passo 7)

---

## Fora do CI (pendências encontradas, não bloqueiam este objetivo)

- `pi4-0101-g6-backend-1/Dockerfile` copia `target/*.jar` sem compilar antes e usa `openjdk:17-jdk-slim` (imagem descontinuada). Precisa de um build em duas etapas com `maven:3.9-eclipse-temurin-17` + `eclipse-temurin:17-jre`.
- `pi4-0101-g6-backend-1/database/schema.sql` está vazio; precisa das tabelas `usuario` (CT9) e `credencial` (CT10).
- `pi4-0101-g6-frontend/Dockerfile` é de Node, mas o `package.json` está em `config/`, então o build da imagem falha.
