-- CADMSG sem MSG_ORIGEM: o tipo sempre nasce da IA (o usuário só valida ou reclassifica), então a coluna
-- ficava sempre 'I' e não separava nada (decisão de 25/11/2026).
--
-- Ordem: primeiro suba a API sem o campo origem; só depois rode o ALTER. Com a API antiga no ar,
-- o Hibernate ainda lê e grava MSG_ORIGEM e o /tipos-erro daria erro.
-- Rode um comando por vez e confira o banco com: SELECT DATABASE(), @@hostname;

ALTER TABLE CADMSG DROP COLUMN MSG_ORIGEM, ALGORITHM=INPLACE, LOCK=NONE;
