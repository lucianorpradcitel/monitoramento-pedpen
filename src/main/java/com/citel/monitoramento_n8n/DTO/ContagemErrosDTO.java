package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/** Quantos erros há, por responsabilidade, em produtos e em pedidos. A chave é a responsabilidade (0 a 4). */
public record ContagemErrosDTO(
        @Schema(description = "Produtos com erro por responsabilidade. Só as responsabilidades que o perfil pode ver; as sem erro vêm com 0",
                example = "{\"0\": 1231, \"1\": 9, \"2\": 2, \"3\": 0, \"4\": 0}")
        Map<Integer, Long> produtos,
        @Schema(description = "Pedidos com erro por responsabilidade, no mesmo formato",
                example = "{\"0\": 155, \"1\": 0, \"2\": 0, \"3\": 0, \"4\": 0}")
        Map<Integer, Long> pedidos
) {
}
