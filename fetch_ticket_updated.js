const axios = require('axios');
const fs = require('fs');

const MOVIDESK_TOKEN = "f5bcf831-0b34-4676-96cf-38b254f3f6a4";
const TICKET_ID = "1093347";
const URL = `https://api.movidesk.com/public/v1/tickets?token=${MOVIDESK_TOKEN}&id=${TICKET_ID}`;

async function fetchTicket() {
    try {
        console.log(`Fetching ticket ${TICKET_ID}...`);
        const response = await axios.get(URL);
        const data = response.data;
        fs.writeFileSync('exemplo_atualizado.json', JSON.stringify(data, null, 2));
        console.log('Ticket data saved to exemplo_atualizado.json');
    } catch (error) {
        console.error('Error fetching ticket:', error.message);
        if (error.response) {
            console.error('Response data:', error.response.data);
        }
    }
}

fetchTicket();
