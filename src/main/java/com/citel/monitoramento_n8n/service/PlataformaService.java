package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.DadosCriacaoPlataforma;
import com.citel.monitoramento_n8n.DTO.PlataformaResumoDTO;
import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.model.Plataforma;
import com.citel.monitoramento_n8n.repository.PlataformaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlataformaService {

    private final PlataformaRepository plataformaRepository;

    public PlataformaService(PlataformaRepository plataformaRepository) {
        this.plataformaRepository = plataformaRepository;
    }

    /**
     * PLA_SISEXT é atribuído pelo request, não pelo banco: o save() do Spring Data faz merge
     * (UPDATE) silencioso quando o Id já existe, em vez de estourar chave duplicada. Por isso o
     * existsById() explícito antes de gravar.
     */
    public PlataformaResumoDTO criar(DadosCriacaoPlataforma dados) {
        if (plataformaRepository.existsById(dados.sistemaExterno())) {
            throw new ConflictException("Já existe plataforma cadastrada com o sistemaExterno '" + dados.sistemaExterno() + "'");
        }

        if (plataformaRepository.existsByDescricaoIgnoreCase(dados.descricao())) {
            throw new ConflictException("Já existe plataforma cadastrada com a descrição '" + dados.descricao() + "'");
        }

        Plataforma nova = new Plataforma(dados.descricao(), dados.sistemaExterno());
        return PlataformaResumoDTO.de(plataformaRepository.save(nova));
    }

    @Transactional(readOnly = true)
    public List<PlataformaResumoDTO> listar() {
        return plataformaRepository.findAllByOrderByDescricaoAsc().stream()
                .map(PlataformaResumoDTO::de)
                .toList();
    }
}
