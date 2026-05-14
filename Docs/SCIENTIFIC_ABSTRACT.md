## Resumo Científico

O projeto Dumply propõe o desenvolvimento de uma plataforma robusta para a gestão de ativos e locações em arquiteturas multi-tenant, fundamentada nos paradigmas de Privacy by Design e Security by Design. O objetivo central é garantir a integridade, disponibilidade e confidencialidade dos dados, assegurando simultaneamente o cumprimento rigoroso da Lei Geral de Proteção de Dados (LGPD). A metodologia técnica aplicada compreende a construção de uma API REST escalável com Spring Boot, utilizando Spring Security para o controle de acesso e Nginx para terminação TLS, assegurando a cifragem de ponta a ponta. 

Os mecanismos de segurança implementados incluem autenticação multifator (2FA) via protocolo TOTP, armazenamento de credenciais utilizando funções de hash criptográfico de alta resistência (BCrypt com salt único) e proteção contra ataques de força bruta. A conformidade com a LGPD é materializada através de funcionalidades de autosserviço para os titulares, permitindo a consulta, exportação em formato interoperável (JSON) e a anonimização estruturada de dados pessoais, além da manutenção de um registro imutável (append-only) de consentimentos granulares. 

Os resultados demonstram que a integração nativa de controles de privacidade desde a concepção do sistema reduz a superfície de ataque e mitiga riscos de vazamento de dados. A solução se mostra eficaz em cenários corporativos que demandam isolamento lógico entre entidades (tenants) e transparência no tratamento de dados. Em conclusão, o Dumply valida a viabilidade técnica de sistemas que equilibram alta produtividade operacional com proteção de direitos fundamentais, estabelecendo um padrão para o desenvolvimento de aplicações seguras e em conformidade legal.

**Palavras-chave:** Segurança da Informação, LGPD, Privacy by Design, Autenticação Multifator, Gestão de Ativos.

---
