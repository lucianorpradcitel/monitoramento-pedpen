-- CADMSG passa a se chamar CADERR (decisão de 25/11/2026): é a tabela dos tipos de erro de integração.
--
-- Ordem: o RENAME e o deploy da API que aponta para CADERR têm de acontecer juntos.
--   * RENAME antes do deploy: a API antiga perde o /tipos-erro.
--   * Deploy antes do RENAME: a API nova perde o /tipos-erro, o /produtos e o /pedidos (o campo "responsabilidade"
--     lê a tabela).
-- Rode um comando por vez e confira o banco com: SELECT DATABASE(), @@hostname;

RENAME TABLE CADMSG TO CADERR;
