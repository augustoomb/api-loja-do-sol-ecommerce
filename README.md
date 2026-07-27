# 🛒 API de E-commerce Completa (Java / Spring Boot)

> Uma vitrine robusta para o seu portfólio backend, demonstrando domínio sobre fluxos comerciais reais, regras de negócio complexas e controle de concorrência.

---

## 🚀 Sobre o Projeto

Este projeto consiste em uma **API RESTful de E-commerce** desenvolvida com foco em boas práticas de mercado, arquitetura limpa e alta performance. O sistema simula o ecossistema completo de uma loja virtual, cobrindo desde a autenticação de usuários até o processamento e rastreio de pedidos, utilizando tecnologias modernas do ecossistema Java.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem & Framework:** `Java 17+` | `Spring Boot 3`
* **Persistência:** `PostgreSQL` | `Spring Data JPA / Hibernate`
* **Segurança:** `Spring Security` | `Tokens JWT`
* **Cache:** `Redis` (via `Spring Cache`)
* **Documentação:** `OpenAPI` / `Swagger` (`springdoc-openapi`)
* **DevOps & Infraestrutura:** `Docker` | `Docker Compose`

---

## 📋 Requisitos e Funcionalidades

### 👤 1. Módulo de Usuários e Autenticação
* Cadastro de novos usuários.
* Autenticação via login com emissão de tokens JWT.
* Perfis de acesso diferenciados (**Admin** e **Cliente**).

### 📦 2. Catálogo e Estoque
* CRUD completo de produtos e categorias.
* Gerenciamento de estoque integrado para **evitar vendas sem saldo disponível**.

### 🛍️ 3. Carrinho e Checkout
* Adição e remoção de itens do carrinho.
* Cálculo automático do valor total.
* Simulação de pagamento e finalização de compra.

### 📦 4. Histórico de Pedidos
* Acompanhamento do ciclo de vida do pedido através de status:
  * 🟡 `Pendente`
  * 🟢 `Pago`
  * 🔵 `Enviado`
  * 🔴 `Cancelado`

---

## 💎 Diferenciais Técnicos

* ⚡ **Cache com Redis:** Utilizado no catálogo de produtos para otimizar consultas frequentes e reduzir a carga no banco de dados relacional.
* 📄 **Documentação Interativa:** Swagger UI integrado para testes rápidos e visualização clara dos endpoints da API.
* 🐳 **Conteinerização Completa:** Ambiente 100% reproduzível com Docker e Docker Compose, permitindo subir a aplicação, banco de dados e cache com um único comando.

---

## ⚙️ Como Executar o Projeto

### Pré-requisitos
* [Docker](https://www.docker.com/) instalado na sua máquina.
* [Docker Compose](https://docs.docker.com/compose/) instalado.

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone [https://github.com/seu-usuario/seu-repositorio.git](https://github.com/seu-usuario/seu-repositorio.git)
   cd seu-repositorio

2. **Acesse a documentação da API:**
Com a aplicação em execução, acesse pelo navegador:
   ```bash
   http://localhost:8080/swagger-ui/index.html
