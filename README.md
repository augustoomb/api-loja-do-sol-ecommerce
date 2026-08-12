# ☀️ Loja do Sol — Backend Core API (e-Commerce Ecosystem)

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Container-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![OpenAPI](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

---

## 📌 Visão Geral & Contexto de Negócio

A **Loja do Sol API** é a solução backend principal projetada para suportar e modernizar a operação comercial e a presença digital da empresa **Loja do Sol Aquecedores**.

Trata-se de uma **API RESTful robusta e escalável**, concebida para integrar o ecossistema de e-commerce da marca. O sistema abrange desde a autenticação segura de clientes até a gestão transacional do catálogo de produtos, controle de estoque em tempo real e fluxo de checkout.

---

## 🏗️ Arquitetura e Organização do Projeto

A aplicação utiliza o padrão de **Arquitetura em Camadas (Layered Architecture)**, promovendo a separação clara de responsabilidades, alta coesão e facilidade de manutenção.

```text
api-loja-do-sol-ecommerce/
├── src/main/java/com/...
│   ├── config/        # Configurações globais (Security, Swagger, Redis, CORS)
│   ├── controller/    # Camada REST (Endpoints, DTOs e validação de requisições)
│   ├── model/         # Entidades de domínio JPA / Mapeamento do Banco de Dados
│   ├── repository/    # Camada de Persistência (Spring Data JPA)
│   └── service/       # Camada de Negócio (Regras, validações e orquestração)
```

### Principais Padrões e Decisões Técnicas
* **Separação por Camadas:** Isolamento total das regras de negócio na camada `service`, mantendo a camada `controller` focada exclusivamente na gestão dos contratos REST e requisições HTTP.
* **Cache Strategy:** Otimização da consulta ao catálogo via **Redis** para mitigar concorrência e reduzir chamadas repetitivas ao banco de dados relacional.
* **Segurança Centralizada:** Autenticação stateless via **Spring Security** com emissão e validação de tokens **JWT**.
* **Integridade Transacional:** Uso rigoroso de controle de transações (`@Transactional`) para garantir a consistência das operações de vendas e baixa de estoque.

---

## 🛠️ Tech Stack & Ferramentas

### Core Backend
* **Linguagem:** Java 17+
* **Framework:** Spring Boot 3.x (Spring MVC, Spring Data JPA, Spring Security, Spring Validation)
* **Persistência:** PostgreSQL & Hibernate ORM
* **Caching:** Redis

### Qualidade & Testes
* **Testes Unitários:** JUnit 5 & Mockito
* **Testes de Integração:** `@SpringBootTest` & Testcontainers (PostgreSQL & Redis)
* **Database Migrations:** Flyway / Liquibase

### DevOps & Documentação
* **Documentação Viva:** OpenAPI 3 / Swagger UI (`springdoc-openapi`)
* **Conteinerização:** Docker & Docker Compose

---

## ⚙️ Módulos do Sistema e Funcionalidades

### 1. Gestão de Acesso e Autenticação (IAM)
- [] Autenticação via login com emissão de tokens JWT.
- [x] Cadastro de novos usuários/clientes.
- [x] Perfis de acesso e autorização diferenciados (`ADMIN` e `CLIENTE`).

### 2. Catálogo & Estoque
- [x] CRUD completo de produtos e categorias.
- [x] Controle e baixa de estoque integrados para impedir vendas sem saldo disponível.
- [x] Livro de movimentações de estoque (entradas, saídas e ajustes) com histórico auditável e operador responsável.
- [x] Estoque mínimo por produto e listagem de produtos com saldo baixo.
- [x] SKU único por produto, com geração automática quando não informado.
- [x] Estratégia de invalidação de cache Redis ao atualizar produtos.

### 3. Carrinho & Checkout
- [x] Adição, alteração e remoção de itens no carrinho de compras (carrinho server-side por usuário autenticado).
- [ ] Cálculo automático de frete e valor total do pedido. (o total de itens é calculado no servidor; frete ainda não implementado)
- [x] Simulação de checkout e integração de pagamento (Stripe: Pix, cartão e boleto via Checkout Session + webhook).

### 4. Ciclo de Vida de Pedidos
- [x] Histórico e acompanhamento de status do pedido:
    - 🟡 `Pendente`
    - 🟢 `Pago`
    - 🔵 `Enviado`
    - 🔴 `Cancelado`

---

## 📦 Controle de Estoque (Módulo implementado)

O estoque é modelado como um **livro de movimentações** (`stock_movements`): entradas, saídas e ajustes geram um lançamento auditável (com motivo, referência, data e usuário operador). O saldo do produto (`products.stock`) é atualizado atomicamente na mesma transação, impedindo saldo negativo mesmo sob concorrência (condição `WHERE stock >= quantidade` + lock otimista `@Version`).

### Endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/products/{id}/stock/entries` | Entrada de estoque (`quantity`, `reason`, `reference?`) |
| `POST` | `/api/products/{id}/stock/withdrawals` | Saída de estoque (falha se saldo insuficiente) |
| `POST` | `/api/products/{id}/stock/adjustments` | Ajuste de saldo (`newStock`, `reason`) — registra o delta |
| `GET` | `/api/products/{id}/stock/movements` | Histórico de movimentações do produto |
| `GET` | `/api/products/low-stock` | Produtos com `stock <= minimumStock` |
| `GET` | `/api/stock/movements` | Movimentações globais (filtros: `productId`, `type`, `from`, `to`) |
| `GET` | `/api/stock/summary` | Resumo: total de produtos/unidades, baixo estoque e zerados |

Todos os endpoints de estoque exigem a role `ROLE_ADMIN`. O produto também ganhou `sku` (único, com geração automática) e `minimumStock` (saldo mínimo para alerta).

---

## ⚡ Cache Redis (Módulo implementado)

O **Redis** é usado como cache de leitura do catálogo via **Spring Cache** (`@Cacheable` / `@CacheEvict`). Apenas dados de **baixa frequência de escrita** são cacheados; carrinho, pedidos e estoque continuam lendo **sempre do banco** (sem risco de dado defasado).

### O que é cacheado

| Cache | Conteúdo | TTL |
|---|---|---|
| `products` | Listagens, produto por ID, por categoria, busca e ativos | 2 min |
| `categories` | Listagem e categorias por ID/nome | 10 min |

### Quando o cache é invalidado

* Criar, atualizar ou excluir um **produto** → cache `products` é esvaziado.
* Criar, atualizar ou excluir uma **categoria** → caches `categories` e `products` são esvaziados (o DTO de produto embute a categoria).
* **Entrada, saída ou ajuste de estoque** → cache `products` é esvaziado (o DTO de produto exibe o saldo). Isso cobre também a baixa automática de estoque do checkout/webhook do Stripe.

### Degradação graciosa

Se o Redis estiver fora do ar, a aplicação **continua funcionando**: a falha é registrada no log e a consulta segue direto ao PostgreSQL (ver `CacheConfig.errorHandler()`).

### Inspecionando o cache

```bash
docker exec -it lojadbsol_redis redis-cli
> KEYS products*
> TTL products::1      # tempo de vida restante de uma chave
> GET  products::1     # valor serializado em JSON
```

* **Configuração:** `src/main/java/com/augustoomb/api_loja_do_sol_ecommerce/config/CacheConfig.java`
* **Variáveis de ambiente:** `REDIS_HOST` (padrão `localhost`) e `REDIS_PORT` (padrão `6379`)
* **Teste:** `CacheIntegrationTest` (verifica popular/invalidar o cache)

---

## 🛒 Vendas, Carrinho & Checkout (Módulo implementado)

O carrinho é **server-side** e vinculado ao usuário autenticado (`carts` / `cart_items`): o total é sempre recalculado no servidor a partir do preço atual dos produtos, e a quantidade máxima por item respeita o saldo em estoque.

O **checkout** cria um pedido `PENDENTE` (com snapshot do endereço de entrega e do preço dos itens em `order_items`) e uma **Checkout Session** no Stripe, esvaziando o carrinho. O pagamento é confirmado de forma **assíncrona pelo webhook** `checkout.session.completed`:

1. Pedido transiciona `PENDENTE → PAGO` (idempotente — um mesmo `sessionId` não cobra/processa duas vezes).
2. A baixa de estoque acontece na **mesma transação** do webhook, reutilizando a regra anti-saldo-negativo do módulo de estoque e registrando uma movimentação `SAIDA` com `reference` = id do pedido.
3. Se a baixa falhar, o pedido é `CANCELADO` e o pagamento é **reembolsado** automaticamente no Stripe.

O **cancelamento de um pedido pago** (pelo admin) reembolsa o cliente no Stripe e **devolve o estoque** (movimentação `ENTRADA`).

### Modo simulado (desenvolvimento)

Com `STRIPE_SIMULATE=true` (padrão no `.env`) **nenhuma chamada real é feita ao Stripe**: o checkout devolve um `sessionId` fake (`cs_simulate_<orderId>`) e o pagamento é concluído chamando o webhook manualmente:

```bash
curl -X POST http://localhost:8080/api/payments/webhook \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"cs_simulate_1","paymentMethod":"PIX"}'
```

Para **pagamentos reais**, preencha `STRIPE_SECRET_KEY` e `STRIPE_WEBHOOK_SECRET` no `.env`, defina `STRIPE_SIMULATE=false` e cadastre o webhook no Stripe Dashboard (`Developers → Webhooks`) apontando para `https://seu-dominio/api/payments/webhook` no evento `checkout.session.completed`.

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `STRIPE_SIMULATE` | `true` | `true` = modo simulado (sem chamadas reais) |
| `STRIPE_SECRET_KEY` | *(vazia)* | Chave secreta do Stripe (`sk_test_...` / `sk_live_...`) |
| `STRIPE_WEBHOOK_SECRET` | *(vazia)* | Secret do webhook (`whsec_...`) usado para validar assinatura |
| `FRONTEND_URL` | `http://localhost:5173` | URLs de sucesso/cancelamento do Checkout |

### Endpoints

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/cart` | autenticado | Carrinho com itens, quantidades e total |
| `POST` | `/api/cart/items` | autenticado | Adicionar item (`productId`, `quantity`) |
| `PATCH` | `/api/cart/items/{productId}` | autenticado | Alterar quantidade do item |
| `DELETE` | `/api/cart/items/{productId}` | autenticado | Remover item do carrinho |
| `DELETE` | `/api/cart` | autenticado | Esvaziar o carrinho |
| `POST` | `/api/checkout` | autenticado | Criar pedido + sessão de pagamento (`addressId?` — padrão: endereço principal) |
| `GET` | `/api/orders` | autenticado | Histórico de pedidos do usuário |
| `GET` | `/api/orders/{id}` | dono / ADMIN | Detalhe do pedido |
| `POST` | `/api/orders/{id}/cancel` | dono / ADMIN | Cancelar pedido pendente |
| `GET` | `/api/admin/orders` | ADMIN | Listar todos os pedidos |
| `GET` | `/api/admin/orders/{id}` | ADMIN | Detalhe de qualquer pedido |
| `PATCH` | `/api/admin/orders/{id}/ship` | ADMIN | Marcar como enviado (`trackingCode`) |
| `POST` | `/api/admin/orders/{id}/cancel` | ADMIN | Cancelar pedido (reembolso + devolução de estoque) |
| `POST` | `/api/payments/webhook` | público (assinatura validada) | Notificação de pagamento do Stripe |

---

## 💎 Diferenciais da Engenharia do Projeto

* ⚡ **Desempenho com Cache Redis:** Consultas de leitura frequente (como a listagem de produtos) são armazenadas em memória, reduzindo drasticamente a latência e a carga no PostgreSQL.
* 🛡️ **Segurança Robusta:** Proteção contra acessos indevidos utilizando filtros encadeados do Spring Security com autenticação JWT sem estado (*stateless*).
* 🐳 **Ambiente Conteinerizado:** A aplicação, banco de dados e servidor Redis estão totalmente configurados via `docker-compose.yml`, permitindo subir o ambiente com um único comando.
* 📄 **Documentação Interativa:** Swagger UI integrado, permitindo que a equipe frontend ou integradores externos testem os contratos da API diretamente pelo navegador.

---

## 🚦 Como Executar a Aplicação

### Pré-requisitos
* [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/) instalados.

### Passo a Passo

1. **Clonar o repositório:**
   ```bash
   git clone https://github.com/seu-usuario/api-loja-do-sol-ecommerce.git
   cd api-loja-do-sol-ecommerce
   ```

2. **Configurar variáveis de ambiente:**
   Copie o arquivo de exemplo para criar o seu `.env` local:
   ```bash
   cp .env.example .env
   ```

   *Certifique-se de preencher o arquivo `.env` com suas credenciais do banco de dados e chave secreta do JWT.*

3. **Subir a aplicação com Docker Compose:**
   ```bash
   docker compose up -d
   ```

4. **Acessar a Documentação (Swagger UI):**
   Com os containers ativos, acesse no navegador:
   ```text
   http://localhost:8080/swagger-ui/index.html
   ```

---

## 📄 Licença
Este projeto foi desenvolvido para atender às demandas de software da **Loja do Sol**. Todos os direitos reservados.
