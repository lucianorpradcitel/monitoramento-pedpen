package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Plataforma;

/** Item do GET /plataformas. Também é a resposta do POST — não há segredo a esconder aqui. */
public record PlataformaResumoDTO(
        String descricao,
        String sistemaExterno
) {

    public static PlataformaResumoDTO de(Plataforma plataforma) {
        return new PlataformaResumoDTO(plataforma.getDescricao(), plataforma.getSistemaExterno());
    }
}
