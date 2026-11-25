package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tipo de erro de integração (CADMSG): plataforma + código estável + responsabilidade (2 ou 3).
 *
 * A responsabilidade fica no tipo, não em cada erro: PROERR.PRO_CODMSG e PEDPEN.PEN_CODMSG apontam para
 * MSG_CODIGO, então reclassificar o tipo muda todos os erros ligados a ele de uma vez.
 *
 * Chave única (MSG_SISEXT, MSG_CODERR). A tabela é latin1 com colação padrão: maiúsculas e minúsculas
 * contam como iguais na busca e na chave.
 */
@Entity
@Table(name = "CADMSG")
@Getter
@Setter
public class TipoErro {

    /** 'S'/'N' em MSG_VALIDA. */
    public static final String VALIDADO = "S";
    public static final String NAO_VALIDADO = "N";

    /** MSG_ORIGEM: quem cadastrou. */
    public static final String ORIGEM_CARGA = "C";
    public static final String ORIGEM_IA = "I";
    public static final String ORIGEM_USUARIO = "U";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MSG_CODIGO")
    private Integer codigo;

    /** PLA_SISEXT da CADPLA (texto, ex.: '1005'). */
    @Column(name = "MSG_SISEXT", nullable = false)
    private String plataforma;

    /** Código estável do erro (ex.: categoria_ausente). Nunca o texto da mensagem, que muda a cada ocorrência. */
    @Column(name = "MSG_CODERR", nullable = false, length = 60)
    private String codigoErro;

    /** STA_CODIGO da STAERR: 0 não informado, 1 nossa, 2 cliente, 3 instabilidade, 4 plataforma externa. */
    @Column(name = "MSG_CODSTA", nullable = false)
    private Integer status;

    @Column(name = "MSG_VALIDA", nullable = false, length = 1)
    private String validado = NAO_VALIDADO;

    @Column(name = "MSG_ORIGEM", nullable = false, length = 1)
    private String origem;

    /** Uma mensagem real de exemplo, para quem for validar. */
    @Column(name = "MSG_EXEMPL")
    private String exemplo;

    /** ID da linha que gerou o tipo (PRO_IDERRO ou PEN_IDPED_). É um exemplo, não é único. */
    @Column(name = "MSG_IDEXEM")
    private String idExemplo;

    @Column(name = "MSG_DTACAD", nullable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "MSG_USUVAL", length = 30)
    private String usuarioValidacao;

    @Column(name = "MSG_DTAVAL")
    private LocalDateTime dataValidacao;
}
