### BUG REPORT

*   **Título:** `[LoginForm] Feedback visual ausente ao atingir limite de tentativas de login (Conta Bloqueada)`.
*   **Severidade/Prioridade:** Major
*   **Passos para Reprodução:**
    1.  Navegar até a URL `/auth/login`.
    2.  Digitar um e-mail válido e senha inválida.
    3.  Clicar em "Entrar" 5 vezes.
    4.  Inserir senha correta.
    5.  Clicar em "Entrar".
*   **Comportamento Atual:** `O sistema exibe nada`.
*   **Comportamento Esperado:** `Deveria exibir a mensagem "Conta Bloqueada"`.
*   **Evidências:** Print da tela (mostrando o erro) ou vídeo rápido.
*   **Ambiente:** `Navegador Chrome v120 / Sistema macOS/ Ambiente de Homologação`.

---