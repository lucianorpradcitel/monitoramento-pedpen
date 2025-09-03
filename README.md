# 📡 API de Monitoramento PEDPEN

API desenvolvida para monitorar os pedidos pendentes de integração, basicamente ela substitui a tabela PEDPEN no novo modelo de integração.
Fornece endpoints para consulta de status, registro de pedidos e consulta.

## 🛠️ Tecnologias 

- Java com Spring
- Banco de dados: MySQL
- Autenticação: JWT
- Documentação: Swagger

- ## Fazer o Deploy utilizando Docker e definir as variáveis de ambiente abaixo:

- ${DATABASE_URL}
- ${DATABASE_USER}
- ${DATABASE_PASSWORD}

## Endpoints:

- GET     /pendentes -> Retorna os pedidos pendentes
- POST    /pendentes -> Adiciona um pedido que ainda não foi integrado
- PATCH   /pendentes -> Passando o NUMPOK + Nome do cliente, atualiza para integrado.

