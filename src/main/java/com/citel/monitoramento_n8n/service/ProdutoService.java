package com.citel.monitoramento_n8n.service;


import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProdutoService {


    /** Todo registro nasce com uma tentativa: o POST que o criou. */
    private static final int PRIMEIRA_TENTATIVA = 1;

    private final ProdutoRepository repository;
    private final IntegracaoService integracaoService;

    public ProdutoService(ProdutoRepository repository, IntegracaoService integracaoService) {

        this.repository = repository;
        this.integracaoService = integracaoService;
    }
    public Produto registrarProduto(ProdutoDTO produtoDTO, Long codigoCliente) {

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
            // Vem do payload e é conferido contra a CADINT do lojista autenticado.
            // Omitido, fica nulo — o CADCLI.CLI_CODAUT não serve mais como origem: ele é um
            // valor por lojista e não distingue as N integrações que um lojista pode ter.
            novoProduto.setIdIntegracao(
                    integracaoService.resolverCodigoIntegracao(produtoDTO.idIntegracao(), codigoCliente));
            novoProduto.setRotina(produtoDTO.rotina());
            return repository.save(novoProduto);
        }
        else {
            // Mesmo codigoProduto + cliente + rotina: o erro se repetiu, então só contabiliza
            // a tentativa. Os demais campos do registro existente ficam como estão.
            Produto existente = produtoComErro.get();
            existente.incrementarTentativa();
            log.info("Produto {} (cliente {}, rotina {}) já registrado - tentativa {}",
                    existente.getCodigoProduto(), existente.getCliente(), existente.getRotina(),
                    existente.getTentativa());
            return repository.save(existente);
        }
    }


    public List<Produto> registrarProdutosList(List<ProdutoLoteDTO> listaProdutos, Long codigoCliente) {
        List<String> clientes = listaProdutos.stream()
                .map(ProdutoLoteDTO::getCliente).distinct().toList();
        List<String> codigos = listaProdutos.stream()
                .map(ProdutoLoteDTO::getCodigoProduto).distinct().toList();

        // Valida todos os códigos de integração do lote numa query só, antes de gravar qualquer
        // coisa: se um deles não for do lojista, o lote inteiro é recusado.
        integracaoService.resolverCodigosIntegracao(
                listaProdutos.stream().map(ProdutoLoteDTO::getIdIntegracao).toList(), codigoCliente);

        // Busca todos os existentes numa única query (evita N+1) e indexa por cliente|codigoProduto|rotina
        Map<String, Produto> existentesPorChave = repository
                .findByClienteInAndCodigoProdutoIn(clientes, codigos)
                .stream()
                .collect(Collectors.toMap(
                        p -> chave(p.getCliente(), p.getCodigoProduto(), p.getRotina()),
                        p -> p,
                        (a, b) -> a));

        List<Produto> listaPro = new ArrayList<>();
        for (ProdutoLoteDTO dto : listaProdutos) {
            Produto existente = existentesPorChave.get(chave(dto.getCliente(), dto.getCodigoProduto(), dto.getRotina()));
            boolean novo = existente == null;

            Produto pro = ProdutoLoteDTO.converterDTO(dto, novo ? new Produto() : existente);
            if (novo) {
                pro.setStatus(0);   // só define status quando é novo
                pro.setTentativa(PRIMEIRA_TENTATIVA);
            } else {
                pro.incrementarTentativa();
            }
            // Já validado acima; omitido, fica nulo.
            pro.setIdIntegracao(StringUtils.hasText(dto.getIdIntegracao()) ? dto.getIdIntegracao().trim() : null);

            log.info(novo ? "Produto criado - {} (tentativa {})" : "Produto atualizado - {} (tentativa {})",
                    dto.getCodigoProduto(), pro.getTentativa());
            listaPro.add(pro);
        }

        return repository.saveAll(listaPro);
    }


    /**
     * Lista os produtos pendentes (status 0). As duas pontas do filtro de tentativas são
     * exclusivas e opcionais: {@code tentativaMaiorQue = 5} traz quem tem 6 ou mais,
     * {@code tentativaMenorQue = 5} traz quem tem 4 ou menos, as duas juntas delimitam uma
     * faixa e nenhuma delas = sem filtro.
     */
    public List<Produto> retornarProdutosPendentes(String codigoProduto, String cliente, String idIntegracao,
                                                   Integer tentativaMaiorQue, Integer tentativaMenorQue) {
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

        log.info("🔍 Buscando Produtos - Cliente: {}, Código: {}, Tentativas: >{} e <{}",
                cliente, codigoProduto, tentativaMaiorQue, tentativaMenorQue);
        return repository.buscarPendentes(codigoProduto, cliente, idIntegracao,
                tentativaMaiorQue, tentativaMenorQue);
    }


    public Optional<Produto> registraComoResolvido(String codigoProduto, String cliente, String rotina, int status, String erro) {
        return repository.findByCodigoProdutoAndClienteAndRotina(codigoProduto, cliente, rotina)
                .stream().findFirst()
                .map(produto -> {
                    produto.setStatus(status);
                    produto.setErro(erro);
                    return repository.save(produto);
                });
    }

    private static String chave(String cliente, String codigoProduto, String rotina) {
        return cliente + "|" + codigoProduto + "|" + rotina;
    }


}
