require('dotenv').config();
const axios = require('axios');
const mysql = require('mysql2/promise');

async function debugActions() {
    const connection = await mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        port: 3306
    });

    try {
        console.log("Fetching 1071897 from API...");
        const res = await axios.get('https://api.movidesk.com/public/v1/tickets', {
            params: {
                token: process.env.MOVIDESK_TOKEN,
                id: 1071897,
                $expand: 'owner,actions,customFieldValues'
            }
        });

        const ticket = res.data;
        const actions = ticket.actions || [];
        console.log(`Ticket 1071897 retornou com ${actions.length} actions da API.`);

        for (const action of actions) {
            console.log(`Processando action ${action.id}...`);
            const createdById = action.createdBy ? action.createdBy.id : null;
            
            if (createdById) {
                const bName = action.createdBy.businessName || action.createdBy.name || null;
                await connection.execute(`
                    INSERT INTO movidesk_people (id, person_type, profile_type, business_name, email, phone)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE business_name = VALUES(business_name)
                `, [createdById, action.createdBy.personType, action.createdBy.profileType, bName, null, null]);
                console.log(`Person ${createdById} inserida/atualizada.`);
            }

            const dateStr = action.createdDate ? new Date(action.createdDate).toISOString().slice(0, 19).replace('T', ' ') : null;

            await connection.execute(`
                INSERT INTO movidesk_actions (
                    id, ticket_id, type, origin, description, created_by_id, created_date, status, justification
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status)
            `, [
                action.id, ticket.id, action.type || null, action.origin || null, 
                'Teste de desc', createdById, dateStr, action.status || null, 
                action.justification || null
            ]);
            console.log(`Action ${action.id} inserida no banco.`);
        }
    } catch (e) {
        if (e.response) {
            console.error("API Error:", e.response.status, e.response.data);
        } else {
            console.error("DB Error:", e.message);
        }
    } finally {
        await connection.end();
    }
}

debugActions();
