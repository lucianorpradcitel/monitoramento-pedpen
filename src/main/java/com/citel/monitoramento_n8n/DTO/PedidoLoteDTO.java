package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Pedido;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PedidoLoteDTO {

    private String codigoPedido;
    private String cliente;
    private String plataforma;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataPedido;
    @Schema(hidden = true)
    private LocalDateTime ultimaAlteracao;
    // Ignorado: o lote não mexe no sequencial (só o POST /pendentes incrementa).
    @Schema(hidden = true)
    private int sequencialProcessamento;

    /**
     * INT_CODAUT da integração que está reportando. Opcional: omitido, PEN_ID_INT fica nulo.
     * Gravado como veio, sem conferir na CADINT: o n8n autentica a captura com o login de
     * serviço, que não é o dono das integrações.
     */
    @Schema(description = "Código da integração (CADINT.INT_CODAUT). Opcional.")
    private String idIntegracao;

    // idIntegracao não entra aqui: o service grava depois, já normalizado (vazio vira nulo).
    public static Pedido converterDTO(PedidoLoteDTO dto, Pedido pd) {
        pd.setCliente(dto.getCliente());
        pd.setCodigoPedido(dto.getCodigoPedido());
        pd.setPlataforma(dto.getPlataforma());
        pd.setDataPedido(dto.getDataPedido());
        pd.setUltimaAlteracao(LocalDateTime.now());

        return pd;
    }
}
