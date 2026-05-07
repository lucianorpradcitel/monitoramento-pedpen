package com.citel.monitoramento_n8n.service;


import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProdutoService {


    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {

        this.repository = repository;
    }
    public Produto registrarProduto(ProdutoDTO produtoDTO) {

       Optional<Produto> produtoComErro =  repository.findByCodigoProdutoAndClienteAndMensagemErro(produtoDTO.codigoProduto(), produtoDTO.cliente(), produtoDTO.mensagemErro());

        if (produtoComErro.isEmpty()) {

            Produto novoProduto = new Produto();
            novoProduto.setStatus(0);
            novoProduto.setCodigoProduto(produtoDTO.codigoProduto());
            novoProduto.setErro(produtoDTO.mensagemErro());
            novoProduto.setCliente(produtoDTO.cliente());
            novoProduto.setPlataforma(produtoDTO.plataforma());
            return repository.save(novoProduto);
        }
        else {
            return produtoComErro.get();
        }
    }




    public List<Produto> retornarProdutosPendentes(String codigoProduto, String cliente) {
        log.info("🔍 Buscando Produtos - Cliente: {}, Código: {}",
                cliente, codigoProduto);

        if (codigoProduto == null && cliente == null)
        {
            return repository.findByStatus(0);
        }
        else if (codigoProduto == null && cliente != null)
        {
            return repository.findByCliente(cliente);
        }
        else {

            return repository.findByCodigoProdutoAndClienteAndStatus(codigoProduto, cliente, 0);
        }
    }
    public Optional<Produto> registraComoResolvido(String codigoProduto, String cliente, int status, String erro) {
        return repository.findByCodigoProdutoAndCliente(codigoProduto, cliente)
                .map(produto -> {
                    produto.setStatus(status);
                    produto.setErro(erro);
                    return repository.save(produto);
                });
    }


}
