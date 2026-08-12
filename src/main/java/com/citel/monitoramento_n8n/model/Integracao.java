package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

/**
 * Registry de tenants da integração multi-tenant (CADINT).
 * A fonte da verdade da configuração de um lojista: o n8n lê daqui pela API, não mais por banco.
 *
 * Não guarda configuração de negócio (CFG_*) — isso vem do /V2/config do webservice do cliente.
 */
@Entity
@Table(name = "CADINT")
@IdClass(IntegracaoId.class)
@Getter
@Setter
public class Integracao {

    /**
     * Código de autorização da integração, vem do ERP. Vai no claim codigoSistema do JWT RS256
     * assinado para o webservice.
     *
     * Pertence à integração, não ao lojista: um lojista com Tray e Mercos tem dois códigos. E como
     * o código pode se repetir entre lojistas diferentes, sozinho ele não identifica a linha.
     */
    @Id
    @Column(name = "INT_CODAUT", length = 7)
    private String codigoIntegracao;

    /** Segunda metade da PK e FK para CADCLI.CLI_CODCLI — o lojista dono desta integração. */
    @Id
    @Column(name = "INT_CODCLI")
    private Long codigoCliente;

    /**
     * Mapeamento de leitura da mesma coluna, só para o nomeCliente das respostas — quem escreve
     * INT_CODCLI é o campo acima, que faz parte da chave.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INT_CODCLI", insertable = false, updatable = false)
    private Cliente cliente;

    @Column(name = "INT_NOMPLA", length = 20, nullable = false)
    private String plataforma;

    @Column(name = "INT_SLUG__", length = 40, nullable = false)
    private String slug;

    /** Token do webhook, gerado no servidor. É o que o n8n manda no header x-ct-token. */
    @Column(name = "INT_WBHTOK", length = 80, nullable = false)
    private String webhookToken;

    /** char(1) 'S'/'N' — exposto como boolean nos DTOs, convertido na borda. */
    @Column(name = "INT_ATIVO_", length = 1, nullable = false)
    private String ativo;

    @Column(name = "INT_URLAPI")
    private String urlApi;

    /** Preenchido pelo OAuth (PATCH /tokens), nunca pelo cadastro. */
    @Column(name = "INT_APITOK")
    private String apiToken;

    /** Idem apiToken. */
    @Column(name = "INT_REFTOK")
    private String refreshToken;

    // INT_CONKEY e INT_CONSEC existem na CADINT mas não são mapeadas de propósito: a API não as
    // expõe, e sem mapeamento o ORM nunca escreve por cima do que já estiver gravado nelas.

    /** Webservice do ERP. */
    @Column(name = "INT_URLWBS", nullable = false)
    private String urlWebservice;

    /** Chave RSA em PEM. Gravada exatamente como recebida — reformatar quebra a assinatura RS256. */
    @Column(name = "INT_PRVKEY", columnDefinition = "text", nullable = false)
    private String chavePrivada;

    /**
     * Preenchida no service. O DEFAULT CURRENT_TIMESTAMP da coluna fica como rede de segurança —
     * se fosse insertable=false, o 201 devolveria null porque o valor só existiria após um refresh.
     */
    @Column(name = "INT_DHUINC")
    private LocalDateTime dataInclusao;

    /**
     * Quem preenche é o ON UPDATE CURRENT_TIMESTAMP do MySQL. O @Generated faz o Hibernate
     * reler a coluna depois do UPDATE — sem ele o PATCH /tokens devolveria a data anterior.
     */
    @Generated(event = EventType.UPDATE)
    @Column(name = "INT_DHUALT")
    private LocalDateTime dataAlteracao;

    public boolean isAtivo() {
        return "S".equalsIgnoreCase(this.ativo);
    }
}
