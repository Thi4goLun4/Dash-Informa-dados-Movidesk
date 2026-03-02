require('dotenv').config();
const axios = require('axios');

const MOVIDESK_TOKEN = process.env.MOVIDESK_TOKEN;
const BASE_URL = 'https://api.movidesk.com/public/v1/tickets';

async function testSingleTicket() {
    try {
        console.log("Buscando ticket 1049525 na API...");
        const response = await axios.get(BASE_URL, {
            params: {
                token: MOVIDESK_TOKEN,
                id: 1049525,
                $expand: 'owner,actions,customFieldValues'
            }
        });
        
        const ticket = response.data;
        console.log(`Ticket ID: ${ticket.id}`);
        console.log(`Actions count: ${ticket.actions ? ticket.actions.length : 'N/A'}`);
        
        if (ticket.actions && ticket.actions.length > 0) {
            console.log("\nPrimeira action:");
            console.log(JSON.stringify(ticket.actions[0], null, 2));
        } else {
             console.log("\n❌ O ticket não trouxe nenhuma action no campo 'actions'");
        }
        
    } catch (e) {
        console.error("Erro na request:", e.message);
    }
}

testSingleTicket();
