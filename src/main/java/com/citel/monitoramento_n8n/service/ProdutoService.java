package com.citel.monitoramento_n8n.service;


import com.citel.monitoramento_n8n.DTO.FiltrosProdutosDTO;
import com.citel.monitoramento_n8n.DTO.PaginaDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProdutoService {


    /** Todo registro nasce com uma tentativa: o POST que o criou. */
    private static final int PRIMEIRA_TENTATIVA = 1;

    /** Registros por página que o GET /produtos aceita, além de {@link #TAMANHO_TODOS}. */
    private static final Set<Integer> TAMANHOS_PERMITIDOS = Set.of(50, 100, 1000);

    /** {@code tamanho=0}: uma única página com todos os registros que atendem aos filtros. */
    private static final int TAMANHO_TODOS = 0;

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {

        this.repository = repository;
    }
    public Produto registrarProduto(ProdutoDTO produtoDTO) {

       Optional<Produto> produtoComErro =  repository.findByCodigoProdutoAndClienteAndRotina(produtoDTO.codigoProduto(), produtoDTO.cliente(), produtoDTO.rotina())
               .stream().findFirst();

        if (produtoComErro.isEmpty()) {

            Produto novoProduto = new Produto();
            novoProduto.setStatus(0);
            novoProduto.setTentativa(PRIMEIRA_TENTATIVA);
            novoProduto.setCodigoProduto(produtoDTO.codigoProduto());
            novoProduto.setErro(produtoDTO.mensagemErro());
            novoProduto.setCliente(produtoDTO.cliente());
            novoProduto.setPlataforma(produtoDTO.plataforma());
            // Gravado como veio, sem conferir na CADINT: o n8n autentica com o login de serviço,
            // que não é o dono das integrações. Omitido, fica nulo.
            novoProduto.setIdIntegracao(normalizarIdIntegracao(produtoDTO.idIntegracao()));
            novoProduto.setRotina(produtoDTO.rotina());
            novoProduto.setCodigoMensagem(produtoDTO.codigoMensagem());
            return repository.save(novoProduto);
        }
        else {
            // Mesmo codigoProduto + cliente + rotina: o erro se repetiu. Contabiliza a tentativa e
            // troca a mensagem pela mais recente - o motivo da falha pode mudar entre um envio e
            // outro (ex.: "corrija o peso" vira "limite do plano atingido"). Os demais campos
            // do registro existente ficam como estão.
            Produto existente = produtoComErro.get();
            existente.incrementarTentativa();

            // Mensagem vazia no payload mantém a gravada. O tipo acompanha a mensagem: mensagem nova
            // troca o tipo pelo enviado (omitido = sem tipo, porque o antigo pode não valer mais).
            // Sem mensagem, o tipo só muda se vier um.
            if (StringUtils.hasText(produtoDTO.mensagemErro())) {
                existente.setErro(produtoDTO.mensagemErro());
                existente.setCodigoMensagem(produtoDTO.codigoMensagem());
            } else if (produtoDTO.codigoMensagem() != null) {
                existente.setCodigoMensagem(produtoDTO.codigoMensagem());
            }

            log.info("Produto {} (cliente {}, rotina {}) já registrado - tentativa {}",
                    existente.getCodigoProduto(), existente.getCliente(), existente.getRotina(),
                    existente.getTentativa());
            return repository.save(existente);
        }
    }


    public List<Produto> registrarProdutosList(List<ProdutoLoteDTO> listaProdutos) {
        // Status fora de 0/1 recusa o lote inteiro antes de gravar qualquer coisa.
        listaProdutos.stream()
                .map(ProdutoLoteDTO::getStatus)
                .filter(Objects::nonNull)
                .forEach(ProdutoService::validarStatus);

        List<String> clientes = listaProdutos.stream()
                .map(ProdutoLoteDTO::getCliente).distinct().toList();
        List<String> codigos = listaProdutos.stream()
                .map(ProdutoLoteDTO::getCodigoProduto).distinct().toList();

        // Busca todos os existentes numa única query (evita N+1) e indexa por cliente|codigoProduto|rotina
        Map<String, Produto> existentesPorChave = repository
                .findByClienteInAndCodigoProdutoIn(clientes, codigos)
                .stream()
                .collect(Collectors.toMap(
                        p -> chave(p.getCliente(), p.getCodigoProduto(), p.getRotina()),
                        p -> p,
                        (a, b) -> a,
                        HashMap::new));

        // Indexado pela chave, e não uma lista: o mesmo produto repetido no lote cai na mesma
        // linha em vez de virar duas.
        Map<String, Produto> paraGravar = new LinkedHashMap<>();
        for (ProdutoLoteDTO dto : listaProdutos) {
            String chave = chave(dto.getCliente(), dto.getCodigoProduto(), dto.getRotina());
            Produto existente = existentesPorChave.get(chave);

            if (dto.getStatus() != null) {
                // Item do monitor: só o status muda. Não cria produto, não soma tentativa e não
                // mexe na mensagem nem no código de integração.
                if (existente == null) {
                    log.warn("Produto {} (cliente {}, rotina {}) não encontrado - status {} ignorado",
                            dto.getCodigoProduto(), dto.getCliente(), dto.getRotina(), dto.getStatus());
                    continue;
                }
                existente.setStatus(dto.getStatus());
                log.info("Produto {} (cliente {}, rotina {}) - status {}",
                        dto.getCodigoProduto(), dto.getCliente(), dto.getRotina(), dto.getStatus());
                paraGravar.put(chave, existente);
                continue;
            }

            boolean novo = existente == null;
            Produto pro = ProdutoLoteDTO.converterDTO(dto, novo ? new Produto() : existente);
            if (novo) {
                pro.setStatus(0);   // só define status quando é novo
                pro.setTentativa(PRIMEIRA_TENTATIVA);
                // A próxima ocorrência da mesma chave no lote atualiza este, em vez de criar outro.
                existentesPorChave.put(chave, pro);
            } else {
                pro.incrementarTentativa();
            }
            // Gravado como veio, sem conferir na CADINT: o n8n autentica o lote com o login de
            // serviço, que não é o dono das integrações. Omitido, fica nulo.
            pro.setIdIntegracao(normalizarIdIntegracao(dto.getIdIntegracao()));

            log.info(novo ? "Produto criado - {} (tentativa {})" : "Produto atualizado - {} (tentativa {})",
                    dto.getCodigoProduto(), pro.getTentativa());
            paraGravar.put(chave, pro);
        }

        return repository.saveAll(new ArrayList<>(paraGravar.values()));
    }


    /**
     * Lista uma página dos produtos no PRO_STATUS pedido: 0 (pendente, o padrão do endpoint) ou 1.
     * As duas pontas do filtro de tentativas são exclusivas e opcionais: {@code tentativaMaiorQue = 5}
     * traz quem tem 6 ou mais, {@code tentativaMenorQue = 5} traz quem tem 4 ou menos, as duas
     * juntas delimitam uma faixa e nenhuma delas = sem filtro. O período filtra a data do erro
     * (PRO_DTAERR) e as duas pontas são inclusivas e opcionais. {@code responsabilidades} filtra pela
     * responsabilidade do tipo do erro (0 a 4; erro sem tipo vale 0); nulo ou vazio = todas.
     *
     * <p>Ordena pela data do erro e desempata pelo id, para que registros com o mesmo instante não
     * troquem de página entre uma consulta e a seguinte. A página começa em 1; uma página além da
     * última volta vazia, com os totais corretos, para o chamador saber onde está.
     */
    public PaginaDTO<Produto> retornarProdutos(int status, String codigoProduto, String cliente, String idIntegracao,
                                               Integer tentativaMaiorQue, Integer tentativaMenorQue,
                                               LocalDateTime dataInicial, LocalDateTime dataFinal,
                                               String plataforma, String produtoContem, String erroContem,
                                               List<Integer> responsabilidades,
                                               int pagina, int tamanho, String ordem) {
        // Recusar um status fora de 0/1 é melhor que devolver lista vazia, que se confunde com
        // "não há produtos nesse status".
        validarStatus(status);
        // Sem o parâmetro vale a lista inteira (0 a 4); fora de 0 a 4 é recusado.
        List<Integer> responsabilidadesFiltro = ResponsabilidadeErro.validar(responsabilidades);
        // Com as duas pontas exclusivas, precisa sobrar pelo menos um inteiro no meio: menorQue
        // tem de ser no mínimo maiorQue + 2. Recusar é melhor que devolver lista vazia, que se
        // confunde com "não há produtos nessa faixa".
        if (tentativaMaiorQue != null && tentativaMenorQue != null
                && tentativaMenorQue <= tentativaMaiorQue + 1) {
            throw new BusinessException(
                    "Nenhum valor satisfaz tentativa > " + tentativaMaiorQue
                            + " e tentativa < " + tentativaMenorQue
                            + ": as duas pontas são exclusivas, então tentativaMenorQue precisa"
                            + " ser ao menos tentativaMaiorQue + 2");
        }

        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            throw new BusinessException(
                    "dataInicial (" + dataInicial + ") não pode ser depois de dataFinal (" + dataFinal + ")");
        }
        if (pagina < 1) {
            throw new BusinessException("pagina deve ser 1 ou maior, mas veio " + pagina);
        }
        boolean todos = tamanho == TAMANHO_TODOS;
        if (!todos && !TAMANHOS_PERMITIDOS.contains(tamanho)) {
            throw new BusinessException("tamanho deve ser 50, 100, 1000 ou 0 (todos), mas veio " + tamanho);
        }
        Sort.Direction direcao = direcaoDe(ordem);

        log.info("🔍 Buscando Produtos - Status: {}, Cliente: {}, Código: {}, Tentativas: >{} e <{}, Período: {} a {}, Plataforma: {}, Contém: produto={} erro={}, Página: {} ({}), Ordem: {}",
                status, cliente, codigoProduto, tentativaMaiorQue, tentativaMenorQue,
                dataInicial, dataFinal, plataforma, produtoContem, erroContem,
                pagina, tamanho, direcao);
        // "Todos" é a página 1 sem limite: o Spring Data não faz COUNT quando a página não enche.
        Pageable pageable = todos
                ? PageRequest.of(0, Integer.MAX_VALUE, Sort.by(direcao, "dataErro", "id"))
                : PageRequest.of(pagina - 1, tamanho, Sort.by(direcao, "dataErro", "id"));
        PaginaDTO<Produto> resultado = PaginaDTO.de(repository.buscar(status, codigoProduto, cliente, idIntegracao,
                tentativaMaiorQue, tentativaMenorQue, paraData(dataInicial), paraData(dataFinal),
                StringUtils.hasText(plataforma) ? plataforma : null,
                paraPadraoLike(produtoContem), paraPadraoLike(erroContem), responsabilidadesFiltro, pageable));
        return todos
                ? new PaginaDTO<>(resultado.itens(), 1, TAMANHO_TODOS, resultado.totalItens(), resultado.totalPaginas())
                : resultado;
    }

    /**
     * Texto da pesquisa pronto para o LIKE da consulta: em branco vira nulo (= sem filtro) e os
     * curingas do LIKE ({@code %}, {@code _}) e o próprio escape ({@code !}) são escapados, para que
     * "ABC_1" procure exatamente "ABC_1" e não "ABC" + qualquer letra + "1".
     */
    private static String paraPadraoLike(String texto) {
        if (!StringUtils.hasText(texto)) {
            return null;
        }
        return texto.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * Clientes e plataformas que têm produto no status pedido (0 ou 1), para a tela montar os filtros
     * sem baixar a lista de produtos. Só traz o que existe naquele momento.
     *
     * <p>Os dois filtros se restringem um ao outro: com {@code plataforma}, os clientes são só os que
     * têm produto nela; com {@code cliente}, as plataformas são só as em que ele tem produto. Cada
     * lista ignora o próprio filtro, para a escolha atual não esconder as demais opções.
     *
     * <p>{@code responsabilidades} (0 a 4; nulo ou vazio = todas) restringe às opções de quem tem produto
     * com erro dessas responsabilidades: sem isso a tela de uma visão listaria clientes que nela não têm nada.
     */
    public FiltrosProdutosDTO retornarFiltros(int status, String cliente, String plataforma,
                                              List<Integer> responsabilidades) {
        validarStatus(status);
        List<Integer> responsabilidadesFiltro = ResponsabilidadeErro.validar(responsabilidades);
        String clienteEscolhido = StringUtils.hasText(cliente) ? cliente : null;
        String plataformaEscolhida = StringUtils.hasText(plataforma) ? plataforma : null;
        return new FiltrosProdutosDTO(
                repository.clientesComProduto(status, plataformaEscolhida, responsabilidadesFiltro),
                repository.plataformasComProduto(status, clienteEscolhido, responsabilidadesFiltro));
    }

    /** "desc" (padrão, mais recente primeiro) ou "asc" (mais antigo primeiro), sem distinguir caixa. */
    private static Sort.Direction direcaoDe(String ordem) {
        if (!StringUtils.hasText(ordem)) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.fromOptionalString(ordem.trim())
                .orElseThrow(() -> new BusinessException("ordem deve ser asc ou desc, mas veio " + ordem));
    }

    /** O banco guarda PRO_DTAERR no fuso do servidor, o mesmo em que o JDBC lê a data. */
    private static Date paraData(LocalDateTime data) {
        return data == null ? null : Timestamp.valueOf(data);
    }


    /**
     * Remove o produto do monitoramento. A chave é codigoProduto + cliente + idIntegracao e,
     * opcionalmente, a rotina:
     * <ul>
     *   <li>rotina omitida (nula): o produto sai de TODAS as rotinas numa chamada só - o
     *       comportamento original, usado pelo varredor das 5 tentativas;</li>
     *   <li>rotina informada: sai só a linha daquela rotina, preservando o erro de outra rotina
     *       do mesmo produto que ainda não foi resolvido.</li>
     * </ul>
     * Rotina presente mas em branco é recusada: quase sempre é expressão do n8n que avaliou
     * vazia, e cair no "apaga tudo" levaria junto justamente os erros que o filtro quer poupar.
     *
     * @return quantas linhas foram apagadas; 0 quando nada casou com a chave
     */
    @Transactional
    public int removerProduto(String codigoProduto, String cliente, String idIntegracao, String rotina) {
        if (rotina != null && !StringUtils.hasText(rotina)) {
            throw new BusinessException(
                    "rotina foi enviada em branco: omita o parâmetro para remover o produto de"
                            + " todas as rotinas, ou informe a rotina a remover");
        }

        int removidos = rotina == null
                ? repository.removerTodasAsRotinas(codigoProduto, cliente, idIntegracao)
                : repository.removerDaRotina(codigoProduto, cliente, idIntegracao, rotina);
        log.info("🗑️ Removido produto {} (cliente {}, integração {}, rotina {}) - {} linha(s)",
                codigoProduto, cliente, idIntegracao, rotina == null ? "todas" : rotina, removidos);
        return removidos;
    }

    /**
     * Liga o erro ao tipo dele (PRO_CODMSG) sem contar tentativa nem tocar na mensagem. É o passo
     * final do n8n: grava o erro (POST /produtos), classifica (SUB_Error_Status) e liga o tipo.
     * Nulo desliga.
     */
    @Transactional
    public Produto definirTipoErro(String id, Integer codigoMensagem) {
        Produto produto = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado: " + id));
        produto.setCodigoMensagem(codigoMensagem);
        log.info("Produto {} (cliente {}, rotina {}) - tipo de erro {}",
                produto.getCodigoProduto(), produto.getCliente(), produto.getRotina(), codigoMensagem);
        return repository.save(produto);
    }

    /** PRO_STATUS que a API aceita: 0 (pendente) e 1. */
    private static void validarStatus(int status) {
        if (status != 0 && status != 1) {
            throw new BusinessException("status deve ser 0 ou 1, mas veio " + status);
        }
    }

    private static String chave(String cliente, String codigoProduto, String rotina) {
        return cliente + "|" + codigoProduto + "|" + rotina;
    }

    /** PRO_ID_INT como veio no payload, sem espaços; vazio ou omitido vira nulo. */
    private static String normalizarIdIntegracao(String idIntegracao) {
        return StringUtils.hasText(idIntegracao) ? idIntegracao.trim() : null;
    }


}
