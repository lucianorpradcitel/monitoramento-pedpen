package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "CADPLA")
@Getter
public class Plataforma {

    /**
     * Código da plataforma no sistema externo, não gerado pelo banco. Sem @GeneratedValue: quem
     * atribui é o request de cadastro, e o service precisa checar existsById antes de save() —
     * com PK client-assigned o Spring Data faz merge (UPDATE) em vez de estourar chave duplicada.
     */

    @Column(name = "PLA_DESCRI")
    private String descricao;

    @Id
    @Column(name = "PLA_SISEXT")
    private String sistemaExterno;

    public Plataforma(String descricao, String sistemaExterno) {
        this.descricao = descricao;
        this.sistemaExterno = sistemaExterno;
    }

    public Plataforma() {}
}
