# Guia de CI/CD com Jenkins — monitoramento-pedpen

**Stack:** Spring Boot 3.5.4 | Java 17 | Maven | MySQL | Docker | SVN
**Estimativa de configuracao inicial:** 4 a 6 horas

---

## Visao Geral do Pipeline

```
[Commit no SVN]
      |
      v
[Jenkins detecta via Poll SCM]
      |
      v
  CHECKOUT          <- faz o checkout do repositorio SVN
      |
      v
  BUILD             <- mvn compile
      |
      v
  TEST              <- mvn test (unit tests)
      |
      v
  PACKAGE           <- mvn package -DskipTests (gera o .jar)
      |
      v
  DOCKER BUILD      <- constroi a imagem Docker
      |
      v
  DOCKER PUSH       <- envia para o registry (ex: Docker Hub ou privado)
      |
      v
  DEPLOY            <- sobe o container no servidor via SSH
      |
      v
  NOTIFY            <- notifica sucesso ou falha (email/Slack)
```

---

## Parte 1 — Pre-requisitos

### 1.1 No servidor Jenkins

- Jenkins >= 2.440
- Java 17 instalado e configurado como JDK no Jenkins
- Maven 3.9+ instalado e configurado no Jenkins
- Docker instalado e o usuario `jenkins` no grupo `docker`:
  ```bash
  sudo usermod -aG docker jenkins
  sudo systemctl restart jenkins
  ```

### 1.2 No servidor de deploy (onde a app vai rodar)

- Docker instalado
- Porta 3001 liberada no firewall
- Usuario com permissao de executar `docker` sem sudo
- Acesso SSH configurado a partir do servidor Jenkins

### 1.3 Plugins Jenkins necessarios

Instalar em **Gerenciar Jenkins > Plugins**:

| Plugin                        | Funcao                                   |
|-------------------------------|------------------------------------------|
| Pipeline                      | Pipeline declarativo (Jenkinsfile)        |
| Subversion                    | Checkout do repositorio SVN              |
| Docker Pipeline               | Comandos docker dentro do pipeline        |
| SSH Agent                     | Deploy via SSH                            |
| Credentials Binding           | Injetar secrets no pipeline               |
| JUnit                         | Publicar resultado dos testes             |
| Email Extension               | Notificacoes por email                    |

---

## Parte 2 — Credenciais no Jenkins

Cadastrar em **Gerenciar Jenkins > Credentials > (global)**:

| ID da Credencial         | Tipo                    | Conteudo                                              |
|--------------------------|-------------------------|-------------------------------------------------------|
| `svn-credentials`        | Username/Password       | Usuario e senha do servidor SVN                       |
| `docker-hub-credentials` | Username/Password       | Usuario e password/token do Docker Hub (ou registry)  |
| `deploy-ssh-key`         | SSH Username/Private Key| Chave SSH privada para acesso ao servidor de deploy   |
| `db-url`                 | Secret Text             | Valor de DATABASE_URL do banco MySQL                  |
| `db-user`                | Secret Text             | Usuario do banco                                      |
| `db-password`            | Secret Text             | Senha do banco                                        |
| `jwt-secret`             | Secret Text             | Chave JWT forte (min 32 chars aleatorios)             |

---

## Parte 3 — Dockerfile

Criar o arquivo `Dockerfile` na raiz do projeto.
O arquivo pronto esta em `Dockerfile` (gerado junto com este guia).

Conteudo explicado:
- Multi-stage build: primeira etapa compila, segunda executa (imagem menor)
- Imagem base: `eclipse-temurin:17-jre-alpine` (pequena e segura)
- Usuario nao-root para seguranca
- Healthcheck configurado

---

## Parte 4 — Jenkinsfile

Criar o arquivo `Jenkinsfile` na raiz do projeto.
O arquivo pronto esta em `Jenkinsfile` (gerado junto com este guia).

### Variaveis de ambiente do Jenkinsfile que voce precisa ajustar:

```groovy
DOCKER_IMAGE   = 'seu-usuario/monitoramento-pedpen'  // sua imagem no registry
DEPLOY_HOST    = '192.168.0.100'                      // IP do servidor de deploy
DEPLOY_USER    = 'ubuntu'                             // usuario SSH no servidor
APP_PORT       = '3001'                               // porta da aplicacao
CONTAINER_NAME = 'monitoramento-pedpen'               // nome do container
```

---

## Parte 5 — Configurar o Job no Jenkins

### 5.1 Criar o Pipeline

1. Jenkins > **Novo Item**
2. Nome: `monitoramento-pedpen`
3. Tipo: **Pipeline**
4. OK

### 5.2 Configurar o Pipeline

Na aba **Pipeline**:

- **Definition:** Pipeline script from SCM
- **SCM:** Subversion
- **Repository URL:** URL completa do trunk ou branch no SVN
  - Exemplo trunk: `svn://192.168.0.50/monitoramento-pedpen/trunk`
  - Exemplo branch: `svn://192.168.0.50/monitoramento-pedpen/branches/develop`
- **Credentials:** `svn-credentials`
- **Local module directory:** `.` (ponto — checkout na raiz do workspace)
- **Script Path:** `Jenkinsfile`

> O `Jenkinsfile` deve estar commitado no proprio repositorio SVN, dentro do trunk/branch configurado.

### 5.3 Configurar Trigger

Na aba **Build Triggers**:

- Marcar **Poll SCM** com `H/5 * * * *` (Jenkins verifica o SVN a cada 5 minutos)
- Para builds mais rapidos, usar `H/2 * * * *` (a cada 2 minutos)

> O SVN nao tem suporte nativo a webhooks como o Git. Poll SCM e o mecanismo padrao.
> Se o servidor SVN suportar hooks `post-commit`, e possivel acionar o Jenkins via HTTP,
> mas o Poll SCM e suficiente para a maioria dos casos.

---

## Parte 6 — Estrategia de Trunk e Branches no SVN

A estrutura padrao de repositorio SVN e:

```
svn://seu-servidor/monitoramento-pedpen/
  trunk/                <- linha principal (equivalente ao main do Git)
  branches/
    develop/            <- homologacao
    feature-XXXX/       <- desenvolvimento de funcionalidades
  tags/
    v1.0.0/             <- snapshots de versoes estáveis
```

Estrategia de pipeline por caminho:

```
trunk/
  -> Pipeline completo: build + test + package + docker + deploy em PRODUCAO

branches/develop/
  -> Pipeline: build + test + package + docker + deploy em HOMOLOGACAO

branches/feature-*/
  -> Pipeline: build + test apenas (sem deploy)
```

**Como implementar no Jenkins:**

Criar um Job separado por ambiente, cada um apontando para o caminho SVN correspondente:

| Job Jenkins                    | URL SVN                                      | Deploy       |
|-------------------------------|----------------------------------------------|--------------|
| `monitoramento-pedpen-prod`    | `.../trunk`                                  | Producao     |
| `monitoramento-pedpen-homolog` | `.../branches/develop`                       | Homologacao  |

No `Jenkinsfile`, usar a variavel de ambiente `SVN_URL` para identificar o ambiente:

```groovy
when {
    environment name: 'SVN_URL', value: 'svn://seu-servidor/.../trunk'
}
```

Ou simplesmente manter Jenkinsfiles distintos por job, com as variaveis de ambiente ajustadas.

---

## Parte 7 — Variaveis de Ambiente por Ambiente

Criar arquivos de configuracao por ambiente:

```
src/main/resources/
  application.properties          <- configuracoes comuns
  application-prod.properties     <- producao (sem secrets, apenas referencias a env vars)
  application-homolog.properties  <- homologacao
```

`application-prod.properties`:
```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
api.security.token.secret=${JWT_SECRET}
api.security.token.issuer=API PedPen
server.port=3001
```

O profile e ativado via variavel de ambiente no container:
```
SPRING_PROFILES_ACTIVE=prod
```

---

## Parte 8 — Monitoramento do Container

Adicionar ao `docker run` ou `docker-compose.yml`:

```bash
# Verificar se a app esta de pe
docker inspect --format='{{.State.Health.Status}}' monitoramento-pedpen

# Ver logs em tempo real
docker logs -f monitoramento-pedpen

# Reiniciar se necessario
docker restart monitoramento-pedpen
```

Para restart automatico em caso de falha:
```bash
--restart unless-stopped
```

Ja esta configurado no Jenkinsfile.

---

## Parte 9 — Rollback Manual

Se o deploy falhar e precisar voltar a versao anterior:

```bash
# No servidor de deploy

# Listar imagens disponiveis
docker images seu-usuario/monitoramento-pedpen

# Parar o container atual
docker stop monitoramento-pedpen
docker rm monitoramento-pedpen

# Subir a versao anterior (usando a tag do build anterior)
docker run -d \
  --name monitoramento-pedpen \
  --restart unless-stopped \
  -p 3001:3001 \
  -e DATABASE_URL="..." \
  -e DATABASE_USER="..." \
  -e DATABASE_PASSWORD="..." \
  -e JWT_SECRET="..." \
  -e SPRING_PROFILES_ACTIVE=prod \
  seu-usuario/monitoramento-pedpen:BUILD_ANTERIOR
```

O Jenkins gera uma tag por numero de build: `seu-usuario/monitoramento-pedpen:42`
E tambem atualiza a tag `latest`.

---

## Parte 10 — Evolucao Futura (opcional)

| Melhoria                    | Ferramenta sugerida               | Quando considerar                        |
|-----------------------------|-----------------------------------|------------------------------------------|
| Analise de codigo estatica  | SonarQube                         | A partir de 2+ desenvolvedores           |
| Registry privado            | Harbor, AWS ECR, GitLab Registry  | Quando nao quiser imagem publica         |
| Orquestracao de containers  | Docker Compose (simples), K8s     | Quando tiver mais de 1 servico           |
| Secrets management          | HashiCorp Vault, AWS Secrets Manager | Quando tiver multiplos ambientes/times |
| Testes de integracao no CI  | Testcontainers + JUnit            | Apos escrever os testes de integracao    |
| Notificacoes no Slack       | Jenkins Slack Plugin              | Para visibilidade do time                |

---

## Checklist de Validacao

Antes de considerar o CI/CD pronto, validar:

- [ ] Commit no `trunk` dispara o pipeline via Poll SCM
- [ ] Falha no `mvn test` bloqueia o deploy
- [ ] Secrets nao aparecem nos logs do Jenkins (mascarados pelas Credentials)
- [ ] Container sobe com `--restart unless-stopped`
- [ ] Healthcheck do Docker marca o container como `healthy` apos o boot
- [ ] Rollback manual documentado e testado
- [ ] Jenkins nao usa usuario `root`
- [ ] Imagem Docker nao usa usuario `root` dentro do container

---

*Gerado em 2026-07-13 | Spring Boot 3.5.4 | Java 17 | Maven | MySQL*