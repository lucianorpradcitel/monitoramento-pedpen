-- Perfil de acesso dos usuários internos (CADUSR).
--
-- O hibernate.ddl-auto é "none": a API NÃO cria esta coluna. Rode este script no banco ANTES de
-- publicar a versão da API que lê USR_PERFIL; sem ele o login quebra para todos.
-- A API antiga continua funcionando com a coluna a mais (o DEFAULT cobre os inserts dela).
-- Rode um comando por vez e confira o banco com: SELECT DATABASE(), @@hostname;

-- 1) A coluna. Quem já existe vira 'USUARIO'; ninguém nasce admin.
ALTER TABLE CADUSR
  ADD COLUMN USR_PERFIL VARCHAR(10) NOT NULL DEFAULT 'USUARIO';

-- 2) Promover alguém a admin. Confira antes com o SELECT, que deve achar uma linha por e-mail.
--    O usuário só existe no CADUSR depois do primeiro login com o Google.
-- SELECT USR_CODUSR, USR_EMAIL_, USR_PERFIL FROM CADUSR
--  WHERE USR_EMAIL_ IN ('pessoa@citelsoftware.com.br');
--
-- UPDATE CADUSR SET USR_PERFIL = 'ADMIN'
--  WHERE USR_EMAIL_ IN ('pessoa@citelsoftware.com.br');

-- 3) Rebaixar volta para USUARIO. Vale na próxima requisição; o token já emitido só reflete na tela
--    no próximo login, mas a API passa a barrar na hora.
-- UPDATE CADUSR SET USR_PERFIL = 'USUARIO' WHERE USR_EMAIL_ = 'pessoa@citelsoftware.com.br';

-- 4) Conferir.
-- SELECT USR_EMAIL_, USR_PERFIL FROM CADUSR ORDER BY USR_PERFIL, USR_EMAIL_;

-- Desfazer tudo (só se a API nova ainda não estiver no ar):
-- ALTER TABLE CADUSR DROP COLUMN USR_PERFIL;
