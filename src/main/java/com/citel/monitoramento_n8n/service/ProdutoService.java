package com.citel.monitoramento_n8n.service;


import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProdutoService {


    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {

        this.repository = repository;
    }
    public Produto registrarProduto(ProdutoDTO produtoDTO, String idInt) {

       Optional<Produto> produtoComErro =  repository.findByCodigoProdutoAndClienteAndRotina(produtoDTO.codigoProduto(), produtoDTO.cliente(), produtoDTO.rotina())
               .stream().findFirst();

        if (produtoComErro.isEmpty()) {

            Produto novoProduto = new Produto();
            novoProduto.setStatus(0);
            novoProduto.setCodigoProduto(produtoDTO.codigoProduto());
            novoProduto.setErro(produtoDTO.mensagemErro());
            novoProduto.setCliente(produtoDTO.cliente());
            novoProduto.setPlataforma(produtoDTO.plataforma());
            novoProduto.setIdIntegracao(idInt);   // vem do cliente autenticado (CADCLI.CLI_CODAUT)
            novoProduto.setRotina(produtoDTO.rotina());
            return repository.save(novoProduto);
        }
        else {
            return produtoComErro.get();
        }
    }


    public List<Produto> registrarProdutosList(List<ProdutoLoteDTO> listaProdutos, String idInt) {
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
                        (a, b) -> a));

        List<Produto> listaPro = new ArrayList<>();
        for (ProdutoLoteDTO dto : listaProdutos) {
            Produto existente = existentesPorChave.get(chave(dto.getCliente(), dto.getCodigoProduto(), dto.getRotina()));
            boolean novo = existente == null;

            Produto pro = ProdutoLoteDTO.converterDTO(dto, novo ? new Produto() : existente);
            if (novo) {
                pro.setStatus(0);   // só define status quando é novo
            }
            pro.setIdIntegracao(idInt);   // vem do cliente autenticado (CADCLI.CLI_CODAUT)

            log.info(novo ? "Produto criado - {}" : "Produto atualizado - {}", dto.getCodigoProduto());
            listaPro.add(pro);
        }

        return repository.saveAll(listaPro);
    }


    public List<Produto> retornarProdutosPendentes(String codigoProduto, String cliente, String idIntegracao) {
        log.info("🔍 Buscando Produtos - Cliente: {}, Código: {}", cliente, codigoProduto);
        return repository.buscarPendentes(codigoProduto, cliente, idIntegracao);
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
