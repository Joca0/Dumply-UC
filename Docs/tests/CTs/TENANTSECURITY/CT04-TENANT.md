**CT04-TENANT:**
*   **ID/Título:** Um nome curto e descritivo. Ex: `CT-04 - Segurança do Tenant`.
*   **Pré-condições:** O que o sistema precisa ter antes de começar. Ex: `2 Empresas diferentes cadastradas`.
*   **Passos:** Ações sequenciais.
    1.  Fazer Login na Empresa A.
    2.  Acessar a página de listagem de faturas.
    3. Cadastrar uma nova fatura para a Empresa A.
    4. Deslogar.
    5. Fazer Login na Empresa B.
    6. Tentar acessar a fatura cadastrada na etapa 3 via URL (/invoices/***).
*   **Resultado Esperado:** `Mensagem de ACESSO NEGADO`.

---