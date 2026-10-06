-- LOGINT: histórico das edições de integração (PATCH /integracoes/{codigoIntegracao}/{codigoCliente}).
-- Uma linha por campo alterado em cada request. O Hibernate roda com ddl-auto=none, então a tabela
-- precisa existir antes de subir a versão da API que grava nela.

CREATE TABLE IF NOT EXISTS LOGINT (
    AUTOINCREM  BIGINT AUTO_INCREMENT PRIMARY KEY,
    LOG_DHUALT  DATETIME DEFAULT CURRENT_TIMESTAMP,  -- data/hora da alteração, preenchida pelo banco
    LOG_NOMPLA  VARCHAR(255),                        -- plataforma da integração alterada
    LOG_NOMCLI  VARCHAR(255),                        -- nome do lojista
    LOG_CODAUT  VARCHAR(7),                          -- código da integração (ex.: 0000004)
    LOG_CMPALT  VARCHAR(255),                        -- coluna do CADINT alterada
    LOG_VALANT  TEXT,                                -- valor anterior
    LOG_VALATU  TEXT,                                -- valor atual
    LOG_NOMUSU  VARCHAR(255)                         -- nome do usuário (CADUSR) que alterou
);

-- Conferir as últimas alterações:
-- SELECT * FROM LOGINT ORDER BY AUTOINCREM DESC LIMIT 50;

-- Reverter (apaga o histórico):
-- DROP TABLE LOGINT;
