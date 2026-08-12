package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.AtualizacaoTokensDTO;
import com.citel.monitoramento_n8n.DTO.DadosCriacaoIntegracao;
import com.citel.monitoramento_n8n.DTO.IntegracaoContextoDTO;
import com.citel.monitoramento_n8n.DTO.IntegracaoCriadaDTO;
import com.citel.monitoramento_n8n.DTO.IntegracaoResumoDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.exception.UnauthorizedException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Integracao;
import com.citel.monitoramento_n8n.model.IntegracaoId;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.IntegracaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry de tenants da integração multi-tenant.
 *
 * As leituras são @Transactional(readOnly = true) porque spring.jpa.open-in-view=false: o
 * nomeCliente vem de um @ManyToOne lazy e montar o DTO fora da transação estoura
 * LazyInitializationException.
 */
@Slf4j
@Service
public class IntegracaoService {

    /**
     * MySQL 5.7 aceita a sintaxe de CHECK mas não aplica a restrição, então a lista fechada de
     * plataformas tem de ser garantida aqui.
     */
    private static final Set<String> PLATAFORMAS_SUPORTADAS = Set.of("tray", "mercos");

    private static final String PREFIXO_WEBHOOK_TOKEN = "ct_";
    private static final int BYTES_WEBHOOK_TOKEN = 32;

    private static final String ATIVO = "S";

    private final IntegracaoRepository repository;
    private final ClienteRepository clienteRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public IntegracaoService(IntegracaoRepository repository, ClienteRepository clienteRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public IntegracaoCriadaDTO criar(DadosCriacaoIntegracao dados) {
        String plataforma = normalizar(dados.plataforma());
        validarPlataforma(plataforma);

        if (repository.existsById(new IntegracaoId(dados.codigoIntegracao(), dados.codigoCliente()))) {
            throw new ConflictException("Já existe integração cadastrada com o código "
                    + dados.codigoIntegracao() + " para o lojista " + dados.codigoCliente());
        }

        if (repository.existsBySlugAndPlataforma(dados.slug(), plataforma)) {
            throw new ConflictException("Já existe integração com o slug '" + dados.slug() + "' na plataforma " + plataforma);
        }

        Cliente cliente = clienteRepository.findById(dados.codigoCliente())
                .orElseThrow(() -> new BusinessException(
                        "Não há lojista cadastrado com o código " + dados.codigoCliente()
                                + ". Cadastre o lojista em POST /cadastro antes de criar a integração."));

        Integracao integracao = new Integracao();
        integracao.setCodigoIntegracao(dados.codigoIntegracao());
        integracao.setCodigoCliente(cliente.getId());
        integracao.setPlataforma(plataforma);
        integracao.setSlug(dados.slug());
        integracao.setWebhookToken(gerarWebhookToken());
        integracao.setAtivo(ATIVO);
        integracao.setUrlApi(dados.urlApi());
        integracao.setUrlWebservice(dados.urlWebservice());
        // Sem trim e sem normalização: reformatar o PEM quebra a assinatura JWT RS256.
        integracao.setChavePrivada(dados.chavePrivada());
        integracao.setDataInclusao(LocalDateTime.now());
        // apiToken e refreshToken ficam nulos de propósito: quem preenche é o OAuth, no PATCH /tokens.

        Integracao salva = repository.save(integracao);
        // A associação é read-only (a coluna pertence à chave), então preenchemos em memória para
        // o DTO montar o nomeCliente sem uma segunda ida ao banco.
        salva.setCliente(cliente);

        log.info("Integração criada - código: {}, lojista: {} ({}), slug: {}, plataforma: {}",
                salva.getCodigoIntegracao(), cliente.getId(), cliente.getNome(), salva.getSlug(), salva.getPlataforma());

        return IntegracaoCriadaDTO.de(salva);
    }

    /**
     * O MASTER_Context_Loader do n8n. Devolve a chave RSA, então a ordem das checagens importa:
     * token ausente derruba a requisição antes de qualquer query.
     */
    @Transactional(readOnly = true)
    public IntegracaoContextoDTO buscarContexto(String slug, String plataforma, String webhookToken) {
        if (!StringUtils.hasText(webhookToken)) {
            log.warn("Tentativa de leitura de contexto sem o header x-ct-token - slug: {}", slug);
            throw new UnauthorizedException("Header x-ct-token é obrigatório");
        }

        Integracao integracao = repository.findBySlugAndPlataforma(slug, normalizar(plataforma))
                .orElseThrow(() -> new NotFoundException(
                        "Integração não encontrada para o slug '" + slug + "' na plataforma " + plataforma));

        if (!tokenConfere(webhookToken, integracao.getWebhookToken())) {
            log.warn("Token de webhook divergente - slug: {}, plataforma: {}", slug, plataforma);
            throw new UnauthorizedException("Token de webhook inválido");
        }

        if (!integracao.isAtivo()) {
            log.warn("Contexto solicitado para integração inativa - slug: {}", slug);
            throw new NotFoundException(
                    "Integração não encontrada para o slug '" + slug + "' na plataforma " + plataforma);
        }

        return IntegracaoContextoDTO.de(integracao);
    }

    /**
     * Os 6 pais de fan-out do n8n usam mode "each" com mappingMode "passthrough": passam a linha
     * inteira ao workflow filho, que espera as credenciais ali. Daí a flag incluirCredenciais.
     * O default sem segredo é de propósito.
     */
    @Transactional(readOnly = true)
    public List<IntegracaoResumoDTO> listarResumo(String plataforma, String ativo) {
        return buscar(plataforma, ativo).stream()
                .map(IntegracaoResumoDTO::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IntegracaoContextoDTO> listarComCredenciais(String plataforma, String ativo) {
        List<Integracao> integracoes = buscar(plataforma, ativo);
        log.info("Contexto completo (com credenciais) solicitado para {} integração(ões) - plataforma: {}",
                integracoes.size(), plataforma);
        return integracoes.stream()
                .map(IntegracaoContextoDTO::de)
                .toList();
    }

    /**
     * Os fluxos ADMIN_Tray_Token_*. Campo nulo ou vazio preserva o que já está no banco — o
     * Token_Refresh manda só os dois tokens, e apagar um refreshToken por engano derruba a
     * integração até o próximo OAuth manual.
     */
    @Transactional
    public IntegracaoResumoDTO atualizarTokens(String codigoIntegracao, Long codigoCliente, AtualizacaoTokensDTO dados) {
        Integracao integracao = repository.findByCodigoIntegracaoAndCodigoCliente(codigoIntegracao, codigoCliente)
                .orElseThrow(() -> new NotFoundException("Integração não encontrada com o código "
                        + codigoIntegracao + " para o lojista " + codigoCliente));

        if (StringUtils.hasText(dados.apiToken())) {
            integracao.setApiToken(dados.apiToken());
        }
        if (StringUtils.hasText(dados.refreshToken())) {
            integracao.setRefreshToken(dados.refreshToken());
        }
        if (StringUtils.hasText(dados.urlApi())) {
            integracao.setUrlApi(dados.urlApi());
        }

        Integracao salva = repository.save(integracao);
        log.info("Tokens atualizados - código: {}, lojista: {}, slug: {}",
                salva.getCodigoIntegracao(), salva.getCodigoCliente(), salva.getSlug());

        return IntegracaoResumoDTO.de(salva);
    }

    /**
     * Confere que o código informado no payload é de uma integração deste lojista, e devolve o
     * valor a gravar em PRO_ID_INT/PEN_ID_INT.
     *
     * Código omitido devolve null: quem ainda não manda o campo grava nulo, de propósito. Sem essa
     * checagem um lojista poderia carimbar os próprios erros com o código de outro.
     */
    @Transactional(readOnly = true)
    public String resolverCodigoIntegracao(String codigoInformado, Long codigoCliente) {
        if (!StringUtils.hasText(codigoInformado)) {
            return null;
        }

        String codigo = codigoInformado.trim();
        if (repository.filtrarCodigosDoCliente(codigoCliente, Set.of(codigo)).isEmpty()) {
            throw new BusinessException("A integração " + codigo
                    + " não existe ou não pertence ao lojista autenticado.");
        }

        return codigo;
    }

    /**
     * Versão em lote: valida todos os códigos distintos numa query só e devolve os aceitos.
     * Um lote pode misturar integrações do mesmo lojista, então a chave é por código.
     */
    @Transactional(readOnly = true)
    public Set<String> resolverCodigosIntegracao(Collection<String> codigosInformados, Long codigoCliente) {
        Set<String> codigos = codigosInformados.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());

        if (codigos.isEmpty()) {
            return Set.of();
        }

        Set<String> validos = Set.copyOf(repository.filtrarCodigosDoCliente(codigoCliente, codigos));

        if (validos.size() != codigos.size()) {
            String invalidos = codigos.stream()
                    .filter(c -> !validos.contains(c))
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new BusinessException("Integração não encontrada para o lojista autenticado: " + invalidos);
        }

        return validos;
    }

    private List<Integracao> buscar(String plataforma, String ativo) {
        String plataformaFiltro = StringUtils.hasText(plataforma) ? normalizar(plataforma) : null;
        String ativoFiltro = StringUtils.hasText(ativo) ? ativo.toUpperCase() : null;
        return repository.buscarPorFiltro(plataformaFiltro, ativoFiltro);
    }

    private void validarPlataforma(String plataforma) {
        if (!PLATAFORMAS_SUPORTADAS.contains(plataforma)) {
            throw new BusinessException("Plataforma não suportada: " + plataforma
                    + ". Valores aceitos: " + String.join(", ", PLATAFORMAS_SUPORTADAS));
        }
    }

    private String gerarWebhookToken() {
        byte[] bytes = new byte[BYTES_WEBHOOK_TOKEN];
        secureRandom.nextBytes(bytes);
        return PREFIXO_WEBHOOK_TOKEN + HexFormat.of().formatHex(bytes);
    }

    /**
     * Comparação em tempo constante. O String.equals sai no primeiro byte diferente e vaza timing
     * num endpoint que devolve chave privada RSA.
     */
    private boolean tokenConfere(String recebido, String esperado) {
        if (esperado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                recebido.getBytes(StandardCharsets.UTF_8),
                esperado.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toLowerCase();
    }
}
