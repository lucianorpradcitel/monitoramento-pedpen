package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.ClienteResumoDTO;
import com.citel.monitoramento_n8n.DTO.DadosCriacaoCliente;
import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Cliente criarCliente(DadosCriacaoCliente dados)
    {
        if (clienteRepository.findByUserName(dados.userName()) != null)
        {
            throw new ConflictException("Já existe lojista cadastrado com o usuário '" + dados.userName() + "'");
        }

        String senhaCriptografada = passwordEncoder.encode(dados.senha());

        Cliente novoCliente = new Cliente(dados.nome(), dados.userName(), senhaCriptografada);
        return clienteRepository.save(novoCliente);
    }

    /**
     * Lista lojistas para o onboarding escolher qual vincular. O filtro de nome é opcional; sem
     * ele, devolve todos ordenados por nome.
     */
    @Transactional(readOnly = true)
    public List<ClienteResumoDTO> listar(String nome)
    {
        String nomeFiltro = StringUtils.hasText(nome) ? nome.trim() : null;

        return clienteRepository.buscarPorNome(nomeFiltro).stream()
                .map(ClienteResumoDTO::de)
                .toList();
    }

}
