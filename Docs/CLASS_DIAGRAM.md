# Diagrama de Classes - Dumply

Este documento apresenta o diagrama de classes das principais entidades do projeto Dumply, detalhando seus atributos e relacionamentos.

```mermaid
classDiagram
    class Company {
        +UUID id
        +String name
        +String slug
        +CompanyStatus status
        +LocalDateTime trialEndsAt
        +LocalDateTime createdAt
    }

    class CompanySuperEntity {
        <<abstract>>
        -UUID companyId
    }

    class User {
        +UUID id
        +String email
        +String fullName
        +boolean firstLogin
        +String document
        +String password
        +Role role
        +Company company
    }

    class Customer {
        +Long id
        +String companyName
        +String fullName
        +String document
        +String email
        +String phone
        +Company company
    }

    class Equipment {
        +Long id
        +String name
        +String serialNumber
        +String category
        +EquipmentStatus status
        +Company company
    }

    class Rental {
        +Long id
        +LocalDateTime startDate
        +LocalDateTime endDate
        +RentalStatus status
        +InvoiceStatus invoiceStatus
        +String fullAddress
        +double latitude
        +double longitude
        +User driver
        +Company company
        +Equipment equipment
        +Customer customer
        +BigDecimal charge
        +Invoice invoice
    }

    class Invoice {
        +Long id
        +Customer customer
        +List~Rental~ items
        +LocalDateTime createdAt
        +BigDecimal totalAmount
        +InvoiceStatus status
        +Company company
    }

    %% Relacionamentos
    Customer --|> CompanySuperEntity
    Equipment --|> CompanySuperEntity
    Rental --|> CompanySuperEntity
    Invoice --|> CompanySuperEntity

    User "*" --> "1" Company : vincula-se a
    Customer "*" --> "1" Company : pertence a
    Equipment "*" --> "1" Company : pertence a
    Rental "*" --> "1" Company : gerenciado por
    Invoice "*" --> "1" Company : faturado por

    Rental "*" --> "0..1" User : motorista (driver)
    Rental "*" --> "0..1" Equipment : utiliza
    Rental "*" --> "1" Customer : locatário
    Rental "*" --> "0..1" Invoice : vinculada a

    Invoice "*" --> "1" Customer : para cliente
    Invoice "1" --> "*" Rental : contém itens (items)
```

## Descrição das Entidades

- **Company**: Representa o inquilino (tenant) do sistema.
- **User**: Usuários que acessam o sistema, podendo ser ADMIN, OWNER, MANAGER ou DRIVER.
- **Customer**: Clientes que realizam os aluguéis.
- **Equipment**: Equipamentos (caçambas/máquinas) disponíveis para aluguel.
- **Rental**: Representa a transação de aluguel entre a empresa e o cliente.
- **Invoice**: Agrupamento de aluguéis para faturamento ao cliente.
- **CompanySuperEntity**: Classe base que fornece a infraestrutura de multi-tenancy para as entidades que dependem de uma empresa.
