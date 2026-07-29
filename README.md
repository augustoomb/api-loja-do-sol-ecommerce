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
- [ ] CRUD completo de produtos e categorias.
- [ ] Controle e baixa de estoque integrados para impedir vendas sem saldo disponível.
- [ ] Estratégia de invalidação de cache Redis ao atualizar produtos.

### 3. Carrinho & Checkout
- [ ] Adição, alteração e remoção de itens no carrinho de compras.
- [ ] Cálculo automático de frete e valor total do pedido.
- [ ] Simulação de checkout e integração de pagamento.

### 4. Ciclo de Vida de Pedidos
- [ ] Histórico e acompanhamento de status do pedido:
    - 🟡 `Pendente`
    - 🟢 `Pago`
    - 🔵 `Enviado`
    - 🔴 `Cancelado`

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
