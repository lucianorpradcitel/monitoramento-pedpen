package com.citel.monitoramento_n8n.service;


import com.citel.monitoramento_n8n.DTO.ProdutoAtzDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.model.ProdutoAtz;
import com.citel.monitoramento_n8n.repository.ProdutoAtzRepository;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProdutoAtzService {
    private final ProdutoAtzRepository repository;

    public ProdutoAtzService(ProdutoAtzRepository repository) {
        this.repository = repository;
    }


    public ProdutoAtz registrarAtzProduto(ProdutoAtzDTO produtoAtzDTO) {

        Optional<ProdutoAtz> produtoAtualizado =  repository.findByCoditeAndNomeEmpresa(produtoAtzDTO.codite(), produtoAtzDTO.nomeEmpresa());

        if (produtoAtualizado.isEmpty()) {

            ProdutoAtz novoProdutoAtualizado = new ProdutoAtz();
            novoProdutoAtualizado.setNomeEmpresa(produtoAtzDTO.nomeEmpresa());
            novoProdutoAtualizado.setNomePlataforma(produtoAtzDTO.nomePlataforma());
            novoProdutoAtualizado.setCodite(produtoAtzDTO.codite());
            novoProdutoAtualizado.setIdPlataforma(produtoAtzDTO.idPlataforma());
            novoProdutoAtualizado.setAcrescimoPreco(produtoAtzDTO.acrescimoPreco());
            novoProdutoAtualizado.setVariacaoEstoque(produtoAtzDTO.variacaoEstoque());
            novoProdutoAtualizado.setValorRecebido(produtoAtzDTO.valorRecebido());
            novoProdutoAtualizado.setValorEnviado(produtoAtzDTO.valorEnviado());
            novoProdutoAtualizado.setCodigoResposta(produtoAtzDTO.codigoResposta());
            novoProdutoAtualizado.setTipoEnvio(produtoAtzDTO.tipoEnvio());
            return repository.save(novoProdutoAtualizado);
        }
        else {
            return produtoAtualizado.get();
        }
    }

}
