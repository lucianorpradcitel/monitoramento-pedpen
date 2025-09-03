package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.DadosCriacaoCliente;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Cliente criarCliente(DadosCriacaoCliente dados)
    {
        if (clienteRepository.findByUserName(dados.userName()) != null)
        {
            throw new RuntimeException("Empresa já existe");
        }

        String senhaCriptografada = passwordEncoder.encode(dados.senha());

        Cliente novoCliente = new Cliente(dados.id(), dados.nome(), dados.userName(),senhaCriptografada );
        return clienteRepository.save(novoCliente);
    }

}
