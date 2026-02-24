require('dotenv').config();
const express = require('express');
const axios = require('axios');
const cors = require('cors');

const app = express();
app.use(cors());

const MOVIDESK_TOKEN = process.env.MOVIDESK_TOKEN;
const BASE_URL = 'https://api.movidesk.com/public/v1/tickets';

// Função auxiliar para pegar o valor do Custom Field
function getCustomFieldValue(ticket, customFieldId) {
    if (!ticket.customFieldValues) return null;
    
    const field = ticket.customFieldValues.find(cf => cf.customFieldId === customFieldId);
    if (!field) return null;

    if (field.value) return String(field.value);
    if (field.items && field.items.length > 0) return String(field.items[0].customFieldItem);
    
    return null;
}

// Função para fazer pausas (evitar Rate Limit da API do Movidesk)
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

app.get('/api/tickets', async (req, res) => {
    try {
        if (!MOVIDESK_TOKEN) {
            return res.status(500).json({ error: 'Token do Movidesk não configurado no backend.' });
        }

        console.log("Iniciando busca de tickets...");

        // 1. Calcular a data de 120 dias atrás
        const dateOffset = (24 * 60 * 60 * 1000) * 120; // 120 dias
        const startDate = new Date(Date.now() - dateOffset).toISOString();

        let allTickets = [];
        let skip = 0;
        const top = 100;
        let hasMore = true;

        // 2. Buscar a lista básica de tickets (Paginação)
        while (hasMore && allTickets.length < 9999) {
            console.log(`Buscando página (skip=${skip})...`);
            const listResponse = await axios.get(BASE_URL, {
                params: {
                    token: MOVIDESK_TOKEN,
                    $select: 'id,createdDate,originEmailAccount,ownerTeam',
                    $filter: `createdDate ge ${startDate} and (ownerTeam eq 'ADF - Brasil' or ownerTeam eq 'ADF - Informa')`,
                    $orderby: 'createdDate desc',
                    $top: top,
                    $skip: skip
                }
            });

            const data = listResponse.data;
            if (data && data.length > 0) {
                allTickets = allTickets.concat(data);
                skip += top;
            } else {
                hasMore = false;
            }
        }

        console.log(`Foram encontrados ${allTickets.length} tickets no total. Buscando detalhes...`);

        // 3. Buscar os detalhes de todos os tickets recolhidos
        // Fazemos em lotes para NÃO derrubar a API do Movidesk (Rate Limit)
        const BATCH_SIZE = 10; // Busca de 10 em 10 tickets por vez
        let validDetailedTickets = [];

        for (let i = 0; i < allTickets.length; i += BATCH_SIZE) {
            const batch = allTickets.slice(i, i + BATCH_SIZE);
            const batchPromises = batch.map(async (ticket) => {
                try {
                    const detailResponse = await axios.get(BASE_URL, {
                        params: { token: MOVIDESK_TOKEN, id: ticket.id }
                    });
                    return detailResponse.data;
                } catch (err) {
                    console.error(`Erro ao buscar detalhes do ticket ${ticket.id}`, err.message);
                    return null;
                }
            });

            const batchResults = await Promise.all(batchPromises);
            validDetailedTickets.push(...batchResults.filter(t => t !== null));
            
            // Pausa de 500ms entre cada lote para respirar a API
            if (i + BATCH_SIZE < allTickets.length) {
               await delay(500);
            }
        }

        console.log("Processando dados e aplicando filtros finais...");

        // 4. Transformar, extrair campos e filtrar (apenas ADF - Informa)
        const finalData = validDetailedTickets
            .filter(t => t.ownerTeam === 'ADF - Informa')
            .map(t => {
                const estadoPais = getCustomFieldValue(t, 234900);
                const responsavel = t.owner ? (t.owner.businessName || t.owner.name) : null;
                const feira = t.originEmailAccount ? t.originEmailAccount.replace('ADF - ', '') : null;

                return {
                    Unidade: 'ADF - Informa',
                    Categoria: t.category,
                    Status: t.status,
                    Ticket: t.id,
                    Responsável: responsavel,
                    Feira: feira,
                    'Data de abertura': t.createdDate,
                    'Estado/País': estadoPais ? estadoPais.toUpperCase() : null
                };
            });

        console.log("Busca finalizada! Enviando dados para o Frontend.");
        // 5. Devolver para o React
        res.json(finalData);

    } catch (error) {
        console.error("Erro geral na API de Tickets", error);
        res.status(500).json({ error: 'Erro ao processar tickets do Movidesk' });
    }
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`🚀 Servidor backend rodando na porta ${PORT}`);
    console.log(`🔗 Endpoint de tickets: http://localhost:${PORT}/api/tickets`);
});
