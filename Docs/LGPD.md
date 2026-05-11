# Conformidade com a Lei Geral de Proteção de Dados (LGPD)

Este documento descreve a implementação dos controles de privacidade e proteção de dados pessoais no projeto **Dumply-UC**, em atendimento à **Lei nº 13.709/2018 (LGPD)** e aos requisitos 4.1 a 4.11 da disciplina de Segurança da Informação — Universidade de Mogi das Cruzes (UMC).

> **Controlador dos Dados:** Dumply-UC — Projeto Acadêmico
> **Encarregado pelo Tratamento de Dados (DPO):** [Érico Marin] — `dpo@dumply.local`
> **Versão do documento:** 1.1
> **Última atualização:** 2026-05-11

---

## 1. Introdução

A LGPD estabelece princípios e obrigações para o tratamento de dados pessoais por pessoas físicas e jurídicas, com o objetivo de proteger os direitos fundamentais de liberdade, privacidade e o livre desenvolvimento da personalidade do titular (Art. 1º). O presente documento sistematiza, sob a ótica técnica, como o sistema **Dumply-UC** materializa tais obrigações em sua arquitetura, modelagem de dados e fluxos de processamento.

A abordagem adotada segue o paradigma **Privacy by Design** (Cavoukian, 2011), incorporando a proteção de dados desde a concepção do software, bem como o paradigma **Privacy by Default**, garantindo que as configurações mais restritivas sejam aplicadas por padrão.

### 1.1 Arquitetura Multi-Tenant e seu Impacto na LGPD

O Dumply-UC adota arquitetura **multi-tenant** baseada em duas entidades principais: `User` (pessoa física — titular de dados pessoais) e `Company` (pessoa jurídica — cliente contratante). Embora a LGPD, em seu Art. 1º, se aplique ao tratamento de dados de pessoas naturais, este documento adota o **princípio da cautela** e estende controles equivalentes aos dados da entidade `Company` nos casos em que possa haver vinculação indireta a pessoa física (ex.: MEI, EIRELI, empresário individual cujo nome empresarial coincide com o nome civil).

---

## 2. Mapeamento dos Requisitos × Implementação

| Req. | Item | Mecanismo | Artefato no Repositório |
|---|---|---|---|
| 4.1 | Listagem de dados pessoais | Tabelas enumerativas (Seção 4.1) | `User.java`, `Company.java` e este documento |
| 4.2 | Finalidade dos dados | Tabela finalidade × base legal | Seção 4.2 |
| 4.3 | Minimização | Princípio Privacy by Design | `User.java`, `Company.java`, `ProfileDTO.java` |
| 4.4 | Registro de consentimento | Entidade `ConsentLog` | `ConsentLog.java` |
| 4.5 | Consentimento × finalidade | Coluna `finalidade` na `consent_log` | `ConsentLog.java` |
| 4.6 | Revogação | `DELETE /lgpd/consent` | `LgpdController.java` |
| 4.7 | Data e versão | Colunas `timestamp` e `version` | `ConsentLog.java` |
| 4.8 | Consulta | `GET /lgpd/me/data` | `LgpdController.java` |
| 4.9 | Exportação | `GET /lgpd/me/export` | `LgpdController.java` |
| 4.10 | Exclusão | `DELETE /lgpd/me` | `LgpdController.java` |
| 4.11 | Fluxo documentado | Seção 4.11 deste documento | `LGPD.md` |

---

## 4.1 Listagem Completa dos Dados Pessoais Coletados

A aplicação coleta apenas os dados pessoais estritamente necessários à autenticação segura do titular, ao funcionamento da arquitetura multi-tenant e à gestão do ciclo de vida contratual da empresa contratante. Os atributos pessoais estão concentrados na entidade `User` (`src/main/java/com/dumply/model/User.java`); os atributos relativos à pessoa jurídica encontram-se na entidade `Company` (`src/main/java/com/dumply/model/Company.java`); a auditoria de manifestações de vontade é mantida na entidade `ConsentLog`.

### 4.1.1 Inventário dos Dados Pessoais (Entidade `User`)

| Atributo | Tipo | Categoria LGPD (Art. 5º) | Forma de Armazenamento | Origem |
|---|---|---|---|---|
| `id` | UUID | Identificador técnico interno | Texto / UUID | Gerado pelo sistema |
| `email` | String | Dado pessoal (Art. 5º, I) | Texto em claro | Informado pelo titular |
| `fullName` | String | Dado pessoal (Art. 5º, I) | Texto em claro | Informado pelo titular |
| `document` | String (CPF/CNPJ) | Dado pessoal — identificador civil | Texto em claro | Informado pelo titular |
| `password` | String | Credencial de autenticação | **Hash BCrypt + salt aleatório** | Definido pelo titular |
| `secret2fa` | String | Credencial — segredo TOTP | Texto (revogável) | Gerado pelo sistema |
| `is2faEnabled` | Boolean | Atributo de configuração | Boolean | Sistema |
| `firstLogin` | Boolean | Telemetria funcional | Boolean | Sistema |
| `failedLoginAttempts` | Integer | Telemetria de segurança | Inteiro | Sistema |
| `locktime` | LocalDateTime | Telemetria de segurança | Timestamp | Sistema |
| `passwordResetToken` | String | Token temporário (TTL 1h) | Texto (UUID) | Sistema |
| `passwordResetExpiresAt` | LocalDateTime | Carimbo de expiração | Timestamp | Sistema |
| `accountNonLocked` | Boolean | Estado da conta | Boolean | Sistema |
| `disable2faCode` | String | Código transacional (one-time) | Texto | Sistema |
| `role` | Enum | Atribuição funcional | Texto (ADMIN, USER, etc.) | Sistema |
| `consentGiven` | Boolean | Registro de manifestação de vontade | Boolean | Titular |
| `consentGivenAt` | LocalDateTime | Carimbo temporal do consentimento | Timestamp | Sistema |
| `consentVersion` | String | Versão do termo aceito | Texto (ex.: "1.0") | Sistema |
| `company_id` (FK) | UUID | Vínculo organizacional (tenant) | UUID | Sistema |

### 4.1.2 Inventário dos Dados Contratuais (Entidade `Company`)

A entidade `Company` representa a pessoa jurídica cliente. Embora dados de pessoa jurídica não estejam, em regra, sob a tutela direta da LGPD (Art. 1º), o sistema adota controles equivalentes em razão da possível vinculação indireta com pessoa física (microempreendedor individual, empresário individual).

| Atributo | Tipo | Natureza | Forma de Armazenamento | Origem |
|---|---|---|---|---|
| `id` | UUID | Identificador técnico interno | Texto / UUID | Gerado pelo sistema |
| `name` | String | Razão social ou nome fantasia | Texto em claro | Informado pelo administrador |
| `slug` | String | Identificador público único da empresa | Texto em claro | Informado/gerado |
| `status` | Enum (`CompanyStatus`) | Estado contratual (TRIAL, ACTIVE, SUSPENDED, etc.) | Texto | Sistema |
| `trialEndsAt` | LocalDateTime | Data de término do período de avaliação | Timestamp | Sistema |
| `createdAt` | LocalDateTime | Carimbo temporal de criação | Timestamp | Sistema |

**Observação sobre o `slug`:** em casos de MEI ou empresário individual, o `slug` pode acidentalmente revelar a identidade da pessoa física (ex.: `joao-silva-mei`). Recomenda-se que, no momento do cadastro, a aplicação alerte o administrador quanto ao caráter público desse identificador.

### 4.1.3 Inventário dos Dados de Auditoria (Entidade `ConsentLog`)

| Atributo | Tipo | Finalidade |
|---|---|---|
| `id` | UUID | Identificador do evento |
| `userId` | UUID | Referência ao titular |
| `action` | String | "GRANT" ou "REVOKE" |
| `finalidade` | String | Finalidade específica do consentimento |
| `version` | String | Versão do termo aceito |
| `timestamp` | LocalDateTime | Carimbo temporal UTC |
| `ipAddress` | String | IP de origem da manifestação |
| `userAgent` | String | Navegador/dispositivo do titular |

### 4.1.4 Dados Pessoais **Não Coletados**

Em obediência ao princípio da necessidade (Art. 6º, III da LGPD), o sistema **não coleta**: endereço residencial, número de telefone, data de nascimento, gênero, dados biométricos, geolocalização, dados de cartão de crédito, ou qualquer dado pessoal sensível previsto no Art. 5º, II (origem racial, convicção religiosa, opinião política, filiação sindical, dado referente à saúde, à vida sexual, dado genético ou biométrico).

---

## 4.2 Associação de Cada Dado a uma Finalidade

Em conformidade com o princípio da finalidade (Art. 6º, I da LGPD), cada dado pessoal coletado está vinculado a um propósito legítimo, explícito e informado ao titular, com base legal devidamente identificada (Art. 7º).

### 4.2.1 Finalidades dos Dados Pessoais (`User`)

| Dado | Finalidade | Base Legal (Art. 7º LGPD) |
|---|---|---|
| `email` | Identificação do titular no login e envio de mensagens transacionais (recuperação de senha, código 2FA) | V — Execução de contrato |
| `fullName` | Personalização da interface e identificação humana do titular | V — Execução de contrato |
| `document` | Identificação civil única no contexto multi-tenant; validação via Caelum Stella | II — Cumprimento de obrigação legal/regulatória |
| `password` (hash) | Autenticação primária do titular | V — Execução de contrato |
| `secret2fa` | Autenticação de dois fatores (TOTP) — camada adicional de segurança | IX — Legítimo interesse (segurança do titular) |
| `failedLoginAttempts`, `locktime`, `accountNonLocked` | Proteção contra ataques de força bruta | IX — Legítimo interesse (segurança) |
| `passwordResetToken`, `passwordResetExpiresAt` | Habilitar recuperação de senha de forma segura | V — Execução de contrato |
| `disable2faCode` | Permitir desativação segura do 2FA | IX — Legítimo interesse (segurança) |
| `role` | Controle de autorização (RBAC) | V — Execução de contrato |
| `consentGiven`, `consentGivenAt`, `consentVersion` | Comprovar manifestação de vontade do titular | II — Cumprimento de obrigação legal e princípio da prestação de contas (Art. 6º, X) |
| `company_id` | Isolamento de dados em arquitetura multi-tenant | V — Execução de contrato |

### 4.2.2 Finalidades dos Dados da Pessoa Jurídica (`Company`)

| Dado | Finalidade | Base Legal |
|---|---|---|
| `name` | Identificação da empresa contratante em telas, comunicações e faturas | V — Execução de contrato |
| `slug` | Geração de URLs amigáveis por tenant e identificação pública | V — Execução de contrato |
| `status` | Gestão do ciclo de vida contratual (trial, ativo, suspenso) | V — Execução de contrato |
| `trialEndsAt` | Controle do período de avaliação gratuita | V — Execução de contrato |
| `createdAt` | Auditoria e métricas de adesão | IX — Legítimo interesse (gestão administrativa) |

---

## 4.3 Evidência de Minimização de Dados

A minimização foi instituída como princípio arquitetural, em conformidade com o Art. 6º, III da LGPD (necessidade — limitação do tratamento ao mínimo necessário para a realização de suas finalidades).

### 4.3.1 Evidências no Código

1. **Coleta restrita no cadastro:** o `RegisterRequestDTO` exige apenas quatro atributos pessoais (`email`, `fullName`, `document`, `password`).
2. **Ausência de campos invasivos no `User`:** a entidade não armazena telefone, endereço, data de nascimento, gênero ou dados sensíveis.
3. **Entidade `Company` enxuta:** apenas seis atributos, todos estritamente necessários à gestão contratual; não há campos como CNPJ separado, inscrição estadual, endereço fiscal ou telefone corporativo no escopo atual.
4. **Hash unidirecional de credenciais:** a senha jamais é persistida em claro — armazena-se apenas seu hash BCrypt, gerado por `PasswordEncoder` (configurado em `SecurityConfig.java`).
5. **Tokens com TTL curto:** `passwordResetToken` expira em 1 hora; tokens JWT podem ser revogados via blacklist no Redis (`TokenBlacklistService`).
6. **DTO de resposta enxuto:** o endpoint `GET /auth/me` retorna apenas um `ProfileDTO` (`fullName`, `role`, `firstLogin`, `is2faEnabled`), omitindo dados sensíveis como `password`, `secret2fa`, `document`.
7. **Logs sem dados sensíveis:** as operações registradas via SLF4J em `AuthService.java` contêm apenas o e-mail e o resultado das operações, jamais a senha ou o segredo 2FA.
8. **Mascaramento de CPF:** nos endpoints de consulta, o `document` é apresentado com mascaramento parcial (`***.***.***-**`).
9. **Isolamento por tenant:** consultas ao `UserRepository` são sempre filtradas por `companyId` (vide `findByEmailAndCompanyId` em `AuthService`), evitando vazamento cruzado entre empresas.

### 4.3.2 Justificativa Técnica

A minimização reduz a superfície de exposição a incidentes de segurança (data breach), em consonância com o princípio da segurança (Art. 6º, VII) e com as recomendações da norma ISO/IEC 27701:2019 e do NIST Privacy Framework (Function: PROTECT — PR.DS).

---

## 4.4 Registro Explícito de Consentimento

O consentimento é coletado de modo **livre, informado, inequívoco e específico**, conforme define o Art. 5º, XII da LGPD, no momento do cadastro (endpoint `POST /auth/register`).

### 4.4.1 Campos Persistidos na Entidade `User`

```java
private Boolean consentGiven;              // true = aceite explícito
private LocalDateTime consentGivenAt;      // carimbo temporal UTC
private String consentVersion;             // versão do termo aceito
private LocalDateTime consentRevokedAt;    // null enquanto ativo
```

### 4.4.2 Fluxo de Coleta do Consentimento

1. O titular acessa a tela de cadastro, na qual é apresentado o **link para a Política de Privacidade** vigente.
2. O titular marca uma **caixa de seleção obrigatória, não pré-marcada**, manifestando o aceite — em conformidade com a interpretação consolidada da ANPD sobre opt-in ativo.
3. O backend valida `consentGiven == true` e a presença de `consentVersion` antes de persistir o `User`.
4. Um registro imutável é gravado simultaneamente na tabela `consent_log` (vide Seção 4.7).

### 4.4.3 Consentimento no Contexto Multi-Tenant

Quando um administrador da `Company` cadastra um novo usuário no tenant, o sistema **não considera válido** o consentimento prestado por terceiro em nome do titular. Em vez disso:

1. A conta é criada em estado `firstLogin = true`, mas com `consentGiven = false`.
2. No primeiro login, o titular é obrigatoriamente conduzido à tela de aceite dos termos.
3. Apenas após o aceite individual e explícito, o `consentGiven` é gravado como `true` e a `consent_log` recebe um evento `GRANT` com o IP e o User-Agent do próprio titular.

Esse fluxo garante o requisito de manifestação **livre** previsto no Art. 5º, XII da LGPD.

### 4.4.4 Evidência Técnica

Caso o consentimento não seja fornecido após o primeiro login, o backend retorna `HTTP 403 Forbidden` para os endpoints protegidos, com a mensagem padronizada `"Aceite dos termos obrigatório para uso do serviço"`. Essa evidência pode ser demonstrada pelos testes automatizados em `AuthServiceConsentTest.java`.

---

## 4.5 Consentimento Associado à Finalidade

A LGPD, em seu Art. 8º, §4º, determina que o consentimento se refira a finalidades determinadas — sendo nulas as autorizações genéricas. Para atender a esse dispositivo, o Dumply-UC implementa **consentimentos granulares por finalidade**.

### 4.5.1 Finalidades Tratadas

| Finalidade | Identificador | Obrigatoriedade | Comentário |
|---|---|---|---|
| Autenticação e gestão da conta | `AUTHENTICATION` | Obrigatória | Indispensável à execução do contrato |
| Comunicações transacionais (e-mails de segurança) | `TRANSACTIONAL_EMAIL` | Obrigatória | Vinculada à execução do contrato |
| Comunicações de marketing / novidades | `MARKETING` | Opcional | Pode ser revogada a qualquer momento sem prejuízo do serviço |
| Notificações de fim de período de avaliação (`trialEndsAt`) | `TRIAL_NOTIFICATION` | Obrigatória | Vinculada à execução do contrato com a `Company` |

A coluna `finalidade` da tabela `consent_log` permite registrar e revogar manifestações independentes para cada uma dessas finalidades, atendendo ao princípio da granularidade exigido pela ANPD.

---

## 4.6 Possibilidade de Revogação do Consentimento

Em atenção ao Art. 8º, §5º da LGPD ("o consentimento pode ser revogado a qualquer momento mediante manifestação expressa do titular, por procedimento gratuito e facilitado"), o sistema expõe endpoint dedicado à revogação.

### 4.6.1 Endpoint

```
DELETE /lgpd/consent?finalidade=MARKETING
Authorization: Bearer <JWT>
```

### 4.6.2 Comportamento por Tipo de Finalidade

- **Finalidades opcionais** (ex.: `MARKETING`): o registro é marcado como revogado na `consent_log`, e a conta permanece ativa.
- **Finalidades obrigatórias** (`AUTHENTICATION`, `TRANSACTIONAL_EMAIL`): por incompatibilidade lógica com a continuidade do serviço, a revogação desencadeia automaticamente o fluxo de **exclusão da conta** do titular (Seção 4.10). O vínculo com a `Company` (`company_id`) é desfeito antes da anonimização.

### 4.6.3 Característica Append-Only

A revogação **não atualiza** o registro de consentimento anterior; ela insere um novo evento na `consent_log` com `action = "REVOKE"`, preservando integralmente o histórico para fins de auditoria.

---

## 4.7 Registro de Data e Versão do Consentimento

Para atender ao princípio da prestação de contas (Art. 6º, X da LGPD), o sistema mantém uma tabela imutável (append-only) de auditoria de consentimento.

### 4.7.1 Entidade `ConsentLog`

```java
@Entity
@Table(name = "consent_log")
public class ConsentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private String action;           // "GRANT" ou "REVOKE"
    private String finalidade;       // AUTHENTICATION | MARKETING | TRANSACTIONAL_EMAIL | TRIAL_NOTIFICATION
    private String version;          // ex.: "1.0"
    private LocalDateTime timestamp; // data/hora em UTC
    private String ipAddress;        // IP de origem da manifestação
    private String userAgent;        // navegador/dispositivo
}
```

### 4.7.2 Atributos de Rastreabilidade

- **Carimbo temporal** com precisão de segundos (`LocalDateTime` em UTC).
- **Versão da política** (`version`) aceita naquela manifestação específica.
- **Metadados técnicos** (`ipAddress`, `userAgent`) para fortalecer a evidência probatória, conforme orientação da ANPD sobre comprovação de consentimento.

### 4.7.3 Garantia de Imutabilidade

A tabela `consent_log` é configurada para aceitar exclusivamente operações `INSERT`. Não há método `update` ou `delete` no `ConsentLogRepository`, e a regra é reforçada por convenção arquitetural documentada nas ADRs (Architecture Decision Records) do projeto.

---

## 4.8 Funcionalidade de Consulta aos Dados do Titular

Atende ao direito de **confirmação de tratamento e acesso aos dados**, previsto no Art. 18, I e II da LGPD.

### 4.8.1 Endpoint

```
GET /lgpd/me/data
Authorization: Bearer <JWT>
```

### 4.8.2 Estrutura da Resposta

```json
{
  "id": "9c7d4a4e-1f3a-4c7e-8d92-7f3b93ab8120",
  "email": "usuario@exemplo.com",
  "fullName": "Maria da Silva",
  "document": "***.***.***-**",
  "role": "USER",
  "is2faEnabled": true,
  "consent": {
    "given": true,
    "version": "1.0",
    "givenAt": "2026-05-01T14:30:00Z",
    "finalidades": ["AUTHENTICATION", "TRANSACTIONAL_EMAIL"]
  },
  "createdAt": "2026-05-01T14:30:00Z",
  "company": {
    "name": "Empresa Exemplo Ltda.",
    "slug": "empresa-exemplo",
    "status": "ACTIVE"
  }
}
```

### 4.8.3 Controles de Privacidade Aplicados

- Os campos `password`, `secret2fa`, `passwordResetToken` e `disable2faCode` **jamais** são retornados.
- O campo `document` é exibido com mascaramento parcial, em conformidade com o princípio da necessidade (Art. 6º, III).
- Os dados da `Company` são incluídos em forma reduzida, apenas para que o titular tenha conhecimento do contexto organizacional em que seus dados são tratados.
- O endpoint exige token JWT válido e está submetido ao filtro `SecurityFilter`, garantindo que o titular acesse exclusivamente seus próprios dados, respeitando o isolamento multi-tenant (`TenantContext`).

---

## 4.9 Funcionalidade de Exportação dos Dados

Atende ao direito de **portabilidade** previsto no Art. 18, V da LGPD, permitindo que o titular leve seus dados a outro fornecedor de serviço em formato interoperável.

### 4.9.1 Endpoint

```
GET /lgpd/me/export
Authorization: Bearer <JWT>
Accept: application/json
```

### 4.9.2 Características Técnicas

- **Formato:** JSON estruturado, padrão aberto e interoperável (leitura por máquina e ser humano).
- **Conteúdo:** todos os dados pessoais do titular, dados contextuais da `Company` à qual está vinculado e o histórico completo da `consent_log`.
- **Cabeçalho HTTP:** `Content-Disposition: attachment; filename="dumply_dados_<userId>.json"` força o download.
- **Versionamento de schema:** o arquivo carrega a chave `schema_version` para garantir compatibilidade futura.

### 4.9.3 Estrutura Exportada

```json
{
  "schema_version": "1.0",
  "exported_at": "2026-05-11T10:00:00Z",
  "subject": {
    "id": "...",
    "email": "...",
    "fullName": "...",
    "document": "...",
    "role": "USER",
    "createdAt": "..."
  },
  "company_context": {
    "id": "...",
    "name": "...",
    "slug": "...",
    "status": "ACTIVE"
  },
  "consent_history": [
    {
      "action": "GRANT",
      "finalidade": "AUTHENTICATION",
      "version": "1.0",
      "timestamp": "2026-05-01T14:30:00Z",
      "ipAddress": "192.0.2.10",
      "userAgent": "Mozilla/5.0 ..."
    }
  ],
  "security_events": {
    "lastLogin": "2026-05-10T09:12:00Z",
    "failedAttempts": 0,
    "is2faEnabled": true
  }
}
```

---

## 4.10 Funcionalidade de Exclusão dos Dados Pessoais

Atende ao direito de **eliminação** previsto no Art. 18, VI da LGPD, possibilitando a remoção dos dados pessoais tratados com base no consentimento, com exceção das hipóteses do Art. 16.

### 4.10.1 Endpoint

```
DELETE /lgpd/me
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "password": "<senha-atual>",
  "confirmation": "EXCLUIR"
}
```

### 4.10.2 Estratégia de Exclusão (Anonimização Estruturada)

1. **Confirmação dupla:** o titular deve fornecer sua senha atual e a string literal `"EXCLUIR"`, evitando exclusões acidentais.
2. **Anonimização dos campos identificáveis na entidade `User`:**
   - `email` → `anonimizado_<uuid>@deletado.local`
   - `fullName` → `"Usuário Anonimizado"`
   - `document` → `null`
   - `password`, `secret2fa`, `passwordResetToken`, `disable2faCode` → `null`
3. **Preservação do vínculo `company_id`:** o relacionamento com a `Company` é mantido (não removido), pois sua exclusão poderia afetar a integridade contábil e operacional do tenant. Como os atributos pessoais foram anonimizados, o registro deixa de ser dado pessoal.
4. **Revogação imediata de sessões ativas:** todos os JWTs válidos do usuário são adicionados à blacklist no Redis por meio do `TokenBlacklistService`.
5. **Preservação seletiva do histórico:** os registros da `consent_log` referentes ao titular são mantidos por **5 anos**, em consonância com o Art. 16, II da LGPD (prestação de contas) e com o prazo prescricional do CDC (Lei nº 8.078/1990).
6. **Resposta:** `HTTP 200 OK` com a confirmação `"Conta excluída com sucesso"`.

### 4.10.3 Exclusão de Empresa (Tenant)

Quando uma `Company` é encerrada (status `TERMINATED`), o sistema executa exclusão em cascata controlada:

1. Todos os `User` vinculados àquele tenant são anonimizados conforme a Seção 4.10.2.
2. Os atributos `name` e `slug` da `Company` são substituídos por valores anonimizados (`"Empresa Encerrada"` e `"encerrada-<uuid>"`).
3. Os campos `status`, `trialEndsAt` e `createdAt` são preservados para fins de auditoria contratual.

### 4.10.4 Característica Irreversível

A operação é **irrevogável**, sem janela de carência (soft-delete). Essa escolha reforça o cumprimento imediato do direito do titular, em alinhamento com a Resolução CD/ANPD nº 4/2023.

---

## 4.11 Fluxo de Atendimento aos Direitos do Titular

Atende integralmente ao Art. 18 da LGPD, que enumera os direitos do titular, e ao Art. 19, §1º, que estabelece o prazo de 15 dias para resposta a determinadas solicitações.

### 4.11.1 Canais Oficiais de Comunicação

| Canal | Endereço | Prazo de Resposta |
|---|---|---|
| Encarregado pelo Tratamento (DPO) | `dpo@dumply.local` | Até **15 dias úteis** (Art. 19, §1º) |
| Portal de Privacidade (in-app) | Menu *Configurações → Privacidade* | Imediato (autosserviço) |
| Suporte ao Usuário | `suporte@dumply.local` | Até 5 dias úteis |

### 4.11.2 Fluxograma do Atendimento

```
┌──────────────────────────────┐
│ Titular formula solicitação  │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────────┐
│ É um direito atendível em        │
│ autosserviço? (consulta,         │
│ exportação, revogação, exclusão) │
└──────────┬───────────────┬───────┘
        Sim│              │Não
           ▼              ▼
┌──────────────────┐ ┌────────────────────────────┐
│ App processa via │ │ Solicitação encaminhada    │
│ endpoint /lgpd/* │ │ ao DPO (dpo@dumply.local)  │
└────────┬─────────┘ └──────────────┬─────────────┘
         │                          │
         ▼                          ▼
┌──────────────────┐ ┌────────────────────────────┐
│ Resposta imediata│ │ DPO analisa em até         │
│ (síncrona)       │ │ 15 dias úteis              │
└──────────────────┘ └──────────────┬─────────────┘
                                    │
                                    ▼
                     ┌────────────────────────────┐
                     │ Resposta formal por e-mail │
                     │ + registro em planilha de  │
                     │ auditoria de solicitações  │
                     └────────────────────────────┘
```

### 4.11.3 Mapeamento Direito → Mecanismo

| Direito (Art. 18 LGPD) | Mecanismo de Exercício |
|---|---|
| I — Confirmação de tratamento | `GET /lgpd/me/data` |
| II — Acesso aos dados | `GET /lgpd/me/data` |
| III — Correção de dados incompletos | `PATCH /auth/me` |
| IV — Anonimização, bloqueio ou eliminação de dados desnecessários | Solicitação ao DPO |
| V — Portabilidade dos dados | `GET /lgpd/me/export` |
| VI — Eliminação dos dados tratados com base em consentimento | `DELETE /lgpd/me` |
| VII — Informação sobre compartilhamento | Seção 5 da Política de Privacidade (não há compartilhamento com terceiros) |
| VIII — Informação sobre não consentir | Política de Privacidade |
| IX — Revogação do consentimento | `DELETE /lgpd/consent` |

### 4.11.4 Direitos Específicos no Contexto Multi-Tenant

Quando um titular exerce o direito de eliminação enquanto sua `Company` permanece ativa, o sistema:

1. Notifica via e-mail o administrador da `Company` sobre a saída do usuário (sem expor o motivo).
2. Garante que o histórico contratual da `Company` (status, datas, métricas agregadas) permaneça íntegro.
3. Registra o evento na `consent_log` como `REVOKE` para todas as finalidades vinculadas ao usuário.

### 4.11.5 Registro e Auditoria das Solicitações

As solicitações recebidas manualmente pelo DPO são registradas em planilha de controle interno, com os seguintes campos: identificação mascarada do titular, direito invocado, data de recebimento, data de resposta, resultado (atendido, parcialmente atendido ou negado com justificativa) e referência ao protocolo. Essa planilha constitui evidência adicional para o princípio da prestação de contas (Art. 6º, X).

---

## 5. Justificativas Técnicas das Escolhas

| Decisão | Justificativa Técnica | Princípio LGPD |
|---|---|---|
| Tabela `consent_log` append-only | Garante prestação de contas e evidência probatória da manifestação de vontade ao longo do tempo | Art. 6º, X |
| Anonimização em vez de hard-delete total | Preserva integridade referencial com a `Company` e mantém apenas os campos necessários para demonstração histórica de conformidade | Art. 16, II |
| Mascaramento de `document` nas respostas | Reduz exposição desnecessária do CPF/CNPJ | Art. 6º, III (necessidade) |
| Hash BCrypt e 2FA TOTP | Protege credenciais mesmo em cenário de comprometimento da base | Art. 6º, VII (segurança) |
| TLS/HTTPS obrigatório (ver `SECURITY_TRAFFIC.md`) | Confidencialidade dos dados pessoais em trânsito | Art. 46 |
| Tokens JWT revogáveis via Redis | Permite invalidação imediata de sessões em caso de exclusão ou incidente | Art. 46 e 47 |
| Granularidade de consentimentos por finalidade | Evita autorizações genéricas, vedadas pelo Art. 8º, §4º | Art. 8º, §4º |
| Isolamento multi-tenant (`TenantContext` + `company_id` em todas as queries) | Evita vazamento cruzado entre empresas distintas | Art. 6º, VII (segurança) |
| Preservação do vínculo `company_id` após anonimização | Mantém a integridade contábil-operacional do tenant sem comprometer a privacidade do titular já anonimizado | Art. 16, II |

---

## 6. Versionamento da Política de Privacidade

| Versão | Data | Alterações |
|---|---|---|
| 1.0 | 2026-05-11 | Versão inicial — define dados coletados, finalidades, bases legais, direitos do titular e fluxos de atendimento |
| 1.1 | 2026-05-11 | Inclusão dos dados da entidade `Company` no inventário e tratamento multi-tenant; consentimento de primeiro login para usuários cadastrados por administradores |

Alterações materiais na Política de Privacidade exigem **novo aceite** do titular (incremento de `consentVersion`). Até que o novo termo seja aceito, o acesso autenticado às funcionalidades é bloqueado, em consonância com a recomendação da ANPD sobre re-coleta de consentimento.

