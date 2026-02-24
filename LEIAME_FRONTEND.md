# Documentação da API Node.js para o Frontend React

Olá! Para a comunicação do Frontend com a API do Movidesk, montamos um backend em Node.js. Isso esconde o Token de API do navegador e tira todo o peso de processamento do React.

## 1. Como rodar o Backend localmente

1. Tenha o **Node.js** instalado na sua máquina.
2. Na pasta `movidesk-api`, crie um arquivo chamado `.env` e coloque o token do Movidesk nele:
   ```env
   MOVIDESK_TOKEN=f5bcf831-0b34-4676-96cf-38b254f3f6a4
   PORT=3001
   ```
3. Abra o terminal na pasta e instale as dependências:
   ```bash
   npm install
   ```
4. Inicie o servidor:
   ```bash
   node index.js
   ```
O servidor estará rodando em `http://localhost:3001`.

---

## 2. Como consumir no React (Frontend)

O backend possui uma única rota principal configurada (com suporte a CORS, então não dará erro de requisição cruzada):
**`GET http://localhost:3001/api/tickets`**

Esta rota já faz a paginação, a busca em lote dos detalhes (para não levar _Rate Limit_ 429 da Movidesk) e o tratamento de todos os Custom Fields.

### Exemplo de código React

```jsx
import React, { useEffect, useState } from 'react';

export default function TabelaTickets() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchTickets() {
      try {
        setLoading(true);
        // Chama o nosso backend Node.js
        const response = await fetch('http://localhost:3001/api/tickets');
        
        if (!response.ok) {
          throw new Error('Falha ao buscar os tickets');
        }

        const data = await response.json();
        setTickets(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    fetchTickets();
  }, []);

  if (loading) return <div>Carregando tickets... (Isso pode demorar devido à quantidade de dados)</div>;
  if (error) return <div>Erro: {error}</div>;

  return (
    <div style={{ padding: '20px' }}>
      <h2>Tickets ADF - Informa (Últimos 120 dias)</h2>
      <table border="1" cellPadding="10">
        <thead>
          <tr>
            <th>Ticket</th>
            <th>Data de abertura</th>
            <th>Categoria</th>
            <th>Status</th>
            <th>Feira</th>
            <th>Responsável</th>
            <th>Estado/País</th>
          </tr>
        </thead>
        <tbody>
          {tickets.map((t) => (
            <tr key={t.Ticket}>
              <td>{t.Ticket}</td>
              <td>{new Date(t['Data de abertura']).toLocaleDateString('pt-BR')}</td>
              <td>{t.Categoria}</td>
              <td>{t.Status}</td>
              <td>{t.Feira}</td>
              <td>{t.Responsável || 'Não atribuído'}</td>
              <td>{t['Estado/País'] || 'Não preenchido'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

### O que o Array JSON retorna?
O backend devolve uma lista de objetos exatos como a tabela do Power Query que você tinha. Exemplo de um registro:

```json
[
  {
    "Unidade": "ADF - Informa",
    "Categoria": "Dúvida Comercial",
    "Status": "Novo",
    "Ticket": 123456,
    "Responsável": "João da Silva",
    "Feira": "Fispal Tecnologia",
    "Data de abertura": "2023-10-24T14:30:00Z",
    "Estado/País": "SÃO PAULO"
  }
]
```
