### BUG REPORT

*   **Título:** `[InvoiceDetail] API retornando erro 500 ao inves de acesso negado`.
*   **Severidade/Prioridade:** Minor
*   **Passos para Reprodução:**
    1.  Fazer login no sistema com a empresa A.
    2.  Gerar uma fatura.
    3.  Deslogar e logar na empresa B.
    4.  Acessar a fatura gerada na empresa A via URL (invoices/**).
*   **Comportamento Atual:** `O sistema retornou erro 500`.
*   **Comportamento Esperado:** `Deveria retornar 403 e exibir a mensagem "ACESSO NEGADO"`.
*   **Evidências:** Print da tela (mostrando o erro) ou vídeo rápido.
*   **Ambiente:** `Navegador Chrome v120 / Sistema macOS/ Ambiente de Homologação`.

---