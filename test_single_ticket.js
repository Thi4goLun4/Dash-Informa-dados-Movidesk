require('dotenv').config();
const axios = require('axios');

const MOVIDESK_TOKEN = process.env.MOVIDESK_TOKEN;
const BASE_URL = 'https://api.movidesk.com/public/v1/tickets';

async function testFilter() {
    try {
        console.log("Testando filtro OData startswith AD FEIRAS");
        const response = await axios.get(BASE_URL, {
            params: {
                token: MOVIDESK_TOKEN,
                $filter: "customFieldValues/any(c: c/customFieldId eq 208535 and c/items/any(i: startswith(i/customFieldItem, 'AD FEIRAS')))",
                $select: 'id',
                $top: 5
            }
        });
        console.log(`Sucesso, tickets retornados: ${response.data.length}`);
    } catch (e) {
        console.error("Erro no OData filter:", e.response ? e.response.data : e.message);
    }
}
testFilter();
