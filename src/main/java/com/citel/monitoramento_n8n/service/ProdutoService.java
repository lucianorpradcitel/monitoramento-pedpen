package com.citel.monitoramento_n8n.service;


import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProdutoService {


    /** Todo registro nasce com uma tentativa: o POST que o criou. */
    private static final int PRIMEIRA_TENTATIVA = 1;

    /** Valor de PRO_LIBERA que marca o produto como fora da liberação. */
    private static final String LIBERA_REMOVIDO = "N";

    /** Carimbo anexado à mensagem de erro quando o produto sai da liberação. */
    private static final String MARCA_REMOCAO = "REMOVIDO DA LIBERACAO";

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
            String liberaNovo = normalizarLibera(produtoDTO.libera());
            novoProduto.setLibera(liberaNovo);
            // Já nasce fora da liberação: a mensagem que veio no payload é a "vigente" aqui.
            novoProduto.setErro(removidoDaLiberacao(liberaNovo)
                    ? marcarRemocao(produtoDTO.mensagemErro())
                    : produtoDTO.mensagemErro());
            novoProduto.setCliente(produtoDTO.cliente());
            novoProduto.setPlataforma(produtoDTO.plataforma());
            // Gravado como veio, sem conferir na CADINT: o n8n autentica com o login de serviço,
            // que não é o dono das integrações. Omitido, fica nulo.
            novoProduto.setIdIntegracao(normalizarIdIntegracao(produtoDTO.idIntegracao()));
            novoProduto.setRotina(produtoDTO.rotina());
            return repository.save(novoProduto);
        }
        else {
            // Mesmo codigoProduto + cliente + rotina: o erro se repetiu. Contabiliza a tentativa e
            // troca a mensagem pela mais recente - o motivo da falha pode mudar entre um envio e
            // outro (ex.: "corrija o peso" vira "limite do plano atingido"). Os demais campos
            // do registro existente ficam como estão.
            Produto existente = produtoComErro.get();
            existente.incrementarTentativa();

            // Liberação omitida no payload não mexe no que já está gravado: os POSTs que o n8n
            // já manda hoje não carregam o campo e não podem zerar a liberação de ninguém.
            String liberaPedido = normalizarLibera(produtoDTO.libera());
            if (liberaPedido != null && !liberaPedido.equals(existente.getLibera())) {
                if (removidoDaLiberacao(liberaPedido)) {
                    log.info("Produto {} (cliente {}, rotina {}) removido da liberação",
                            existente.getCodigoProduto(), existente.getCliente(), existente.getRotina());
                }
                existente.setLibera(liberaPedido);
            }

            // Mensagem vazia no payload mantém a gravada. Produto fora da liberação (agora ou de
            // antes) mantém o carimbo também na mensagem nova; marcarRemocao é idempotente, então
            // reenviar o mesmo POST não empilha a marca.
            String mensagemVigente = StringUtils.hasText(produtoDTO.mensagemErro())
                    ? produtoDTO.mensagemErro()
                    : existente.getMensagemErro();
            existente.setErro(removidoDaLiberacao(existente.getLibera())
                    ? marcarRemocao(mensagemVigente)
                    : mensagemVigente);

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
     * Lista os produtos no PRO_STATUS pedido: 0 (pendente, o padrão do endpoint) ou 1. As duas
     * pontas do filtro de tentativas são exclusivas e opcionais: {@code tentativaMaiorQue = 5}
     * traz quem tem 6 ou mais, {@code tentativaMenorQue = 5} traz quem tem 4 ou menos, as duas
     * juntas delimitam uma faixa e nenhuma delas = sem filtro.
     */
    public List<Produto> retornarProdutos(int status, String codigoProduto, String cliente, String idIntegracao,
                                          Integer tentativaMaiorQue, Integer tentativaMenorQue) {
        // Recusar um status fora de 0/1 é melhor que devolver lista vazia, que se confunde com
        // "não há produtos nesse status".
        validarStatus(status);
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

        log.info("🔍 Buscando Produtos - Status: {}, Cliente: {}, Código: {}, Tentativas: >{} e <{}",
                status, cliente, codigoProduto, tentativaMaiorQue, tentativaMenorQue);
        return repository.buscar(status, codigoProduto, cliente, idIntegracao,
                tentativaMaiorQue, tentativaMenorQue);
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

    /** PRO_STATUS que a API aceita: 0 (pendente) e 1. */
    private static void validarStatus(int status) {
        if (status != 0 && status != 1) {
            throw new BusinessException("status deve ser 0 ou 1, mas veio " + status);
        }
    }

    private static String chave(String cliente, String codigoProduto, String rotina) {
        return cliente + "|" + codigoProduto + "|" + rotina;
    }

    /**
     * Normaliza PRO_LIBERA: vazio/branco vira nulo (= não informado) e o resto sobe para
     * maiúscula, para que 'n' e 'N' gravem o mesmo valor.
     */
    private static String normalizarLibera(String libera) {
        return StringUtils.hasText(libera) ? libera.trim().toUpperCase() : null;
    }

    /** PRO_ID_INT como veio no payload, sem espaços; vazio ou omitido vira nulo. */
    private static String normalizarIdIntegracao(String idIntegracao) {
        return StringUtils.hasText(idIntegracao) ? idIntegracao.trim() : null;
    }

    private static boolean removidoDaLiberacao(String libera) {
        return LIBERA_REMOVIDO.equals(libera);
    }

    /**
     * Anexa {@value #MARCA_REMOCAO} à mensagem vigente. Idempotente: se a marca já está lá,
     * devolve a mensagem intacta, então reenviar o mesmo POST não empilha carimbos.
     */
    private static String marcarRemocao(String mensagemVigente) {
        if (!StringUtils.hasText(mensagemVigente)) {
            return MARCA_REMOCAO;
        }
        if (mensagemVigente.contains(MARCA_REMOCAO)) {
            return mensagemVigente;
        }
        return mensagemVigente + " - " + MARCA_REMOCAO;
    }


}
