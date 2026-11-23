-- STAERR e CADMSG: responsabilidade dos erros de integração (2 = culpa nossa, 3 = não é culpa nossa),
-- usadas pelos endpoints /tipos-erro. PRO_CODMSG e PEN_CODMSG ligam cada erro ao tipo dele.
--
-- JÁ RODADO no banco da Monint em 19/11/2026 (registro para quem montar outro ambiente).
-- O Hibernate roda com ddl-auto=none: as tabelas precisam existir antes de subir a API que usa /tipos-erro.
-- MySQL 5.7: os ALTER são feitos online (ALGORITHM=INPLACE, LOCK=NONE dá erro em vez de travar a tabela).
-- Rode um comando por vez e confira o banco com: SELECT DATABASE(), @@hostname;

-- 1) Status
CREATE TABLE STAERR (
  STA_CODIGO int(11)     NOT NULL,
  STA_DESCRI varchar(60) NOT NULL,
  PRIMARY KEY (STA_CODIGO)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO STAERR (STA_CODIGO, STA_DESCRI) VALUES
  (2, 'Erro da integracao (culpa nossa)'),
  (3, 'Nao e culpa nossa (cliente/plataforma)');

-- 2) Tipos de erro. MSG_SISEXT é utf8mb4 porque a chave estrangeira exige o mesmo charset do CADPLA.PLA_SISEXT.
CREATE TABLE CADMSG (
  MSG_CODIGO int(11)      NOT NULL AUTO_INCREMENT,
  MSG_SISEXT varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  MSG_CODERR varchar(60)  NOT NULL,
  MSG_CODSTA int(11)      NOT NULL,
  MSG_VALIDA char(1)      NOT NULL DEFAULT 'N',
  MSG_ORIGEM char(1)      NOT NULL,              -- C carga inicial · I IA · U usuário
  MSG_EXEMPL varchar(255) DEFAULT NULL,
  MSG_IDEXEM varchar(255) DEFAULT NULL,          -- PRO_IDERRO ou PEN_IDPED_ de exemplo
  MSG_DTACAD datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MSG_USUVAL varchar(30)  DEFAULT NULL,
  MSG_DTAVAL datetime     DEFAULT NULL,
  PRIMARY KEY (MSG_CODIGO),
  UNIQUE KEY UK_CADMSG_01 (MSG_SISEXT, MSG_CODERR),
  KEY IDX_CADMSG_01 (MSG_CODSTA),
  KEY IDX_CADMSG_02 (MSG_VALIDA),
  CONSTRAINT FK_CADMSG_CADPLA FOREIGN KEY (MSG_SISEXT) REFERENCES CADPLA (PLA_SISEXT),
  CONSTRAINT FK_CADMSG_STAERR FOREIGN KEY (MSG_CODSTA) REFERENCES STAERR (STA_CODIGO)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 3) Vínculo de cada erro com o tipo (sem chave estrangeira: no 5.7 ela copiaria a tabela). Fora do pico.
ALTER TABLE PROERR ADD COLUMN PRO_CODMSG int(11) DEFAULT NULL, ADD KEY IDX_PROERR_03 (PRO_CODMSG), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE PEDPEN ADD COLUMN PEN_CODMSG int(11) DEFAULT NULL, ADD KEY IDX_PEDPEN_03 (PEN_CODMSG), ALGORITHM=INPLACE, LOCK=NONE;

-- Conferir:
-- SELECT * FROM STAERR;
-- SELECT * FROM CADMSG ORDER BY MSG_CODIGO DESC LIMIT 50;

-- Desfazer (na ordem inversa):
-- ALTER TABLE PEDPEN DROP KEY IDX_PEDPEN_03, DROP COLUMN PEN_CODMSG;
-- ALTER TABLE PROERR DROP KEY IDX_PROERR_03, DROP COLUMN PRO_CODMSG;
-- DROP TABLE CADMSG;
-- DROP TABLE STAERR;
