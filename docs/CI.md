# CI — Passo a passo do que falta

Objetivo: CI nos repositórios do Servidor, do Cliente e do Módulo de Criptografia rodando os testes JUnit a cada push, com o build do Cliente usando o Módulo de Criptografia do repositório dele.

## Onde estamos

| Repositório | Situação |
|---|---|
| `pi4-0101-g6-backend-1` (Servidor) | Branch `ciJunit` atualizada com a `main`, sem conflitos. CI a cada push. Falta PR + merge. |
| `pi4-0101-g6-backend-2` (Módulo de Criptografia) | Branch `ciJunit` atualizada com a `main`, sem conflitos. CI a cada push e jar importável pelo Cliente. Falta PR + merge. |
| `pi4-0101-g6-frontend` (Cliente) | Falta ajustar o `pom.xml` e o CI para baixar e instalar o Módulo. |

O que a branch `ciJunit` muda em relação à `main`:

- **Os dois back-ends:** o CI dispara em todo push e pull request, em qualquer branch (antes só `main`/`master`), e a pasta `target/` sai do git (já estava no `.gitignore`, mas tinha arquivos versionados).
- **Só o Módulo (backend-2):** o `spring-boot-maven-plugin` ganhou `<classifier>exec</classifier>`. Com isso o build gera dois jars:
  - `backend-2-0.0.1-SNAPSHOT.jar`: jar normal, que o Cliente importa como dependência (`CryptoService` etc.);
  - `backend-2-0.0.1-SNAPSHOT-exec.jar`: jar executável do Spring Boot, usado pelo `Dockerfile` (já ajustado).

  Sem isso o Spring Boot reempacota o jar e o Cliente não consegue importar nenhuma classe do módulo.

**A ordem importa:** o CI do Cliente baixa a branch `main` do Módulo. Faça os passos na ordem abaixo.

Os comandos assumem os repositórios clonados lado a lado na mesma pasta.

---

## Passo 1 — Avisar o grupo

Avise o Leonardo (Módulo de Criptografia) e o Guilherme (Cliente):
- O jar executável do backend-2 agora se chama `backend-2-0.0.1-SNAPSHOT-exec.jar` (o `Dockerfile` já foi ajustado). Quem roda o jar à mão deve usar esse nome.
- O Cliente vai depender de `br.puc.cofre:backend-2`. Mudanças de nome/assinatura no `CryptoService` quebram o CI do Cliente.

---

## Passo 2 — PR e merge do Módulo de Criptografia (backend-2)

1. Abra https://github.com/egili/pi4-0101-g6-backend-2
2. Clique em **Compare & pull request** no aviso amarelo da branch `ciJunit`.
   - Se não aparecer: aba **Pull requests** → **New pull request** → `base: main` ← `compare: ciJunit`.
3. Título sugerido: `CI a cada push e jar importável pelo Cliente`.
4. **Create pull request**.
5. Espere o check **build** ficar verde no PR.
6. **Merge pull request** → **Confirm merge** → (opcional) **Delete branch**.
7. Atualize sua cópia local:
   ```bash
   cd pi4-0101-g6-backend-2
   git switch main
   git pull
   ```

---

## Passo 3 — PR e merge do Servidor (backend-1)

Mesmo processo, em https://github.com/egili/pi4-0101-g6-backend-1:

1. **Compare & pull request** da branch `ciJunit` para `main`.
2. Título sugerido: `CI roda testes JUnit a cada push`.
3. Espere o check **build** ficar verde → **Merge pull request** → **Confirm merge**.
4. Atualize localmente:
   ```bash
   cd pi4-0101-g6-backend-1
   git switch main
   git pull
   ```

---

## Passo 4 — Cliente (frontend)

Só faça depois do passo 2 (o Módulo precisa estar na `main`).

### 4.1 Criar a branch

```bash
cd pi4-0101-g6-frontend
git switch main
git pull
git switch -c ciJunit
```

### 4.2 Trocar a dependência no `pom.xml`

No `pom.xml` do Cliente, troque este bloco:

```xml
        <dependency>
            <groupId>br.edu.pi4.g6</groupId>
            <artifactId>modulo-criptografia</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```

por:

```xml
        <dependency>
            <groupId>br.puc.cofre</groupId>
            <artifactId>backend-2</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
```

(São as coordenadas reais do `pom.xml` do backend-2.)

### 4.3 Trocar o workflow

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

O repositório do Módulo é público, então não precisa de token.

### 4.4 Criar um teste que usa o Módulo

Crie o arquivo `src/test/java/br/edu/pi4/g6/cliente/ModuloCriptografiaTest.java`:

```java
package br.edu.pi4.g6.cliente;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import br.puc.cofre.backend2.dto.CryptoResponse;
import br.puc.cofre.backend2.service.CryptoService;
import org.junit.jupiter.api.Test;

class ModuloCriptografiaTest {

    @Test
    void usaModuloDeCriptografia() {
        CryptoResponse resposta = new CryptoService().gerarCredenciais("senhaMestraDeTeste");
        assertNotEquals(resposta.getHashLogin(), resposta.getChaveCifra());
    }
}
```

### 4.5 Ignorar a pasta de build

```bash
echo "target/" >> .gitignore
```

### 4.6 Testar localmente

```bash
cd ../pi4-0101-g6-backend-2
mvn install -DskipTests
cd ../pi4-0101-g6-frontend
mvn test
```

Tem que aparecer `Tests run: 1, Failures: 0` e `BUILD SUCCESS`.

### 4.7 Commit, push e PR

```bash
git add pom.xml .github/workflows/ci.yml .gitignore src/test
git commit -m "CI: roda testes JUnit usando o Módulo de Criptografia"
git push -u origin ciJunit
```

No GitHub (https://github.com/egili/pi4-0101-g6-frontend): **Compare & pull request** → espere o **build** verde → **Merge pull request**.

> `ref: main` faz o Cliente usar sempre a versão mais nova do Módulo. Se uma mudança no Módulo quebrar o Cliente, o CI do Cliente falha. Para travar uma versão, crie uma tag no Módulo (`git tag v0.1.0 && git push origin v0.1.0`) e use `ref: v0.1.0`.

---

## Passo 5 — Bloquear merge com CI quebrado (opcional, recomendado)

Faça nos 3 repositórios (Servidor, Cliente, Módulo), **depois** que o CI já tiver rodado pelo menos uma vez:

1. Repositório no GitHub → **Settings** → **Branches** (ou **Rules → Rulesets**).
2. **Add branch protection rule** (ou **New ruleset**), padrão de branch: `main`.
3. Marque **Require a pull request before merging**.
4. Marque **Require status checks to pass before merging**.
5. Na busca de checks, digite e selecione **`build`**.
6. **Save changes** / **Create**.

Precisa ser administrador do repositório (dono da conta `egili`).

---

## Passo 6 — Conferência final

Em cada um dos 3 repositórios:

1. Aba **Actions** → workflow **Java CI with Maven** → último run da `main` em verde.
2. Abra o run → job **build** → passo de testes → procure `Tests run: N, Failures: 0`.
3. Faça um push qualquer em outra branch e confira que o CI dispara também.

Checklist:

- [ ] Grupo avisado (passo 1)
- [ ] backend-2 `ciJunit` mergeado na `main` (passo 2)
- [ ] backend-1 `ciJunit` mergeado na `main` (passo 3)
- [ ] Cliente com `pom.xml`, CI e teste ajustados e mergeados (passo 4)
- [ ] Proteção de branch configurada (passo 5)
- [ ] Os 3 CIs verdes na `main` (passo 6)

---

## Fora do CI (não bloqueia este objetivo)

- A documentação (ADRs e visão geral) descreve o Módulo de Criptografia como biblioteca dentro do Cliente e a comunicação via Socket TCP, mas o código atual usa Spring Boot com API REST nos dois back-ends. Vale o grupo alinhar e registrar em um ADR.
- `pi4-0101-g6-frontend/Dockerfile` é de Node, mas o `package.json` está em `config/`, então o build da imagem falha.
