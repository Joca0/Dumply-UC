# Criptografia de Tráfego (TLS/HTTPS)

Este documento descreve a implementação da criptografia de tráfego no projeto Dumply, atendendo aos requisitos de segurança 3.1, 3.2 e 3.3.

## 1. Estratégia de Implementação

A estratégia adotada para garantir que todo o tráfego entre o cliente e o servidor seja cifrado baseia-se no uso de um **Proxy Reverso** (Nginx) que realiza a terminação TLS.

### Componentes:
- **Nginx (Frontend Container):** Atua como o ponto de entrada único para o tráfego. Ele escuta na porta 443 (HTTPS) e redireciona todo o tráfego da porta 80 (HTTP) para a 443.
- **Certificados SSL/TLS:** No ambiente de produção, recomenda-se o uso de certificados emitidos por uma CA (Certificate Authority) confiável, como Let's Encrypt.
- **Spring Boot (Backend):** Configurado para exigir conexões seguras e validar o estado do tráfego.

## 2. Configurações Realizadas

### 2.1 Ambiente de Desenvolvimento (Local)
Para garantir a conformidade com os requisitos de tráfego cifrado mesmo durante o desenvolvimento, o ambiente local foi configurado para utilizar HTTPS:
1.  **Vite HTTPS:** O servidor de desenvolvimento do Vite em `frontend/vite.config.js` está configurado para usar os certificados da pasta `certs`.
2.  **Spring Boot SSL:** O backend no perfil `dev` utiliza um arquivo `keystore.p12` (gerado a partir dos mesmos certificados) para servir a API na porta `8443` via HTTPS.
3.  **Vite Proxy:** Redireciona chamadas `/api/*` para `https://localhost:8443/*`, com `secure: false` para aceitar o certificado auto-assinado.

### 2.2 Ambiente de Produção (Docker/Nginx)
O servidor Nginx foi configurado para:
1.  Escutar na porta 443 com suporte a SSL.
2.  Utilizar protocolos seguros (TLSv1.2 e TLSv1.3).
3.  Redirecionar automaticamente requisições HTTP para HTTPS.
4.  Realizar o proxy das chamadas de API para o container de backend.

### 2.3 Docker Compose (docker-compose.yml)
- Mapeamento da porta **443:443** no serviço de frontend.
- Volume montado em `/etc/nginx/certs` para persistência dos certificados.

### 2.4 Backend (SecurityConfig.java)
- Implementação de verificação condicional via propriedade `security.require-ssl`. Quando ativada (em produção), o Spring Security exige HTTPS em todas as rotas através de `requiresChannel`.

## 3. Como Gerar Certificados para Teste (Auto-assinados)

Para testes em ambiente de desenvolvimento ou homologação local, você pode gerar certificados auto-assinados com o seguinte comando OpenSSL:

```bash
mkdir -p certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout certs/nginx-selfsigned.key \
  -out certs/nginx-selfsigned.crt \
  -subj "/C=BR/ST=SP/L=SaoPaulo/O=Dumply/CN=localhost"
```

Os arquivos gerados devem ser colocados na pasta `certs` na raiz do projeto.

### 3.1 Gerar Keystore para o Spring Boot
Para que o Java reconheça os certificados, é necessário criar um arquivo PKCS12:
```bash
openssl pkcs12 -export -in certs/nginx-selfsigned.crt -inkey certs/nginx-selfsigned.key -out src/main/resources/keystore.p12 -name dumply -password pass:password
```

## 4. Evidência de Tráfego Cifrado
Com esta configuração ativa:
1.  O navegador exibirá o ícone de cadeado ao lado da URL.
2.  Na aba **Network** das ferramentas de desenvolvedor (F12), o protocolo utilizado será `h2` ou `http/1.1` sobre TLS.
3.  Tentativas de acesso via `http://` serão automaticamente migradas para `https://`.

## 5. Justificativa Técnica
O uso de TLS/HTTPS é fundamental para proteger dados sensíveis em trânsito (como credenciais de login e tokens JWT) contra ataques de *Man-in-the-Middle* (MitM). A escolha do Nginx para terminação TLS é uma prática de mercado que reduz a carga de processamento do servidor de aplicação (Spring Boot) e centraliza a gestão de certificados.
