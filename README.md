<div align="center">

# 🛡️ Dumply-UC

### Sistema de Autenticação Segura com Spring Boot

*Projeto acadêmico desenvolvido para a disciplina de Segurança da Informação — Universidade de Mogi das Cruzes UMC*

---

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square)
![License](https://img.shields.io/badge/license-Acadêmica-lightgrey?style=flat-square)
![Last Commit](https://img.shields.io/github/last-commit/Joca0/Dumply-UC?style=flat-square)

</div>

---

## 📖 Sobre o Projeto

O **Dumply-UC** é uma aplicação back-end construída com **Spring Boot 3.4.1** que implementa práticas modernas de **Segurança da Informação**. O projeto contempla autenticação baseada em **JWT**, **autenticação de dois fatores (2FA)** via Google Authenticator, proteção contra ataques de *brute-force*, validação de documentos brasileiros e gerenciamento de tokens com **Redis**.

> Este repositório foi criado como parte das entregas das atividades da disciplina de **Segurança da Informação**.

---

## 📑 Sumário

1. [Sobre o Projeto](#-sobre-o-projeto)
2. [Principais Funcionalidades](#-principais-funcionalidades)
3. [Arquitetura & Tecnologias](#-arquitetura--tecnologias)
4. [Estrutura de Diretórios](#-estrutura-de-diretórios)
5. [Dependências do Projeto](#-dependências-do-projeto)
6. [Pré-requisitos](#-pré-requisitos)
7. [Instalação e Execução](#-instalação-e-execução)
8. [Configuração de Ambientes](#-configuração-de-ambientes)
9. [Testes](#-testes)
10. [Contribuição](#-contribuição)
11. [Licença](#-licença)
12. [Autor](#-autor)

---

## ✨ Principais Funcionalidades

- 🔐 **Autenticação JWT** — tokens seguros e stateless com biblioteca Auth0
- 📱 **Autenticação de Dois Fatores (2FA)** — integração com Google Authenticator (TOTP)
- 🛡️ **Proteção contra Brute-Force** — bloqueio de tentativas suspeitas de login
- 📧 **Envio de E-mails** — notificações transacionais via SMTP
- 🗂️ **Blacklist de Tokens** — controle de tokens revogados com Redis
- 🇧🇷 **Validação de Documentos Brasileiros** — CPF, CNPJ e outros (Caelum Stella)
- 🐘 **Suporte Multi-Banco** — PostgreSQL, Oracle e H2 (para testes)
- 🐳 **Containerização** — pronto para execução com Docker Compose

---

## 🏗️ Arquitetura & Tecnologias

<table>
  <tr>
    <td><b>Linguagem</b></td>
    <td>Java 17</td>
  </tr>
  <tr>
    <td><b>Framework</b></td>
    <td>Spring Boot 3.4.1</td>
  </tr>
  <tr>
    <td><b>Build Tool</b></td>
    <td>Apache Maven</td>
  </tr>
  <tr>
    <td><b>Persistência</b></td>
    <td>Spring Data JPA / Hibernate</td>
  </tr>
  <tr>
    <td><b>Bancos de Dados</b></td>
    <td>PostgreSQL · Oracle · H2</td>
  </tr>
  <tr>
    <td><b>Cache / Sessão</b></td>
    <td>Redis</td>
  </tr>
  <tr>
    <td><b>Segurança</b></td>
    <td>Spring Security · JWT (Auth0) · Google Authenticator</td>
  </tr>
  <tr>
    <td><b>Containerização</b></td>
    <td>Docker · Docker Compose</td>
  </tr>
  <tr>
    <td><b>Utilitários</b></td>
    <td>Lombok · Bean Validation · Caelum Stella</td>
  </tr>
</table>

---

## 📂 Estrutura de Diretórios

```bash
Dumply-UC/
│
├── 📄 Dockerfile                   # Imagem Docker da aplicação
├── 📄 docker-compose.yml           # Orquestração dos contêineres
├── 📄 pom.xml                      # Gerenciamento de dependências Maven
├── 📄 requisitos.md                # Requisitos funcionais e não funcionais
├── 📄 mvnw | mvnw.cmd              # Maven Wrapper (Unix / Windows)
│
└── 📁 src/
    │
    ├── 📁 main/
    │   ├── 📁 java/com/dumply/
    │   │   │
    │   │   ├── 📁 config/security/
    │   │   │   ├── SecurityConfig.java        # Configuração do Spring Security
    │   │   │   ├── SecurityFilter.java        # Filtro de autenticação JWT
    │   │   │   └── TokenService.java          # Geração e validação de tokens
    │   │   │
    │   │   ├── 📁 controller/
    │   │   │   └── AuthController.java        # Endpoints REST de autenticação
    │   │   │
    │   │   ├── 📁 model/
    │   │   │   └── User.java                  # Entidade de usuário
    │   │   │
    │   │   ├── 📁 service/
    │   │   │   ├── AuthService.java           # Regras de negócio — autenticação
    │   │   │   ├── EmailService.java          # Serviço de envio de e-mails
    │   │   │   └── TokenBlacklistService.java # Gerenciamento de tokens revogados
    │   │   │
    │   │   └── DumplyApplication.java         # Classe principal (entry-point)
    │   │
    │   └── 📁 resources/
    │       ├── application-dev.properties     # Configurações — ambiente DEV
    │       └── application-prod.properties    # Configurações — ambiente PROD
    │
    └── 📁 test/java/com/dumply/
        ├── 📁 service/
        │   └── AuthServiceBruteForceTest.java # Teste — proteção contra brute-force
        └── DumplyApplicationTests.java        # Teste de inicialização da aplicação
```

---

## 📦 Dependências do Projeto

### 🏛️ Parent POM

| GroupId | ArtifactId | Versão |
|---------|------------|:------:|
| `org.springframework.boot` | `spring-boot-starter-parent` | **3.4.1** |

### ⚙️ Propriedades de Build

| Propriedade | Valor |
|-------------|:-----:|
| `java.version` | `17` |
| `project.build.sourceEncoding` | `UTF-8` |
| `project.reporting.outputEncoding` | `UTF-8` |

### 🔧 Dependências Spring Boot Starters

| Artifact | Escopo | Finalidade |
|----------|:------:|------------|
| `spring-boot-starter-web` | — | Construção de APIs REST |
| `spring-boot-starter-data-jpa` | — | Persistência com JPA / Hibernate |
| `spring-boot-starter-security` | — | Infraestrutura de segurança |
| `spring-boot-starter-data-redis` | — | Integração com Redis |
| `spring-boot-starter-validation` | — | Bean Validation (Jakarta) |
| `spring-boot-starter-mail` | — | Envio de e-mails via SMTP |
| `spring-boot-starter-test` | `test` | Framework de testes unitários |
| `spring-boot-devtools` | `runtime` | Hot-reload em desenvolvimento |

### 🗄️ Drivers de Banco de Dados

| GroupId | ArtifactId | Versão | Escopo |
|---------|------------|:------:|:------:|
| `org.postgresql` | `postgresql` | — | `runtime` |
| `com.h2database` | `h2` | — | `runtime` |
| `com.oracle.database.jdbc` | `ojdbc8` | `21.11.0.0` | — |
| `com.oracle.database.security` | `oraclepki` | `21.11.0.0` | — |
| `com.oracle.database.security` | `osdt_cert` | `21.11.0.0` | — |
| `com.oracle.database.security` | `osdt_core` | `21.11.0.0` | — |

### 🔐 Segurança e Autenticação

| GroupId | ArtifactId | Versão | Finalidade |
|---------|------------|:------:|------------|
| `com.auth0` | `java-jwt` | `4.4.0` | Geração e validação de JWT |
| `com.warrenstrange` | `googleauth` | `1.5.0` | 2FA via Google Authenticator (TOTP) |
| `org.springframework.security` | `spring-security-test` | — | Testes de segurança |

### 🇧🇷 Validação de Documentos Brasileiros

| GroupId | ArtifactId | Versão |
|---------|------------|:------:|
| `br.com.caelum.stella` | `caelum-stella-core` | `2.1.6` |
| `com.jereztech` | `validation-br-api` | `1.3` |

### 🛠️ Utilitários

| GroupId | ArtifactId | Escopo |
|---------|------------|:------:|
| `org.projectlombok` | `lombok` | `optional` |

### 🔌 Plugins de Build

| Plugin | Finalidade |
|--------|------------|
| `maven-compiler-plugin` | Processamento de anotações (Lombok) |
| `spring-boot-maven-plugin` | Empacotamento e execução da aplicação |

---

## 🧰 Pré-requisitos

Antes de iniciar, certifique-se de ter instalado:

- ☕ **JDK 17** ou superior
- 🧱 **Maven 3.9+** *(ou utilize o Maven Wrapper incluído)*
- 🐳 **Docker** e **Docker Compose** *(opcional, mas recomendado)*
- 📝 **Git**

---

## 🚀 Instalação e Execução

### 1️⃣ Clonando o repositório

```bash
git clone https://github.com/Joca0/Dumply-UC.git
cd Dumply-UC
```

### 2️⃣ Execução local (Maven Wrapper)

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

### 3️⃣ Execução via Docker Compose

```bash
docker-compose up --build
```

A aplicação estará disponível em: **`http://localhost:8080`**

---

## ⚙️ Configuração de Ambientes

O projeto disponibiliza dois perfis de configuração:

| Arquivo | Perfil | Descrição |
|---------|:------:|-----------|
| `application-dev.properties` | `dev` | Ambiente de desenvolvimento |
| `application-prod.properties` | `prod` | Ambiente de produção |

Para ativar um perfil específico:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

> 💡 **Dica:** Variáveis sensíveis (senhas, secrets JWT, credenciais SMTP) devem ser definidas via **variáveis de ambiente**, nunca versionadas no repositório.

---

## 🧪 Testes

Para executar a suíte de testes:

```bash
./mvnw test
```

Testes disponíveis:

- ✅ `DumplyApplicationTests` — validação do contexto Spring
- ✅ `AuthServiceBruteForceTest` — proteção contra tentativas repetidas de login

---

## 🤝 Contribuição

Contribuições são bem-vindas! Para contribuir:

1. Faça um **fork** do projeto
2. Crie uma **branch** para sua feature (`git checkout -b feature/minha-feature`)
3. **Commit** suas mudanças (`git commit -m 'feat: adiciona minha feature'`)
4. **Push** para a branch (`git push origin feature/minha-feature`)
5. Abra um **Pull Request**

---

## 📜 Licença

Este projeto foi desenvolvido para fins **acadêmicos**, como parte da disciplina de Segurança da Informação da **Universidade Cruzeiro do Sul**.

---

## 👤 Autor

<div align="center">

**Joca0**

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Joca0)
[![Repositório](https://img.shields.io/badge/Dumply--UC-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Joca0/Dumply-UC)

---

⭐ *Se este projeto foi útil, considere deixar uma estrela no repositório!*

</div>
