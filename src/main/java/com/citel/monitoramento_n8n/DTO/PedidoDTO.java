package com.citel.monitoramento_n8n.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class PedidoDTO {

    @Setter
    private String codigoPedido;
    @Setter
    private String cliente;
    @Setter
    private String erro;
    @Setter
    private String plataforma;
    // Sem setter, mas o JSON preenche mesmo assim: como há getter, o Jackson usa o campo privado
    // para gravar. O service copia esse valor para o pedido, então o status enviado é o que vale.
    private int status;
    @Setter
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataPedido;
    @Setter
    @Schema(hidden = true)
    private LocalDateTime ultimaAlteracao;
    // Ignorado: o service calcula o sequencial a partir do que está gravado.
    @Setter
    @Schema(hidden = true)
    private int sequencialProcessamento;
    @Setter
    private String rotina;

    /**
     * INT_CODAUT da integração que está reportando. Opcional: omitido, PEN_ID_INT fica nulo.
     * O service confere que o código pertence ao lojista autenticado antes de gravar.
     */
    @Setter
    @Schema(description = "Código da integração (CADINT.INT_CODAUT). Opcional.")
    private String idIntegracao;
}
