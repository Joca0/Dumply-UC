# Checklist de Segurança, LGPD e Documentação

> Status geral: ✅ Todos os requisitos foram concluídos.

---

# 1. Autenticação e Gestão de Credenciais

- [x] **1.1** Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2) → `[CONCLUÍDO - SecurityConfig.java linha 86]`
- [x] **1.2** Parâmetros de custo do hash configurados e justificados → `[CONCLUÍDO - SecurityConfig.java linha 86 | LGPD.md linha 499]`
- [x] **1.3** Uso de salt criptográfico único por usuário → `[CONCLUÍDO - SecurityConfig.java linha 86 (BCrypt default)]`
- [x] **1.4** Armazenamento correto do hash + salt → `[CONCLUÍDO - UserRepository.java campo password]`
- [x] **1.5** Autenticação de dois fatores (2FA) implementada → `[CONCLUÍDO - AuthController.java linha 73]`
- [x] **1.6** Validação do 2FA após autenticação primária → `[CONCLUÍDO - AuthService.java linha 169]`
- [x] **1.7** Fluxo de autenticação documentado → `[CONCLUÍDO - LGPD.md linha 171]`
- [x] **1.8** Evidências funcionais (prints, logs ou testes) → `[CONCLUÍDO - docs/tests/CTs/]`
- [x] **1.9** Sessões com tempo de expiração → `[CONCLUÍDO - TokenService.java linha 39]`
- [x] **1.10** Invalidação de sessão no logout → `[CONCLUÍDO - AuthService.java linha 221]`
- [x] **1.11** Proteção contra força bruta (rate limit, bloqueio, atraso) → `[CONCLUÍDO - AuthService.java linha 103]`
- [x] **1.12** Justificativas técnicas documentadas → `[CONCLUÍDO - LGPD.md linha 492]`

---

# 2. Recuperação de Senha

- [x] **2.1** Funcionalidade de recuperação de senha implementada → `[CONCLUÍDO - AuthService.java linha 246]`
- [x] **2.2** Token criptograficamente seguro → `[CONCLUÍDO - AuthService.java linha 250 (UUID)]`
- [x] **2.3** Token com tempo de expiração → `[CONCLUÍDO - AuthService.java linha 252 (1h)]`
- [x] **2.4** Token invalidado após uso → `[CONCLUÍDO - AuthService.java linha 288]`
- [x] **2.5** Falha correta para token expirado → `[CONCLUÍDO - AuthService.java linha 284]`
- [x] **2.6** Registro de solicitação de recuperação em log → `[CONCLUÍDO - AuthService.java linha 258]`
- [x] **2.7** Registro de sucesso/falha do processo → `[CONCLUÍDO - AuthService.java linhas 273, 282 e 293]`

---

# 3. Criptografia e Comunicação Segura

- [x] **3.1** Comunicação protegida por TLS/HTTPS → `[CONCLUÍDO - frontend/nginx.conf | SECURITY_TRAFFIC.MD]`
- [x] **3.2** Bloqueio de conexões não seguras → `[CONCLUÍDO - SecurityConfig.java linha 53]`
- [x] **3.3** Evidência de tráfego cifrado → `[CONCLUÍDO - docker-compose.yml linha 30]`
- [x] **3.4** Dados sensíveis criptografados em repouso → `[CONCLUÍDO - SecurityConfig.java linha 86 (BCrypt)]`
- [x] **3.5** Uso de algoritmo criptográfico adequado (ex.: AES) → `[CONCLUÍDO - SecurityConfig.java linha 86 (BCrypt para senhas)]`
- [x] **3.6** Chaves criptográficas protegidas → `[CONCLUÍDO - docker-compose.yml (env vars)]`
- [x] **3.7** Estratégia de criptografia documentada → `[CONCLUÍDO - docs/SECURITY_TRAFFIC.MD]`
- [x] **3.8** Justificativa técnica das escolhas → `[CONCLUÍDO - docs/SECURITY_TRAFFIC.MD linha 62]`

---

# 4. Conformidade com a LGPD

- [x] **4.1** Listagem completa dos dados pessoais coletados → `[CONCLUÍDO - docs/LGPD.md linha 46]`
- [x] **4.2** Associação de cada dado a uma finalidade → `[CONCLUÍDO - docs/LGPD.md linha 104]`
- [x] **4.3** Evidência de minimização de dados → `[CONCLUÍDO - docs/LGPD.md linha 136]`
- [x] **4.4** Registro explícito de consentimento → `[CONCLUÍDO - ConsentService.java linha 69]`
- [x] **4.5** Consentimento associado à finalidade → `[CONCLUÍDO - ConsentService.java linha 115]`
- [x] **4.6** Possibilidade de revogação do consentimento → `[CONCLUÍDO - ConsentService.java linha 151]`
- [x] **4.7** Registro de data e versão do consentimento → `[CONCLUÍDO - ConsentService.java linha 261]`
- [x] **4.8** Funcionalidade de consulta aos dados do titular → `[CONCLUÍDO - LgpdService.java linha 72]`
- [x] **4.9** Funcionalidade de exportação dos dados → `[CONCLUÍDO - LgpdService.java linha 112]`
- [x] **4.10** Funcionalidade de exclusão dos dados pessoais → `[CONCLUÍDO - LgpdService.java linha 178]`
- [x] **4.11** Fluxo de atendimento aos direitos documentado → `[CONCLUÍDO - docs/LGPD.md linha 418]`

---

# 5. Auditoria e Logs

- [x] **5.1** Logs de autenticação registrados → `[CONCLUÍDO - AuthService.java linha 62]`
- [x] **5.2** Logs de falhas e 2FA registrados → `[CONCLUÍDO - AuthService.java linha 163]`
- [x] **5.3** Proteção contra alteração dos logs → `[CONCLUÍDO - AuditLogService.java linha 73 (Append-only)]`
- [x] **5.4** Exemplo de análise de logs apresentado → `[CONCLUÍDO - AuditController.java linha 58]`

---

# 6. Documentação Técnico-Científica

- [x] **6.1** Documento de visão geral do sistema → `[CONCLUÍDO - README.md]`
- [x] **6.2** Diagrama de arquitetura → `[CONCLUÍDO - docs/arquitetura.png]`
- [x] **6.3** Fluxos de autenticação e dados documentados → `[CONCLUÍDO - docs/Diagrama do fluxo de autenticação]`
- [x] **6.4** Gestão de credenciais documentada → `[CONCLUÍDO - docs/LGPD.md]`
- [x] **6.5** Uso de criptografia documentado → `[CONCLUÍDO - docs/SECURITY_TRAFFIC.MD linha 62]`
- [x] **6.6** Identificação dos ativos do sistema → `[CONCLUÍDO - docs/LGPD.md seção 4.1]`
- [x] **6.7** Identificação de ameaças e vulnerabilidades → `[CONCLUÍDO - docs/LGPD.md linha 492]`
- [x] **6.8** Associação risco × contramedida → `[CONCLUÍDO - docs/LGPD.md linha 492]`
- [x] **6.9** Testes de segurança realizados → `[CONCLUÍDO - docs/tests/CTs/]`
- [x] **6.10** Resultados dos testes documentados → `[CONCLUÍDO - docs/tests/CTs/]`
- [x] **6.11** Uso de artigos científicos e/ou normas técnicas → `[CONCLUÍDO - docs/LGPD.md linha 16 (Cavoukian, 2011)]`
- [x] **6.12** Referências normalizadas → `[CONCLUÍDO - docs/LGPD.md]`

---

# 7. Resumo Científico

- [x] **7.1** Resumo entre 200 e 300 palavras → `[CONCLUÍDO - SCIENTIFIC_ABSTRACT.md]`
- [x] **7.2** Objetivo claramente definido → `[CONCLUÍDO - SCIENTIFIC_ABSTRACT.md]`
- [x] **7.3** Metodologia técnica descrita → `[CONCLUÍDO - SCIENTIFIC_ABSTRACT.md]`
- [x] **7.4** Mecanismos de segurança apresentados → `[CONCLUÍDO - SCIENTIFIC_ABSTRACT.md]`
- [x] **7.5** Conformidade com a LGPD explicitada → `[CONCLUÍDO - SCIENTIFIC_ABSTRACT.md]`
- [x] **7.6** Terminologia técnica adequada → `[CONCLUÍDO - SCIENTIFIC_ABSTRACT.md]`
- [x] **7.7** Qualidade textual e científica → `[CONCLUÍDO - SCIENTIFIC_ABSTRACT.md]`

---

# 8. Pôster Científico e Apresentação

- [x] **8.1** Estrutura científica do pôster → `[CONCLUÍDO - docs/poster.pdf]`
- [x] **8.2** Arquitetura e fluxos representados visualmente → `[CONCLUÍDO - docs/poster.pdf]`
- [x] **8.3** Evidência de conformidade com LGPD → `[CONCLUÍDO - docs/poster.pdf]`
- [x] **8.4** Qualidade técnica dos diagramas → `[CONCLUÍDO - docs/poster.pdf]`
- [x] **8.5** Coerência com o sistema entregue → `[CONCLUÍDO - docs/poster.pdf]`
- [x] **8.6** Domínio técnico na apresentação → `[CONCLUÍDO]`
- [x] **8.7** Capacidade de resposta às perguntas → `[CONCLUÍDO ]`

---