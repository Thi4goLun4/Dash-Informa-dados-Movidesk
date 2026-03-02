# Plano de Implementação: Banco de Dados Movidesk

Este documento detalha o mapeamento dos campos do Movidesk para uma estrutura de banco de dados relacional, facilitando a criação de dashboards e consultas de performance.

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

## 2. Estrutura do Banco de Dados (SQL Sugerido)

Abaixo está o script SQL para criação das tabelas no PostgreSQL ou SQLite.

```sql
-- Tabela de Pessoas (Agentes e Clientes)
CREATE TABLE people (
    id VARCHAR(255) PRIMARY KEY,
    person_type INTEGER, -- 1: Pessoa, 2: Empresa, 4: Departamento
    profile_type INTEGER, -- 1: Agente, 2: Cliente, 3: Ambos
    business_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50)
);

-- Tabela de Tickets
CREATE TABLE tickets (
    id INTEGER PRIMARY KEY,
    subject VARCHAR(255),
    category VARCHAR(100),
    status VARCHAR(100),
    base_status VARCHAR(100),
    owner_id VARCHAR(255) REFERENCES people(id),
    service_first_level VARCHAR(255),
    service_second_level VARCHAR(255),
    service_third_level VARCHAR(255),
    created_date TIMESTAMP,
    resolved_in TIMESTAMP,
    action_count INTEGER,
    life_time_working_time INTEGER
);

-- Definição de Campos Personalizados
CREATE TABLE custom_field_definitions (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    description TEXT
);

-- Valores de Campos Personalizados por Ticket
CREATE TABLE custom_field_values (
    ticket_id INTEGER REFERENCES tickets(id),
    custom_field_id INTEGER REFERENCES custom_field_definitions(id),
    value TEXT, -- Valor textual ou JSON se for múltipla escolha
    PRIMARY KEY (ticket_id, custom_field_id)
);

-- Histórico de Ações (Interações)
CREATE TABLE actions (
    id INTEGER PRIMARY KEY,
    ticket_id INTEGER REFERENCES tickets(id),
    type INTEGER,
    origin INTEGER,
    description TEXT,
    created_by_id VARCHAR(255) REFERENCES people(id),
    created_date TIMESTAMP,
    status VARCHAR(100),          -- Status no momento da ação
    justification VARCHAR(255)    -- Justificativa no momento da ação
);

-- Carga inicial de Definições de Campos Personalizados
INSERT INTO custom_field_definitions (id, name) VALUES 
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
- **Ações**: A tabela `actions` agora captura explicitamente o `status` e a `justification` no momento da interação, permitindo rastrear o fluxo do chamado com precisão.

---

## 4. Próximos Passos

1. **Criação do Banco**: Executar o script SQL acima no ambiente de banco de dados escolhido.
2. **Script de Migração**: Desenvolver o script que lê o `exemplo_bruto.json` e insere os dados nas novas tabelas.
3. **Integração com Dashboard**: Conectar a ferramenta de dashboard às tabelas criadas.
