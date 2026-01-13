package com.citel.monitoramento_n8n.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="PONPEN")
public class PontuacaoPendente {

    @Id
    @Column(name="PON_NUMDOC")
    private String numeroDocumento;
    @Column(name="PON_NOMCLI")
    private String nomeCliente;
    @Column(name="PON_PAYLOD")
    private String jsonEnviado;
    @Column(name="PON_LOGERR")
    private String msgErr;
    @Column(name="PON_STATUS")
    private int status;
}
