require('dotenv').config();
const axios = require('axios');
const fs = require('fs');

const MOVIDESK_TOKEN = process.env.MOVIDESK_TOKEN;
const BASE_URL = 'https://api.movidesk.com/public/v1/tickets';

async function extrairTicketBruto() {
    try {
        if (!MOVIDESK_TOKEN) {
            console.error("ERRO: O Token do Movidesk não foi encontrado. Crie o arquivo .env com o MOVIDESK_TOKEN=seu_token");
            return;
        }

        console.log("Conectando ao Movidesk para buscar 1 ticket de exemplo...");

        const ticketId = '1062631';
        console.log(`Buscando ticket específico: #${ticketId}`);
        console.log("Baixando todas as informações brutas (raio-x) deste ticket...");

        // 2. Busca o detalhe completo deste ticket específico
        const detailResponse = await axios.get(BASE_URL, {
            params: { token: MOVIDESK_TOKEN, id: ticketId }
        });

        // 3. Salva os dados brutos em um arquivo JSON na pasta
        fs.writeFileSync('exemplo_bruto.json', JSON.stringify(detailResponse.data, null, 2), 'utf-8');
        
        console.log("✅ Concluído! O arquivo 'exemplo_bruto.json' foi salvo na sua pasta.");
        console.log("Abra este arquivo no VS Code para ver todos os campos que existem na Movidesk de fato.");

    } catch (error) {
        console.error("Erro ao buscar o ticket no Movidesk:", error.message);
        if (error.response) {
            console.error("Detalhes do erro do Movidesk:", error.response.data);
        }
    }
}

extrairTicketBruto();
