# Plano de Implementação: Banco de Dados Movidesk (MySQL)

Este documento detalha o mapeamento dos campos do Movidesk para uma estrutura de banco de dados relacional em **MySQL**, facilitando a criação de dashboards e consultas de performance. 

> [!NOTE]
> Todas as tabelas do Movidesk utilizam o prefixo `movidesk_` para se diferenciarem das tabelas do sistema Benner (prefixo `dw_`).

## 1. Mapeamento de Campos Personalizados (Custom Fields)

Com base no ticket de exemplo `1050636`, os campos personalizados foram identificados e nomeados conforme os padrões oficiais:

| ID | Nome Oficial | Descrição / Uso |
| :--- | :--- | :--- |
| **205913** | Feiras Nacionais | Identifica a feira específica (ex: Agrishow). |
| **206220** | Tipo de Atendimento AD Feiras | Categoria detalhada do atendimento (ex: Participante). |
| **208535** | Unidade | Unidade de negócio responsável (ex: AD FEIRAS - INFORMA). |
| **234900** | País/Estado | Localização geográfica do solicitante (ex: COLOMBIA). |
| **237387** | Núcleo | Segmento ou núcleo de mercado (ex: Agro). |

---

## 2. Estrutura do Banco de Dados (Script MySQL)

Você pode copiar e colar este script diretamente no seu MySQL Workbench ou terminal.

```sql
-- Tabela de Pessoas (Agentes e Clientes)
CREATE TABLE movidesk_people (
    id VARCHAR(255) PRIMARY KEY,
    person_type INT, -- 1: Pessoa, 2: Empresa, 4: Departamento
    profile_type INT, -- 1: Agente, 2: Cliente, 3: Ambos
    business_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabela de Tickets
CREATE TABLE movidesk_tickets (
    id INT PRIMARY KEY,
    subject VARCHAR(255),
    category VARCHAR(100),
    status VARCHAR(100),
    base_status VARCHAR(100),
    owner_id VARCHAR(255),
    service_first_level VARCHAR(255),
    service_second_level VARCHAR(255),
    service_third_level VARCHAR(255),
    created_date DATETIME,
    resolved_in DATETIME,
    action_count INT,
    life_time_working_time INT,
    CONSTRAINT fk_ticket_owner FOREIGN KEY (owner_id) REFERENCES movidesk_people(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Definição de Campos Personalizados
CREATE TABLE movidesk_custom_field_definitions (
    id INT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Valores de Campos Personalizados por Ticket
CREATE TABLE movidesk_custom_field_values (
    ticket_id INT,
    custom_field_id INT,
    val_text TEXT, 
    PRIMARY KEY (ticket_id, custom_field_id),
    CONSTRAINT fk_val_ticket FOREIGN KEY (ticket_id) REFERENCES movidesk_tickets(id),
    CONSTRAINT fk_val_def FOREIGN KEY (custom_field_id) REFERENCES movidesk_custom_field_definitions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Histórico de Ações (Interações)
CREATE TABLE movidesk_actions (
    id INT PRIMARY KEY,
    ticket_id INT,
    type INT,
    origin INT,
    description TEXT,
    created_by_id VARCHAR(255),
    created_date DATETIME,
    status VARCHAR(100),
    justification VARCHAR(255),
    CONSTRAINT fk_action_ticket FOREIGN KEY (ticket_id) REFERENCES movidesk_tickets(id),
    CONSTRAINT fk_action_creator FOREIGN KEY (created_by_id) REFERENCES movidesk_people(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Carga inicial de Definições de Campos Personalizados
INSERT INTO movidesk_custom_field_definitions (id, name) VALUES 
(205913, 'Feiras Nacionais'),
(206220, 'Tipo de Atendimento AD Feiras'),
(208535, 'Unidade'),
(234900, 'País/Estado'),
(237387, 'Núcleo');
```

---

## 3. Lógica de Negócio e Tipos

- **personType**: 1 (Pessoa Física), 2 (Pessoa Jurídica), 4 (Departamento).
- **profileType**: 1 (Agente), 2 (Cliente), 3 (Agente e Cliente).
- **Ações**: A tabela `movidesk_actions` captura explicitamente o `status` e a `justification` no momento da interação.

---

## 4. Próximos Passos

1. **Criação do Banco**: Criar as tabelas acima no banco `dashinforma`.
2. **Importação**: Desenvolver o script de carga para ler o JSON e popular as tabelas.
