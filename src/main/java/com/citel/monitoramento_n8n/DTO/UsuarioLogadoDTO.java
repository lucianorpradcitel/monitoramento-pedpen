package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Quem é o usuário interno dono do token, para o n8n decidir quem pode exportar workflows.
 *
 * Usuario implementa UserDetails e carrega o googleId e as datas de acesso — por isso o endpoint
 * nunca devolve a entidade, só este recorte.
 */
public record UsuarioLogadoDTO(
        String email,
        String nome,
        @Schema(description = "ADMIN ou USUARIO, lido do CADUSR (USR_PERFIL) no momento da chamada.",
                allowableValues = {Usuario.PERFIL_ADMIN, Usuario.PERFIL_USUARIO})
        String perfil
) {

    /** O perfil sai normalizado: um valor estranho no banco aparece como USUARIO, nunca como lixo. */
    public static UsuarioLogadoDTO de(Usuario usuario) {
        return new UsuarioLogadoDTO(
                usuario.getEmail(),
                usuario.getNome(),
                usuario.isAdmin() ? Usuario.PERFIL_ADMIN : Usuario.PERFIL_USUARIO);
    }
}
