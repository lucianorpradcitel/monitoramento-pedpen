# Guia de Refatoracao — monitoramento-pedpen

**Objetivo:** Tornar a API RESTful, performatica, escalavel e facil de manter.
**Estimativa total:** 6 a 9 dias uteis (desenvolvedor unico, tempo parcial dedicado).

---

## FASE 1 — Criticos e Bugs (Dia 1)

Estimativa: **4 a 6 horas**

Esses itens causam comportamento incorreto ou falha de seguranca agora.

---

### 1.1 Corrigir tipo do ID no PedidosRepository

**Arquivo:** `repository/PedidosRepository.java:11`

O repositorio declara `JpaRepository<Pedido, Long>` mas o ID de `Pedido` e `String` (UUID).
E um type mismatch silencioso que pode causar falhas em operacoes por ID.

**O que fazer:**
```java
// ANTES
public interface PedidosRepository extends JpaRepository<Pedido, Long>

// DEPOIS
public interface PedidosRepository extends JpaRepository<Pedido, String>
```

---

### 1.2 Remover secret JWT fraco do application.properties

**Arquivo:** `resources/application.properties:11`

`api.security.token.secret=secret` e uma chave trivialmente quebravel e commitada no repositorio.

**O que fazer:**
1. Remover o valor do `application.properties`, deixar apenas a chave:
   ```properties
   api.security.token.secret=${JWT_SECRET}
   api.security.token.issuer=${JWT_ISSUER:API PedPen}
   ```
2. Definir a variavel de ambiente `JWT_SECRET` no servidor/container com no minimo 32 caracteres aleatorios.
3. Adicionar `JWT_SECRET` ao `.gitignore` se usar `.env`.

---

### 1.3 Corrigir propagacao de excecoes (stack trace perdido)

**Arquivos:** `PedidosController.java:42,50,89` | `ProdutoController.java:43,67,87`

O padrao `throw new RuntimeException("mensagem: " + e)` descarta o stack trace original.

**O que fazer:**
```java
// ANTES
throw new RuntimeException("Ocorreu um erro inesperado: " + e);

// DEPOIS
throw new RuntimeException("Ocorreu um erro inesperado", e);
```

Aplicar em todos os catch dos controllers.

---

### 1.4 Resolver ou remover allow-bean-definition-overriding

**Arquivo:** `resources/application.properties:14`

`spring.main.allow-bean-definition-overriding=true` mascara um conflito real de beans.

**O que fazer:**
1. Remover a propriedade.
2. Subir a aplicacao e identificar qual bean esta em conflito pelo erro.
3. Resolver o conflito (renomear o bean ou qualificar com `@Primary` / `@Qualifier`).

---

## FASE 2 — RESTful e Contrato HTTP (Dia 2)

Estimativa: **4 a 6 horas**

---

### 2.1 Corrigir retorno 200 para 201 nos POSTs de criacao

**Arquivo:** `PedidosController.java:40` | `ProdutoController.java:41`

POST de criacao deve retornar `201 Created` com header `Location`.

**O que fazer em PedidosController:**
```java
@PostMapping
public ResponseEntity<PedidoResponseDTO> registrarPedido(
        @RequestBody @Valid PedidoDTO request,
        UriComponentsBuilder uriBuilder) {

    Pedido pedido = service.registrarPedido(request);
    URI uri = uriBuilder.path("/pendentes/{id}").buildAndExpand(pedido.getId()).toUri();
    return ResponseEntity.created(uri).body(new PedidoResponseDTO(pedido));
}
```

O mesmo padrao para `POST /produtos`.

---

### 2.2 Corrigir DELETE — identificador deve estar no path

**Arquivo:** `PedidosController.java:82`

`DELETE /pendentes?codigoPedido=X&codigoCliente=Y` viola REST. O recurso deve ser identificado pelo path.

**O que fazer:**
```java
// ANTES
@DeleteMapping("/pendentes")
public void removePedidoDoMonitoramento(@RequestParam String codigoPedido, @RequestParam String codigoCliente)

// DEPOIS — opcao A (se tiver ID unico acessivel)
@DeleteMapping("/{id}")
public ResponseEntity<Void> removePedidoDoMonitoramento(@PathVariable String id)

// DEPOIS — opcao B (se a chave de negocio e codigoPedido + cliente)
@DeleteMapping("/{codigoPedido}")
public ResponseEntity<Void> removePedidoDoMonitoramento(
        @PathVariable String codigoPedido,
        @RequestParam String cliente)
```

Retornar `204 No Content` ao inves de `void`.

---

### 2.3 Corrigir PATCH — identificador no path

**Arquivo:** `ProdutoController.java:80`

`PATCH /produtos` sem identificador nao e REST.

**O que fazer:**
```java
// ANTES
@PatchMapping()
public ResponseEntity<Produto> atualizarPedido(@RequestBody ProdutoDTO request)

// DEPOIS
@PatchMapping("/{codigoProduto}")
public ResponseEntity<ProdutoResponseDTO> atualizarProduto(
        @PathVariable String codigoProduto,
        @RequestBody @Valid ProdutoStatusDTO request)
```

---

### 2.4 Corrigir URL do endpoint de autenticacao

**Arquivo:** `AuthController.java:23`

`/Autenticar` usa PascalCase e e um verbo. Endpoints REST devem ser substantivos e lowercase.

**O que fazer:**
```java
// ANTES
@RequestMapping("/Autenticar")

// DEPOIS
@RequestMapping("/auth")
// e o POST vira POST /auth/token
@PostMapping("/token")
```

Atualizar tambem a regra em `SecurityConfigurations.java`:
```java
req.requestMatchers(HttpMethod.POST, "/auth/token").permitAll();
```

---

### 2.5 Adicionar @RequestMapping na classe PedidosController

**Arquivo:** `PedidosController.java:21`

Os mappings `/pendentes` e `pendentes-lote` estao soltos nos metodos. Organizar:

```java
@RestController
@RequestMapping("/pendentes")
public class PedidosController {

    @PostMapping               // POST /pendentes
    @PostMapping("/lote")      // POST /pendentes/lote
    @GetMapping                // GET  /pendentes
    @DeleteMapping("/{id}")    // DELETE /pendentes/{id}
}
```

---

### 2.6 POST /pendentes-lote — barra faltando e retorno incorreto

**Arquivo:** `PedidosController.java:45`

```java
// ANTES
@PostMapping("pendentes-lote")   // sem /

// DEPOIS (apos 2.5 acima, vira)
@PostMapping("/lote")
```

---

### 2.7 Retorno 409 Conflict ao inves de RuntimeException para duplicatas

**Arquivo:** `ClienteService.java:21`

```java
// ANTES
throw new RuntimeException("Empresa ja existe");

// DEPOIS — lancar excecao de dominio que o @ControllerAdvice mapeia para 409
throw new ConflictException("Username ja cadastrado: " + dados.userName());
```

---

## FASE 3 — Tratamento Global de Erros (Dia 2-3)

Estimativa: **3 a 4 horas**

Criar um `@ControllerAdvice` centralizado elimina todos os try-catch dos controllers e padroniza as respostas de erro.

---

### 3.1 Criar excecoes de dominio

Criar pacote `exception/`:

```
exception/
  NotFoundException.java       -> 404
  ConflictException.java       -> 409
  BusinessException.java       -> 422 Unprocessable Entity
```

Exemplo:
```java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
```

---

### 3.2 Criar GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        // mapear erros de campo
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGeneric(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado");
    }
}
```

Usar `ProblemDetail` (RFC 7807) disponivel no Spring 6+.

---

### 3.3 Remover todos os try-catch dos controllers

Apos o `@ControllerAdvice`, os controllers ficam limpos:

```java
@PostMapping
public ResponseEntity<PedidoResponseDTO> registrarPedido(@RequestBody @Valid PedidoDTO request, UriComponentsBuilder uriBuilder) {
    Pedido pedido = service.registrarPedido(request);
    URI uri = uriBuilder.path("/pendentes/{id}").buildAndExpand(pedido.getId()).toUri();
    return ResponseEntity.created(uri).body(new PedidoResponseDTO(pedido));
}
```

---

## FASE 4 — Validacao de Entrada (Dia 3)

Estimativa: **2 a 3 horas**

---

### 4.1 Adicionar Bean Validation nas DTOs

Adicionar dependencia (se nao existir):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Anotar os campos:
```java
public record PedidoDTO(
    @NotBlank(message = "codigoPedido e obrigatorio")
    String codigoPedido,

    @NotBlank(message = "cliente e obrigatorio")
    String cliente,

    @NotBlank(message = "plataforma e obrigatoria")
    String plataforma,

    String erro,
    int status
) {}
```

---

### 4.2 Anotar os controllers com @Valid

```java
public ResponseEntity<?> registrarPedido(@RequestBody @Valid PedidoDTO request) { ... }
```

O `GlobalExceptionHandler` ja captura `MethodArgumentNotValidException` e retorna 400.

---

## FASE 5 — DTOs de Resposta e Desacoplamento de Entidades (Dia 3-4)

Estimativa: **3 a 4 horas**

Entidades JPA nao devem ser retornadas diretamente na API. Mudancas no modelo quebram o contrato.

---

### 5.1 Criar DTOs de resposta como records

```
DTO/
  response/
    PedidoResponseDTO.java
    ProdutoResponseDTO.java
    ClienteResponseDTO.java
```

Exemplo:
```java
public record PedidoResponseDTO(
    String id,
    String codigoPedido,
    String cliente,
    String plataforma,
    String erro,
    int status,
    LocalDateTime dataPedido,
    LocalDateTime ultimaAlteracao
) {
    public PedidoResponseDTO(Pedido pedido) {
        this(pedido.getId(), pedido.getCodigoPedido(), pedido.getCliente(),
             pedido.getPlataforma(), pedido.getErro(), pedido.getStatus(),
             pedido.getDataPedido(), pedido.getUltimaAlteracao());
    }
}
```

---

### 5.2 Converter PedidoDTO e PedidoLoteDTO para records

```java
// ANTES — classe com getters/setters
public class PedidoDTO { ... }

// DEPOIS — record imutavel
public record PedidoDTO(
    String codigoPedido,
    String cliente,
    String erro,
    String plataforma,
    int status,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime dataPedido,
    int sequencialProcessamento
) {}
```

---

### 5.3 Mover DadosAuth e DadosTokenJWT para pacote DTO

**Arquivo:** `AuthController.java:17-18`

```java
// Remover do topo do AuthController e criar:
// DTO/request/AuthRequestDTO.java
public record AuthRequestDTO(String userName, String senha) {}

// DTO/response/TokenResponseDTO.java
public record TokenResponseDTO(String token) {}
```

---

### 5.4 Mover logica de mapeamento para fora do DTO

**Arquivo:** `PedidoLoteDTO.java:59` — `converterDTO()` e metodo estatico num DTO.

**O que fazer:**
Remover o metodo do DTO e mover a logica para `PedidoService.registrarPedidosList()` diretamente, ou criar um `PedidoMapper` separado.

---

## FASE 6 — Injecao de Dependencia Consistente (Dia 4)

Estimativa: **1 a 2 horas**

---

### 6.1 Substituir @Autowired por constructor injection

Arquivos com @Autowired no campo (anti-padrao):
- `AuthController.java` — manager, tokenService
- `SecurityFilter.java` — tokenService, clienteRepository
- `ClienteService.java` — clienteRepository, passwordEncoder
- `SecurityConfigurations.java` — securityFilter

**O que fazer em cada um:**
```java
// ANTES
@Autowired
private AuthenticationManager manager;

// DEPOIS — com Lombok @RequiredArgsConstructor ou construtor manual
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager manager;
    private final TokenService tokenService;
    ...
}
```

---

## FASE 7 — Status como Enum (Dia 4)

Estimativa: **2 horas**

Magic numbers `0`, `1` espalhados pelo codigo sem significado claro.

---

### 7.1 Criar enums de status

```java
// model/enums/StatusPedido.java
public enum StatusPedido {
    PENDENTE(0),
    INTEGRADO(1),
    ERRO(2);

    private final int valor;
    StatusPedido(int valor) { this.valor = valor; }
    public int getValor() { return valor; }

    public static StatusPedido fromValor(int valor) {
        return Arrays.stream(values())
            .filter(s -> s.valor == valor)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Status invalido: " + valor));
    }
}
```

```java
// model/enums/StatusProduto.java
public enum StatusProduto { PENDENTE(0), RESOLVIDO(1); ... }
```

Atualizar os modelos, DTOs e services para usar o enum.

---

## FASE 8 — Performance: Paginacao (Dia 5)

Estimativa: **3 a 4 horas**

`repository.findAll()` sem limite e um risco operacional. Qualquer listagem sem filtro pode retornar milhares de registros.

---

### 8.1 Adicionar Pageable em todos os GETs de listagem

**PedidosController:**
```java
@GetMapping
public ResponseEntity<Page<PedidoResponseDTO>> retornarPedidos(
        @RequestParam(required = false) String cliente,
        @RequestParam(required = false) String codigoPedido,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate data,
        @PageableDefault(size = 50, sort = "dataPedido", direction = DESC) Pageable pageable) {

    return ResponseEntity.ok(service.retornarPedidos(cliente, codigoPedido, status, data, pageable));
}
```

O cliente passa `?page=0&size=20&sort=dataPedido,desc` na URL.

---

## FASE 9 — Performance: Substituir if-else de filtros por Specification (Dia 5-6)

Estimativa: **4 a 5 horas**

O bloco if-else de 8 condicoes em `PedidoService` e os 7 metodos derivados no `PedidosRepository` podem ser substituidos por um unico `JpaSpecificationExecutor`.

---

### 9.1 Fazer PedidosRepository estender JpaSpecificationExecutor

```java
public interface PedidosRepository extends JpaRepository<Pedido, String>, JpaSpecificationExecutor<Pedido> {
    // remover todos os findBy* derivados
}
```

### 9.2 Criar PedidoSpecification

```java
public class PedidoSpecification {

    public static Specification<Pedido> comCliente(String cliente) {
        return (root, query, cb) ->
            cliente == null ? null : cb.equal(root.get("cliente"), cliente);
    }

    public static Specification<Pedido> comCodigoPedido(String codigo) {
        return (root, query, cb) ->
            codigo == null ? null : cb.equal(root.get("codigoPedido"), codigo);
    }

    public static Specification<Pedido> comStatus(Integer status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Pedido> comData(LocalDate data) {
        return (root, query, cb) ->
            data == null ? null : cb.equal(cb.function("DATE", LocalDate.class, root.get("dataPedido")), data);
    }
}
```

### 9.3 Simplificar PedidoService

```java
public Page<Pedido> retornarPedidos(String cliente, String codigoPedido, String status, LocalDate data, Pageable pageable) {
    Integer statusInt = parseStatus(status);

    Specification<Pedido> spec = Specification
        .where(comCliente(cliente))
        .and(comCodigoPedido(codigoPedido))
        .and(comStatus(statusInt))
        .and(comData(data));

    return repository.findAll(spec, pageable);
}
```

O bloco if-else inteiro e os 7 metodos do repository sao removidos.

---

## FASE 10 — @Transactional e Consistencia (Dia 6)

Estimativa: **1 a 2 horas**

---

### 10.1 Anotar metodos que fazem multiplas operacoes no banco

```java
// PedidoService
@Transactional
public List<Pedido> registrarPedidosList(List<PedidoLoteDTO> listaPedidos) { ... }

@Transactional
public void removePedidoDoMonitoramento(String codigoPedido, String cliente) { ... }

// ProdutoService
@Transactional
public Produto registrarProduto(ProdutoDTO produtoDTO) { ... }

@Transactional
public Optional<Produto> registraComoResolvido(...) { ... }
```

---

### 10.2 Corrigir performance do lote (N+1)

**Arquivo:** `PedidoService.java:52`

Ao inves de buscar pedido por pedido dentro do loop:

```java
// O que fazer:
// 1. Extrair todos os codigos e clientes da lista recebida
// 2. Buscar todos os existentes em uma unica query
// 3. Montar um Map para lookup O(1)
// 4. Iterar e fazer merge

List<String> codigos = listaPedidos.stream().map(PedidoLoteDTO::getCodigoPedido).toList();

Map<String, Pedido> existentes = repository.findByCodigoPedidoIn(codigos)
    .stream()
    .collect(Collectors.toMap(p -> p.getCodigoPedido() + "|" + p.getCliente(), p -> p));

// Adicionar ao repository:
// List<Pedido> findByCodigoPedidoIn(List<String> codigos);
```

---

## FASE 11 — Correcoes de Modelo (Dia 6)

Estimativa: **1 a 2 horas**

---

### 11.1 Substituir java.util.Date por java.time.LocalDateTime em Produto

**Arquivo:** `model/Produto.java:9`

```java
// ANTES
import java.util.Date;
private Date dataErro;

// DEPOIS
import java.time.LocalDateTime;
private LocalDateTime dataErro;
```

Atualizar tambem `ProdutoDTO` que usa `Date dataErro`.

---

### 11.2 Timezone configuravel no TokenService

**Arquivo:** `service/TokenService.java:27`

```java
// ANTES
return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));

// DEPOIS — via properties
@Value("${api.security.token.expiration-hours:2}")
private int expirationHours;

@Value("${api.security.token.zone-offset:-03:00}")
private String zoneOffset;

private Instant dataExpiracao() {
    return LocalDateTime.now().plusHours(expirationHours).toInstant(ZoneOffset.of(zoneOffset));
}
```

---

## FASE 12 — shopifyProductService: Logging e Qualidade (Dia 7)

Estimativa: **2 a 3 horas**

---

### 12.1 Substituir System.out.println por @Slf4j

**Arquivo:** `sync/service/shopifyProductService.java`

```java
// ANTES
System.out.println("=== Iniciando sincronizacao de produtos Shopify ===");
System.err.println("ERRO SKU " + variant.getSku() + ": " + e.getMessage());

// DEPOIS
@Slf4j
public class ShopifyProductService {
    // ...
    log.info("Iniciando sincronizacao de produtos Shopify");
    log.error("Erro ao processar SKU {}: {}", variant.getSku(), e.getMessage(), e);
}
```

---

### 12.2 Injetar ObjectMapper como bean

**Arquivo:** `sync/service/shopifyProductService.java:20`

```java
// ANTES — instancia nova a cada deploy do servico
this.objectMapper = new ObjectMapper();

// DEPOIS — injetar via construtor (Spring ja registra ObjectMapper como bean)
public ShopifyProductService(RestTemplate restTemplate, ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
}
```

---

### 12.3 Renomear classe para PascalCase

```
shopifyProductService  ->  ShopifyProductService
shopifyCategoryService ->  ShopifyCategoryService
shopifyCategoryController -> ShopifyCategoryController
```

Convencao Java: classes sempre em PascalCase.

---

## FASE 13 — Configuracao do Pool Hikari (Dia 7)

Estimativa: **30 minutos**

**Arquivo:** `resources/application.properties`

```properties
# Pool de conexoes
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=3
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
spring.datasource.hikari.keepalive-time=60000
spring.datasource.hikari.pool-name=MonitoramentoHikariPool
```

Ajustar `maximum-pool-size` conforme o numero de conexoes que o banco suporta e o numero de instancias da aplicacao.

---

## FASE 14 — Versionamento de API (Dia 8)

Estimativa: **1 a 2 horas**

---

### 14.1 Adicionar prefixo /api/v1 em todos os controllers

Opcao mais simples — adicionar em cada `@RequestMapping`:

```java
@RequestMapping("/api/v1/pendentes")
@RequestMapping("/api/v1/produtos")
@RequestMapping("/api/v1/auth")
@RequestMapping("/api/v1/cadastro")
```

Atualizar `SecurityConfigurations` com os novos paths.
Atualizar o Swagger (`springdoc.api-docs.path` e `springdoc.swagger-ui.path`) se necessario.

---

## FASE 15 — Documentacao Swagger Correta (Dia 8)

Estimativa: **1 a 2 horas**

Correcoes pontuais nos `@ApiResponse` errados:

- `ProdutoController.java:55` — `schema = @Schema(implementation = Pedido.class)` referencia `Pedido` num endpoint de `Produto`.
- `ProdutoController.java:73` — descricao diz "pedido" em endpoint de produto (copy-paste).
- `PedidosController.java:33` — descricao diz "Erro registrado com sucesso" num endpoint de registro de pedido pendente.
- Adicionar `@Operation` nos endpoints que ainda nao tem (POST /pendentes-lote, POST /cadastro, PATCH /produtos).
- Documentar os parametros de query com `@Parameter`.

---

## FASE 16 — Escalabilidade: Sync Assincrona (Dia 9 — Opcional)

Estimativa: **3 a 4 horas**

A sincronizacao Shopify e sincrona e bloqueante, podendo travar a thread por varios minutos.

**O que fazer:**
1. Marcar o metodo de sincronizacao com `@Async`:
   ```java
   @Async
   public CompletableFuture<Void> iniciarSincronizacao(ShopifySyncProductRequest request) { ... }
   ```

2. Habilitar `@EnableAsync` na aplicacao.

3. O endpoint de disparo passa a retornar `202 Accepted` imediatamente:
   ```java
   @PostMapping("/sync")
   public ResponseEntity<Void> dispararSync(@RequestBody ShopifySyncProductRequest request) {
       shopifyService.iniciarSincronizacao(request); // nao bloqueia
       return ResponseEntity.accepted().build();
   }
   ```

---

## Cronograma Resumido

| Fase | Descricao                              | Estimativa | Dia Sugerido |
|------|----------------------------------------|------------|--------------|
| 1    | Criticos e bugs                        | 4-6h       | Dia 1        |
| 2    | RESTful e contrato HTTP                | 4-6h       | Dia 2        |
| 3    | Tratamento global de erros             | 3-4h       | Dia 2-3      |
| 4    | Validacao de entrada                   | 2-3h       | Dia 3        |
| 5    | DTOs de resposta e desacoplamento      | 3-4h       | Dia 3-4      |
| 6    | Injecao de dependencia consistente     | 1-2h       | Dia 4        |
| 7    | Status como enum                       | 2h         | Dia 4        |
| 8    | Paginacao nos GETs                     | 3-4h       | Dia 5        |
| 9    | Specification substituindo if-else     | 4-5h       | Dia 5-6      |
| 10   | @Transactional e correcao do N+1       | 1-2h       | Dia 6        |
| 11   | Correcoes de modelo (Date, timezone)   | 1-2h       | Dia 6        |
| 12   | shopifyProductService: qualidade       | 2-3h       | Dia 7        |
| 13   | Configuracao Hikari                    | 0,5h       | Dia 7        |
| 14   | Versionamento /api/v1                  | 1-2h       | Dia 8        |
| 15   | Correcoes no Swagger                   | 1-2h       | Dia 8        |
| 16   | Sync assincrona (opcional)             | 3-4h       | Dia 9        |

**Total minimo (sem fase 16):** 36 a 49 horas
**Total maximo (com fase 16):** 39 a 53 horas
**Em dias uteis com dedicacao de ~6h/dia: 6 a 9 dias**

---

## Ordem de Dependencias

Algumas fases dependem de outras — respeitar essa ordem evita retrabalho:

```
Fase 1  (bugs criticos)
  -> Fase 3  (exception handler) — precisa existir antes de remover os try-catch
      -> Fase 2  (contrato REST)  — usa o exception handler para tratar 404, 409
          -> Fase 4  (validacao)  — usa o exception handler para tratar 400
              -> Fase 5  (DTOs de resposta) — usados nos endpoints ja corrigidos
                  -> Fase 9  (Specification) — refatora o service ja com DTOs prontos
                      -> Fase 8  (paginacao) — atualiza signatures com Page<T>

Fase 6  (DI) — independente, pode ser feita a qualquer momento
Fase 7  (enum) — independente, pode ser feita a qualquer momento
Fase 10 (@Transactional) — independente
Fase 11 (modelo) — independente, mas feita antes de Fase 5 evita retrabalho no DTO
Fase 12 (shopify) — independente
Fase 13 (hikari) — independente
Fase 14 (versionamento) — feita por ultimo evita atualizar paths multiplas vezes
Fase 15 (swagger) — feita por ultimo quando os endpoints ja estao finais
Fase 16 (async) — independente, opcional
```

---

*Gerado em 2026-07-13 com base na analise do codigo fonte da versao atual.*