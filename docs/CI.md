# CI — Servidor, Cliente e Módulo de Criptografia

## Situação atual

| Repositório | CI | Estado |
|---|---|---|
| `pi4-0101-g6-backend-1` (Servidor) | `mvn -B test` a cada push e pull request, em qualquer branch | Pronto |
| `pi4-0101-g6-backend-2` (Módulo de Criptografia) | `mvn -B test` a cada push e pull request, em qualquer branch | Pronto |
| `pi4-0101-g6-frontend` (Cliente) | Ainda dispara só em `main`/`master` e não encontra o Módulo | **Falta fazer (passo 1)** |

O Módulo de Criptografia agora é uma biblioteca Maven pura (sem Spring Boot), publicada localmente como:

```xml
<dependency>
    <groupId>br.edu.pi4.g6</groupId>
    <artifactId>modulo-criptografia</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Essas coordenadas são exatamente as que o `pom.xml` do Cliente já declara, então o `pom.xml` do Cliente não precisa mudar.

A primeira função disponível é `br.edu.pi4.g6.criptografia.Criptografia.gerarSalt()`.

---

## Passo 1 — CI do Cliente (responsável pelo `pi4-0101-g6-frontend`)

O artefato do Módulo não está publicado em nenhum repositório Maven. Por isso o CI do Cliente precisa clonar o repositório do Módulo e instalá-lo no Maven local do runner **antes** de rodar os testes. O repositório é público, então não é preciso token.

Substitua todo o conteúdo de `.github/workflows/ci.yml` no repositório `pi4-0101-g6-frontend` por:

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

Observações:
- `ref: main` faz o Cliente usar sempre a versão mais recente do Módulo. Se uma mudança no Módulo quebrar o Cliente, o CI do Cliente vai falhar. Para congelar uma versão, crie uma tag no Módulo (ex.: `v0.1.0`) e use `ref: v0.1.0`.
- Para provar que a dependência funciona, adicione no Cliente um teste em `src/test/java/br/edu/pi4/g6/cliente/ModuloCriptografiaTest.java`:

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

### Rodar localmente

Com os repositórios clonados lado a lado:

```bash
cd pi4-0101-g6-backend-2 && mvn install -DskipTests
cd ../pi4-0101-g6-frontend && mvn test
```

---

## Passo 2 — Docker Compose (responsável pelo `pi4-2026-2-0101-grupo06`)

Como o Módulo deixou de ser um servidor, o `Dockerfile` dele foi removido. No `docker-compose.yml`:

1. Remova o serviço `backend-2` inteiro.
2. Remova a variável `BACKEND2_URL: http://backend-2:8081` do serviço `backend-1`.

Sem isso, `docker compose up` falha, porque `../pi4-0101-g6-backend-2` não tem mais `Dockerfile`.

---

## Passo 3 (opcional) — Bloquear merge com CI quebrado

Faça isso em cada um dos 3 repositórios, depois que o CI tiver rodado pelo menos uma vez (o GitHub só lista checks que já executaram):

1. No GitHub, abra o repositório → **Settings** → **Branches** (ou **Rules → Rulesets**).
2. **Add branch protection rule** (ou **New ruleset**) com o padrão de branch `main`.
3. Marque **Require status checks to pass before merging**.
4. Na busca de checks, selecione **`build`** (nome do job no `ci.yml`).
5. Marque **Require a pull request before merging**, se o grupo ainda não usa.
6. Salve.

É preciso ser administrador do repositório. Como os repositórios estão na conta `egili`, quem faz isso é o dono da conta.

---

## Passo 4 — Conferir no GitHub

1. Faça push de qualquer branch em cada repositório.
2. Abra a aba **Actions** e confira o workflow **Java CI with Maven** em verde.
3. Dentro do job, no passo de testes, procure `Tests run: N, Failures: 0`.
