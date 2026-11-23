package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.TipoErro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TipoErroRepository extends JpaRepository<TipoErro, Integer> {

    /** A colação latin1 padrão já ignora maiúsculas e minúsculas na comparação. */
    Optional<TipoErro> findByPlataformaAndCodigoErro(String plataforma, String codigoErro);

    /** Filtros opcionais, combinados com E. Parâmetro nulo não filtra. */
    @Query("""
            select t from TipoErro t
            where (:plataforma is null or t.plataforma = :plataforma)
              and (:validado is null or t.validado = :validado)
              and (:codigoErro is null or t.codigoErro = :codigoErro)
            order by t.plataforma, t.codigoErro""")
    List<TipoErro> buscar(@Param("plataforma") String plataforma,
                          @Param("validado") String validado,
                          @Param("codigoErro") String codigoErro);
}
