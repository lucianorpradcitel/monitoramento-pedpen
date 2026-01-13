package com.citel.monitoramento_n8n.service;


import com.citel.monitoramento_n8n.model.PontuacaoPendente;
import com.citel.monitoramento_n8n.repository.PontuacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PontuacaoPendenteService {

    private final PontuacaoRepository repository;

    public PontuacaoPendenteService(PontuacaoRepository repository) {

        this.repository = repository;
    }


    public List<PontuacaoPendente> retornarPontuacoes()
    {
        return  repository.findByStatus(0);

    }


    public PontuacaoPendente registrarPontuacao(PontuacaoPendente pontuacao) {

        Optional<PontuacaoPendente> pontuacaoPendente = repository.findByNumeroDocumentoAndNomeCliente(pontuacao.getNumeroDocumento(), pontuacao.getNomeCliente());


        if (pontuacaoPendente.isEmpty())
        {
            PontuacaoPendente pontuacaoNaoEnviada = new PontuacaoPendente();
            pontuacaoNaoEnviada.setStatus(0);
            pontuacaoNaoEnviada.setNumeroDocumento(pontuacao.getNumeroDocumento());
            pontuacaoNaoEnviada.setMsgErr(pontuacao.getMsgErr());
            pontuacaoNaoEnviada.setNomeCliente(pontuacao.getNomeCliente());
            pontuacaoNaoEnviada.setJsonEnviado(pontuacao.getJsonEnviado());
            return repository.save(pontuacaoNaoEnviada);
        }

        else {

            return pontuacaoPendente.get();
        }

    }



}
