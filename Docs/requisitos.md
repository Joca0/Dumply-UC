# Requisitos

## 1. Autenticação e Gestão de Credenciais

- 1.1 Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2)
- 1.2 Parâmetros de custo do hash configurados e justificados
- 1.3 Uso de salt criptográfico único por usuário
- 1.4 Armazenamento correto do hash + salt
- 1.5 Autenticação de dois fatores (2FA) implementada
- 1.6 Validação do 2FA após autenticação primária
- 1.7 Fluxo de autenticação documentado
- 1.8 Evidências funcionais (prints, logs ou testes)
- 1.9 Sessões com tempo de expiração
- 1.10 Invalidação de sessão no logout
- 1.11 Proteção contra força bruta (rate limit, bloqueio, atraso)
- 1.12 Justificativas técnicas documentadas

---

## 2. Recuperação de Senha

- 2.1 Funcionalidade de recuperação de senha implementada
- 2.2 Token criptograficamente seguro
- 2.3 Token com tempo de expiração
- 2.4 Token invalidado após uso
- 2.5 Falha correta para token expirado
- 2.6 Registro de solicitação de recuperação em log
- 2.7 Registro de sucesso/falha do processo

---

## 3. Criptografia e Comunicação Segura

- 3.1 Comunicação protegida por TLS/HTTPS
- 3.2 Bloqueio de conexões não seguras
- 3.3 Evidência de tráfego cifrado
- 3.4 Dados sensíveis criptografados em repouso
- 3.5 Uso de algoritmo criptográfico adequado (ex.: AES)
- 3.6 Chaves criptográficas protegidas
- 3.7 Estratégia de criptografia documentada
- 3.8 Justificativa técnica das escolhas

---

## 4. Conformidade com a LGPD

- 4.1 Listagem completa dos dados pessoais coletados
- 4.2 Associação de cada dado a uma finalidade
- 4.3 Evidência de minimização de dados
- 4.4 Registro explícito de consentimento
- 4.5 Consentimento associado à finalidade
- 4.6 Possibilidade de revogação do consentimento
- 4.7 Registro de data e versão do consentimento
- 4.8 Funcionalidade de consulta aos dados do titular
- 4.9 Funcionalidade de exportação dos dados
- 4.10 Funcionalidade de exclusão dos dados pessoais
- 4.11 Fluxo de atendimento aos direitos documentado

---

## 5. Auditoria e Logs

- 5.1 Logs de autenticação registrados
- 5.2 Logs de falhas e 2FA registrados
- 5.3 Proteção contra alteração dos logs
- 5.4 Exemplo de análise de logs apresentado

---

## 6. Documentação Técnico-Científica

- 6.1 Documento de visão geral do sistema
- 6.2 Diagrama de arquitetura
- 6.3 Fluxos de autenticação e dados documentados
- 6.4 Gestão de credenciais documentada
- 6.5 Uso de criptografia documentado
- 6.6 Identificação dos ativos do sistema
- 6.7 Identificação de ameaças e vulnerabilidades
- 6.8 Associação risco × contramedida
- 6.9 Testes de segurança realizados
- 6.10 Resultados dos testes documentados
- 6.11 Uso de artigos científicos e/ou normas técnicas
- 6.12 Referências normalizadas

---

## 7. Resumo Científico

- 7.1 Resumo entre 200 e 300 palavras
- 7.2 Objetivo claramente definido
- 7.3 Metodologia técnica descrita
- 7.4 Mecanismos de segurança apresentados
- 7.5 Conformidade com a LGPD explicitada
- 7.6 Terminologia técnica adequada
- 7.7 Qualidade textual e científica

---

## 8. Pôster Científico e Apresentação

- 8.1 Estrutura científica do pôster
- 8.2 Arquitetura e fluxos representados visualmente
- 8.3 Evidência de conformidade com LGPD
- 8.4 Qualidade técnica dos diagramas
- 8.5 Coerência com o sistema entregue
- 8.6 Domínio técnico na apresentação
- 8.7 Capacidade de resposta às perguntas