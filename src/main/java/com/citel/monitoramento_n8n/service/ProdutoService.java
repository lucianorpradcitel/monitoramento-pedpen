package com.citel.monitoramento_n8n.service;


import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {


    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {

        this.repository = repository;
    }
    public Produto registrarProduto(ProdutoDTO produtoDTO) {
        Produto novoProduto = new Produto();
        novoProduto.setStatus(0);
        novoProduto.setCodigoProduto(produtoDTO.codigoProduto());
        novoProduto.setErro(produtoDTO.mensagemErro());
        novoProduto.setCliente(produtoDTO.cliente());
        novoProduto.setPlataforma(produtoDTO.plataforma());
        return repository.save(novoProduto);
    }


    public List<Produto> retornarProdutosPendentes() {

        return repository.findByStatus(0);
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
