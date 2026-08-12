package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Cliente;

/**
 * Resposta do POST /cadastro.
 *
 * Existe para a entidade Cliente não ser serializada direto: ela implementa UserDetails, então o
 * corpo levava o hash bcrypt de CLI_SENCLI (em "senha" e em "password") até o navegador.
 */
public record ClienteCriadoDTO(
        Long codigoCliente,
        String nome,
        String userName
) {

    public static ClienteCriadoDTO de(Cliente cliente) {
        return new ClienteCriadoDTO(cliente.getId(), cliente.getNome(), cliente.getUsername());
    }
}
