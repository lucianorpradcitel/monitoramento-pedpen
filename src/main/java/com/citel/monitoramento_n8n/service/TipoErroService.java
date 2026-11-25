package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.DadosCriacaoTipoErro;
import com.citel.monitoramento_n8n.DTO.TipoErroDTO;
import com.citel.monitoramento_n8n.DTO.ValidacaoTipoErroDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.TipoErro;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.PlataformaRepository;
import com.citel.monitoramento_n8n.repository.StatusErroRepository;
import com.citel.monitoramento_n8n.repository.TipoErroRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Tipos de erro (CADMSG). Quem grava tipo novo é o n8n (SUB_Error_Status), depois que a IA decide;
 * quem valida e reclassifica é o time, pelo portal.
 */
@Slf4j
@Service
public class TipoErroService {

    /** MSG_USUVAL é varchar(30). */
    private static final int TAMANHO_USUARIO = 30;
    private static final int TAMANHO_TEXTO = 255;

    private final TipoErroRepository tipoErroRepository;
    private final PlataformaRepository plataformaRepository;
    private final StatusErroRepository statusErroRepository;

    public TipoErroService(TipoErroRepository tipoErroRepository,
                           PlataformaRepository plataformaRepository,
                           StatusErroRepository statusErroRepository) {
        this.tipoErroRepository = tipoErroRepository;
        this.plataformaRepository = plataformaRepository;
        this.statusErroRepository = statusErroRepository;
    }

    /** Resultado do POST: o tipo e se ele foi criado agora (201) ou já existia (200). */
    public record Registro(TipoErroDTO tipo, boolean criado) {
    }

    @Transactional(readOnly = true)
    public List<TipoErroDTO> listar(String plataforma, String validado, String codigoErro) {
        String filtroValidado = vazioViraNulo(validado);
        if (filtroValidado != null) {
            filtroValidado = filtroValidado.toUpperCase(Locale.ROOT);
            if (!TipoErro.VALIDADO.equals(filtroValidado) && !TipoErro.NAO_VALIDADO.equals(filtroValidado)) {
                throw new BusinessException("validado deve ser S ou N");
            }
        }

        return tipoErroRepository.buscar(vazioViraNulo(plataforma), filtroValidado, vazioViraNulo(codigoErro))
                .stream()
                .map(TipoErroDTO::de)
                .toList();
    }

    /**
     * Grava o tipo novo ou devolve o que já existe para a mesma plataforma + código, sem erro.
     * Dois erros iguais podem chegar juntos: o segundo bate na chave única e recebe o tipo do primeiro.
     *
     * Sem @Transactional de propósito: se o save estourar a chave única, a transação dele é desfeita
     * e a releitura abaixo roda numa transação nova.
     *
     * @param usuario usuário interno (admin pelo portal) ou nulo (token de lojista, o n8n)
     */
    public Registro registrar(DadosCriacaoTipoErro dados, Usuario usuario) {
        String plataforma = dados.plataforma().trim();
        String codigoErro = dados.codigoErro().trim();

        var existente = tipoErroRepository.findByPlataformaAndCodigoErro(plataforma, codigoErro);
        if (existente.isPresent()) {
            return new Registro(TipoErroDTO.de(existente.get()), false);
        }

        if (!plataformaRepository.existsById(plataforma)) {
            throw new BusinessException("Plataforma não cadastrada na CADPLA: " + plataforma);
        }
        exigirStatusValido(dados.status());

        TipoErro novo = new TipoErro();
        novo.setPlataforma(plataforma);
        novo.setCodigoErro(codigoErro);
        novo.setStatus(dados.status());
        novo.setExemplo(cortar(vazioViraNulo(dados.exemplo()), TAMANHO_TEXTO));
        novo.setIdExemplo(cortar(vazioViraNulo(dados.idExemplo()), TAMANHO_TEXTO));
        novo.setDataCadastro(LocalDateTime.now());

        if (usuario != null) {
            // Cadastro manual pelo portal: quem cadastra já está validando.
            novo.setOrigem(TipoErro.ORIGEM_USUARIO);
            marcarValidado(novo, usuario);
        } else {
            novo.setOrigem(TipoErro.ORIGEM_IA);
            novo.setValidado(TipoErro.NAO_VALIDADO);
        }

        try {
            TipoErro salvo = tipoErroRepository.saveAndFlush(novo);
            log.info("Tipo de erro criado - código: {}, plataforma: {}, erro: {}, status: {}, origem: {}",
                    salvo.getCodigo(), plataforma, codigoErro, salvo.getStatus(), salvo.getOrigem());
            return new Registro(TipoErroDTO.de(salvo), true);
        } catch (DataIntegrityViolationException e) {
            log.info("Tipo de erro gravado ao mesmo tempo por outra requisição - plataforma: {}, erro: {}",
                    plataforma, codigoErro);
            return tipoErroRepository.findByPlataformaAndCodigoErro(plataforma, codigoErro)
                    .map(t -> new Registro(TipoErroDTO.de(t), false))
                    .orElseThrow(() -> e);
        }
    }

    /**
     * Valida o tipo (o time confirmou) e, se vier status, reclassifica. Todos os erros ligados
     * a ele pelo CODMSG passam a mostrar o status novo.
     */
    @Transactional
    public TipoErroDTO validar(Integer codigo, ValidacaoTipoErroDTO dados, Usuario usuario) {
        TipoErro tipo = tipoErroRepository.findById(codigo)
                .orElseThrow(() -> new NotFoundException("Tipo de erro não encontrado: " + codigo));

        Integer statusAnterior = tipo.getStatus();
        if (dados != null && dados.status() != null) {
            exigirStatusValido(dados.status());
            tipo.setStatus(dados.status());
        }
        marcarValidado(tipo, usuario);

        // O 3 diz ao cliente "a culpa é sua": quem validou e o que mudou ficam no log, além da tabela.
        log.info("Tipo de erro validado - código: {}, plataforma: {}, erro: {}, status: {} -> {}, por: {}",
                codigo, tipo.getPlataforma(), tipo.getCodigoErro(), statusAnterior, tipo.getStatus(),
                tipo.getUsuarioValidacao());

        return TipoErroDTO.de(tipoErroRepository.save(tipo));
    }

    private void exigirStatusValido(Integer status) {
        if (status == null || !statusErroRepository.existsById(status)) {
            throw new BusinessException("status inválido: " + status + " (precisa existir na STAERR)");
        }
    }

    private static void marcarValidado(TipoErro tipo, Usuario usuario) {
        tipo.setValidado(TipoErro.VALIDADO);
        tipo.setUsuarioValidacao(identificar(usuario));
        tipo.setDataValidacao(LocalDateTime.now());
    }

    /**
     * MSG_USUVAL tem 30 caracteres e o e-mail completo não cabe. O domínio é sempre o da Citel
     * (login pelo Google Workspace), então guarda só a parte antes do @.
     */
    static String identificar(Usuario usuario) {
        if (usuario == null || usuario.getEmail() == null) {
            return null;
        }
        String email = usuario.getEmail();
        int arroba = email.indexOf('@');
        return cortar(arroba > 0 ? email.substring(0, arroba) : email, TAMANHO_USUARIO);
    }

    private static String vazioViraNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String cortar(String valor, int tamanho) {
        return valor == null || valor.length() <= tamanho ? valor : valor.substring(0, tamanho);
    }
}
