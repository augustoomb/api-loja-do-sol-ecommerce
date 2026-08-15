# <img width="15" height="15" alt="515258110_17846218257516446_532124129804207679_n" src="https://github.com/user-attachments/assets/2e780b3d-ce18-492d-88fe-e5dda992e272" /> Loja do Sol — API de E-commerce

API RESTful do e-commerce **Loja do Sol**: catálogo de produtos com cache em Redis, carrinho de compras server-side, checkout integrado ao Stripe e controle de estoque com auditoria.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF?style=for-the-badge&logo=stripe&logoColor=white)

---

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Como executar](#como-executar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Documentação da API](#documentação-da-api)
- [Referência de endpoints](#referência-de-endpoints)
- [Cache Redis](#cache-redis)
- [Pagamentos com Stripe](#pagamentos-com-stripe)
- [Testes](#testes)
- [Observabilidade](#observabilidade)
- [Licença](#licença)

---

## Sobre o projeto

A API concentra todo o fluxo comercial da loja, do cadastro do cliente até a entrega do pedido:

1. **Cliente** se cadastra, faz login (JWT) e cadastra endereços e telefones.
2. **Catálogo** com produtos, categorias e busca — leituras servidas com cache em Redis.
3. **Carrinho** server-side, com quantidades validadas contra o estoque.
4. **Checkout** cria o pedido e a sessão de pagamento no Stripe (PIX, cartão ou boleto).
5. **Webhook** do Stripe confirma o pagamento, baixa o estoque na mesma transação e, se algo falhar, cancela o pedido e reembolsa o cliente.
6. **Administrador** acompanha os pedidos, marca envios e cancela pedidos (com reembolso e devolução de estoque).

## Funcionalidades

- **Autenticação e autorização** — registro e login com JWT stateless e perfis `ROLE_USER` e `ROLE_ADMIN`.
- **Catálogo** — CRUD de produtos e categorias, busca por nome, filtro por status e listagem por categoria.
- **Estoque** — livro de movimentações auditado (entradas, saídas e ajustes), estoque mínimo por produto e proteção contra saldo negativo.
- **Carrinho de compras** — carrinho por usuário, com total sempre recalculado no servidor.
- **Checkout e pedidos** — integração com Stripe e ciclo de vida do pedido: `PENDENTE`, `PAGO`, `ENVIADO` e `CANCELADO`.
- **Cadastro do cliente** — endereços e telefones vinculados ao usuário autenticado.

## Tecnologias

### Backend

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem |
| Spring Boot | 4.1.0 | Framework |
| Spring MVC | — | Camada REST |
| Spring Data JPA / Hibernate | — | Persistência e mapeamento objeto-relacional |
| Spring Security + JJWT | 0.12.6 | Autenticação e autorização com JWT |
| Spring Cache + Spring Data Redis | — | Cache de leitura do catálogo |
| Spring Actuator | — | Health check, métricas e logs |
| PostgreSQL | 16 | Banco de dados relacional |
| Redis | 7 | Cache em memória |
| Stripe (stripe-java) | 33.2.0 | Pagamentos e webhooks |
| springdoc-openapi | 2.8.6 | Documentação OpenAPI 3 / Swagger UI |
| Docker / Docker Compose | — | Ambiente local conteinerizado |
| Prometheus | 3.13 | Coleta e armazenamento de métricas (via Micrometer) |
| Grafana | 13.1 | Dashboards de monitoramento (JVM / Spring Boot) |
| Maven (wrapper) | — | Build e gerenciamento de dependências |

### Testes

- **JUnit 5** e **Mockito** para testes unitários das camadas de serviço.
- **Testes de integração** com `@SpringBootTest` contra o PostgreSQL e o Redis locais (providos pelo Docker Compose).

## Arquitetura

O projeto segue o padrão de **arquitetura em camadas**, com regras de negócio isoladas na camada de serviço e a camada de controller dedicada aos contratos REST:

```text
api-loja-do-sol-ecommerce/
├── docker-compose.yml          # PostgreSQL + Redis + aplicação + Prometheus + Grafana
├── prometheus/prometheus.yml   # Configuração de coleta de métricas
├── grafana/                    # Provisionamento automático (datasource + dashboard JVM)
├── pom.xml
└── src/
    ├── main/java/com/augustoomb/api_loja_do_sol_ecommerce/
    │   ├── config/             # Segurança, CORS, cache Redis, OpenAPI e bootstrap do admin
    │   ├── controller/         # Endpoints REST
    │   ├── dto/                # Objetos de transferência de dados
    │   ├── exception/          # Exceções de negócio e tratamento global de erros
    │   ├── model/              # Entidades JPA
    │   ├── repository/         # Repositórios Spring Data JPA
    │   ├── security/           # Filtro JWT e autenticação
    │   ├── service/            # Regras de negócio e orquestração
    │   └── web/                # Logging de requisições e request ID
    └── test/java/...           # Testes unitários e de integração
```

Principais decisões:

- **Transações** — operações de venda e baixa de estoque usam `@Transactional` para garantir consistência.
- **Consistência do estoque** — a baixa usa condição `WHERE stock >= quantidade` com lock otimista (`@Version`), impedindo venda sem saldo mesmo sob concorrência.
- **Cache de DTOs** — o Redis armazena apenas `ProductResponseDTO`/`CategoryResponseDTO` em JSON, nunca entidades JPA.
- **Degradação graciosa** — se o Redis estiver fora do ar, a aplicação continua respondendo consultando o PostgreSQL diretamente.

## Como executar

### Pré-requisitos

- [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/).
- Java 21 e Maven apenas para desenvolvimento local fora do Docker.

### Passo a passo

```bash
# 1. Clone o repositório e acesse o diretório
git clone https://github.com/seu-usuario/api-loja-do-sol-ecommerce.git
cd api-loja-do-sol-ecommerce

# 2. Crie o arquivo de ambiente a partir do exemplo
cp .env.example .env

# 3. Edite o .env com as credenciais do banco e o JWT_SECRET
#    (gere o segredo JWT com: openssl rand -hex 64)

# 4. Suba a stack completa (PostgreSQL + Redis + API + Prometheus + Grafana)
docker compose up -d
```

No primeiro boot, um usuário administrador é criado automaticamente com as credenciais definidas em `ADMIN_EMAIL` e `ADMIN_PASSWORD` no `.env`.

### Desenvolvimento local

```bash
# Com o PostgreSQL e o Redis rodando (docker compose up -d db redis)
./mvnw spring-boot:run
```

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | sim | — | Credenciais do container PostgreSQL |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | sim | — | Conexão JDBC da aplicação |
| `JWT_SECRET` | sim | — | Segredo para assinatura dos tokens JWT |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | sim | — | Credenciais do administrador criado no primeiro boot |
| `CORS_ALLOWED_ORIGINS` | não | `http://localhost:5173` | Origens permitidas no CORS (separadas por vírgula) |
| `REDIS_HOST` / `REDIS_PORT` | não | `localhost` / `6379` | Endereço do Redis (fora do Docker) |
| `STRIPE_SECRET_KEY` | não | *(vazia)* | Chave secreta do Stripe (`sk_test_...`) |
| `STRIPE_WEBHOOK_SECRET` | não | *(vazia)* | Secret do webhook (`whsec_...`) |
| `STRIPE_SIMULATE` | não | `true` | `true` = modo simulado (sem chamadas reais ao Stripe) |
| `FRONTEND_URL` | não | `http://localhost:5173` | URLs de sucesso/cancelamento do checkout |
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | não | `admin` / `admin` | Credenciais de acesso ao Grafana |

## Documentação da API

Com a aplicação no ar, a documentação interativa está disponível em:

```text
http://localhost:8080/swagger-ui.html
```

O contrato OpenAPI também pode ser consultado em `http://localhost:8080/v3/api-docs`.

## Referência de endpoints

### Autenticação — `/api/auth`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/register` | público | Cadastro de cliente |
| `POST` | `/api/auth/login` | público | Login e emissão do token JWT |

### Catálogo — `/api/products` e `/api/categories`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/products` | autenticado | Lista produtos (filtro opcional `?enabled=true`) |
| `GET` | `/api/products/{id}` | autenticado | Detalhe do produto |
| `GET` | `/api/products/category/{categoryId}` | autenticado | Produtos por categoria |
| `GET` | `/api/products/search?name=` | autenticado | Busca de produtos por nome |
| `POST` | `/api/products` | ADMIN | Criar produto |
| `PUT` | `/api/products/{id}` | ADMIN | Atualizar produto |
| `DELETE` | `/api/products/{id}` | ADMIN | Excluir produto |
| `GET` | `/api/categories` | autenticado | Lista categorias |
| `GET` | `/api/categories/{id}` | autenticado | Detalhe da categoria |
| `POST` | `/api/categories` | ADMIN | Criar categoria |
| `PUT` | `/api/categories/{id}` | ADMIN | Atualizar categoria |
| `DELETE` | `/api/categories/{id}` | ADMIN | Excluir categoria |

### Estoque — `/api/products/{id}/stock` e `/api/stock`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/products/{id}/stock/entries` | ADMIN | Entrada de estoque (`quantity`, `reason`, `reference?`) |
| `POST` | `/api/products/{id}/stock/withdrawals` | ADMIN | Saída de estoque (falha se saldo insuficiente) |
| `POST` | `/api/products/{id}/stock/adjustments` | ADMIN | Ajuste de saldo (`newStock`, `reason`) |
| `GET` | `/api/products/{id}/stock/movements` | ADMIN | Histórico de movimentações do produto |
| `GET` | `/api/products/low-stock` | ADMIN | Produtos com `stock <= minimumStock` |
| `GET` | `/api/stock/movements` | ADMIN | Movimentações globais (filtros: `productId`, `type`, `from`, `to`) |
| `GET` | `/api/stock/summary` | ADMIN | Resumo: totais, baixo estoque e produtos zerados |

### Carrinho — `/api/cart`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/cart` | autenticado | Carrinho com itens, quantidades e total |
| `POST` | `/api/cart/items` | autenticado | Adicionar item (`productId`, `quantity`) |
| `PATCH` | `/api/cart/items/{productId}` | autenticado | Alterar quantidade do item |
| `DELETE` | `/api/cart/items/{productId}` | autenticado | Remover item do carrinho |
| `DELETE` | `/api/cart` | autenticado | Esvaziar o carrinho |

### Checkout e pedidos — `/api/checkout`, `/api/orders` e `/api/admin/orders`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/checkout` | autenticado | Criar pedido + sessão de pagamento (`addressId?`) |
| `GET` | `/api/orders` | autenticado | Histórico de pedidos do usuário |
| `GET` | `/api/orders/{id}` | dono / ADMIN | Detalhe do pedido |
| `POST` | `/api/orders/{id}/cancel` | dono / ADMIN | Cancelar pedido pendente |
| `GET` | `/api/admin/orders` | ADMIN | Listar todos os pedidos |
| `GET` | `/api/admin/orders/{id}` | ADMIN | Detalhe de qualquer pedido |
| `PATCH` | `/api/admin/orders/{id}/ship` | ADMIN | Marcar como enviado (`trackingCode`) |
| `POST` | `/api/admin/orders/{id}/cancel` | ADMIN | Cancelar pedido (reembolso + devolução de estoque) |
| `POST` | `/api/payments/webhook` | público (assinatura validada) | Notificação de pagamento do Stripe |

### Cadastro do cliente — `/api/addresses` e `/api/phones`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` / `POST` | `/api/addresses`, `/api/addresses/{id}` | autenticado | Listar e criar endereços |
| `PUT` / `DELETE` | `/api/addresses/{id}` | autenticado | Atualizar e excluir endereço |
| `GET` / `POST` | `/api/phones`, `/api/phones/{id}` | autenticado | Listar e criar telefones |
| `PUT` / `DELETE` | `/api/phones/{id}` | autenticado | Atualizar e excluir telefone |

### Administração — `/api/users` e `/api/roles`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` / `POST` | `/api/users` | ADMIN | Listar e criar usuários |
| `GET` / `PUT` / `DELETE` | `/api/users/{id}` | ADMIN | Detalhar, atualizar e excluir usuário |
| `GET` | `/api/roles` | ADMIN | Listar perfis de acesso |

## Cache Redis

O Redis é utilizado como cache de leitura do catálogo via Spring Cache (`@Cacheable` / `@CacheEvict`). Dados de escrita frequente — carrinho, pedidos e estoque — continuam sendo lidos sempre do banco.

| Cache | Conteúdo | TTL |
|---|---|---|
| `products` | Listagens, produto por ID, por categoria, busca e ativos | 2 min |
| `categories` | Listagem e categorias por ID/nome | 10 min |

**Invalidação:**

- Criar, atualizar ou excluir um **produto** → esvazia o cache `products`.
- Criar, atualizar ou excluir uma **categoria** → esvazia `categories` e `products` (o DTO de produto embute a categoria).
- **Entrada, saída ou ajuste de estoque** → esvazia `products` (o DTO exibe o saldo). Isso cobre também a baixa automática do checkout via webhook do Stripe.

**Degradação graciosa:** se o Redis estiver indisponível, a falha é registrada no log e a consulta segue direto ao PostgreSQL (ver `CacheConfig.errorHandler()`).

```bash
# Inspecionando o cache
docker exec -it lojadbsol_redis redis-cli
> KEYS products*
> TTL products::1
> GET  products::1
```

## Pagamentos com Stripe

O checkout cria um pedido `PENDENTE` e uma **Checkout Session** no Stripe. A confirmação é assíncrona, via webhook `checkout.session.completed`:

1. O pedido transiciona `PENDENTE → PAGO` (idempotente — um mesmo `sessionId` não processa duas vezes).
2. A baixa de estoque acontece na mesma transação do webhook, registrando uma movimentação de `SAIDA` com a referência do pedido.
3. Se a baixa falhar, o pedido é `CANCELADO` e o pagamento é reembolsado automaticamente.

### Modo simulado (padrão no desenvolvimento)

Com `STRIPE_SIMULATE=true`, nenhuma chamada real é feita ao Stripe: o checkout devolve um `sessionId` fake (`cs_simulate_<orderId>`) e o pagamento pode ser concluído chamando o webhook manualmente:

```bash
curl -X POST http://localhost:8080/api/payments/webhook \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"cs_simulate_1","paymentMethod":"PIX"}'
```

### Pagamentos reais

Defina `STRIPE_SECRET_KEY` e `STRIPE_WEBHOOK_SECRET` no `.env`, altere `STRIPE_SIMULATE=false` e cadastre o webhook no [Stripe Dashboard](https://dashboard.stripe.com/webhooks) apontando para `https://seu-dominio/api/payments/webhook` no evento `checkout.session.completed`.

## Testes

```bash
# Com o PostgreSQL e o Redis locais rodando (docker compose up -d db redis)
STRIPE_SIMULATE=true ./mvnw test
```

- **48 testes** entre unitários (JUnit 5 + Mockito) e de integração (`@SpringBootTest`).
- Os testes de integração de pedidos utilizam o fluxo simulado do Stripe — por isso a suíte deve rodar com `STRIPE_SIMULATE=true`.

## Observabilidade

### Métricas (Prometheus + Grafana)

A API expõe métricas no formato Prometheus em `/actuator/prometheus` (Micrometer), coletadas pelo Prometheus e visualizadas no Grafana:

- **JVM** — memória, GC, threads, uptime.
- **HTTP** — latência e contagem por rota/status (`http_server_requests_*`).
- **Infraestrutura** — pool HikariCP, Redis (Lettuce), cache e contagem de logs por nível (`logback_events_total`).

```bash
docker compose up -d   # sobe também Prometheus (9090) e Grafana (3000)
```

| Ferramenta | URL | Acesso |
|---|---|---|
| Métricas cruas | `http://localhost:8080/actuator/prometheus` | público |
| Prometheus UI | `http://localhost:9090` | local |
| Grafana | `http://localhost:3000` | `admin` / `admin` (defina `GRAFANA_ADMIN_PASSWORD` no `.env`) |

O Grafana é provisionado automaticamente com o datasource Prometheus e a dashboard **JVM/Micrometer (ID 4701)** — ao abrir `http://localhost:3000`, basta navegar em *Dashboards*.

> **Nota de segurança:** `/actuator/prometheus` é público por design (necessário para o scrape). As métricas são agregadas — contagens e latências por rota — e não contêm dados sensíveis, mas revelam a superfície de endpoints da API. Se a API for exposta à internet, restrinja o acesso (ex.: Basic Auth ou `management.server.port` dedicado à rede interna).

### Actuator e logs

- **Spring Actuator** expõe `health` (incluindo os grupos `liveness` e `readiness`), `info`, `metrics`, `loggers` e `prometheus`. `health`, `info` e `prometheus` (alvo do scrape) são públicos; os demais exigem `ROLE_ADMIN`.
- **Informações da aplicação**: `/actuator/info` exibe nome, descrição e versão do projeto.
- **Logging estruturado** de requisições: cada log carrega o `requestId` da requisição e o `userId` autenticado, facilitando o rastreio de erros.
- O profile `prod` (`SPRING_PROFILES_ACTIVE=prod`) emite logs no formato ECS, pronto para ferramentas como Elastic Stack.

## Licença

Projeto desenvolvido para a **Loja do Sol**. Todos os direitos reservados.
