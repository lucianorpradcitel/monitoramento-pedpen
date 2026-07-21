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
    private int sequencialProcessamento;

    public static Pedido converterDTO(PedidoLoteDTO dto, Pedido pd) {
        pd.setCliente(dto.getCliente());
        pd.setCodigoPedido(dto.getCodigoPedido());
        pd.setPlataforma(dto.getPlataforma());
        pd.setDataPedido(dto.getDataPedido());
        pd.setUltimaAlteracao(LocalDateTime.now());
        pd.setSequencialProcessamento(pd.getSequencialProcessamento() + 1);

        return pd;
    }
}
