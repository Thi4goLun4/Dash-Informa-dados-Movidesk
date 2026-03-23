require('dotenv').config();
const axios = require('axios');
const mysql = require('mysql2/promise');

const MOVIDESK_TOKEN = process.env.MOVIDESK_TOKEN;
const BASE_URL = 'https://api.movidesk.com/public/v1/tickets';

// Configurações do banco de dados MySQL
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT || 3306,
    // Em alguns casos de conexões remota cloud é preciso ativar connectTimeout ou permiti-lo:
    connectTimeout: 20000
};

// Função auxiliar para converter formato de data do Movidesk para o MySQL DATETIME
const formatDate = (dateStr) => {
    if (!dateStr) return null;
    return new Date(dateStr).toISOString().slice(0, 19).replace('T', ' ');
};

// Função auxiliar para aguardar (timeout/delay)
const delay = ms => new Promise(res => setTimeout(res, ms));

async function syncTickets() {
    let connection;
    try {
        console.log("Conectando ao banco de dados MySQL Google Cloud...");
        connection = await mysql.createConnection(dbConfig);
        console.log("✅ Conexão bem sucedida!");

        console.log("⏳ Buscando tickets da equipe 'ADF - Informa'...");
        
        let allTickets = [];
        let skip = 0;
        const top = 1000;
        let hasMore = true;

        // 1. Fase de Busca na API (Paginação)
        while (hasMore) {
            console.log(`📡 Buscando lote de tickets (skip=${skip})...`);
            const response = await axios.get(BASE_URL, {
                params: {
                    token: MOVIDESK_TOKEN,
                    $filter: "customFieldValues/any(c: c/customFieldId eq 208535 and c/items/any(i: startswith(i/customFieldItem, 'AD FEIRAS - INFORMA')))", 
                    $select: 'id', // Trazemos apenas o ID para a listagem para economizar banda
                    $top: top,
                    $skip: skip
                }
            });

            const data = response.data;
            if (data && data.length > 0) {
                // Guardamos apenas os IDs
                allTickets = allTickets.concat(data.map(t => t.id));
                skip += top;
            } else {
                hasMore = false;
            }
        }

        console.log(`🟢 Foram encontrados ${allTickets.length} tickets. Iniciando sincronização no banco...`);

        // 2. Fase de Inserção / Atualização no Banco de Dados (Upsert Detalhado)
        let processedCount = 0;
        for (const ticketId of allTickets) {
            
            // Buscar os detalhes completos de CADA ticket (Isso garante que todas as 'actions' venham)
            // Com retentativa para contornar lentidões/erros 504 do Movidesk
            let ticket = null;
            let retries = 3;
            for(let i=0; i<retries; i++) {
                try {
                    const detailResponse = await axios.get(BASE_URL, {
                        params: { 
                            token: MOVIDESK_TOKEN, 
                            id: ticketId,
                            $expand: 'owner,actions,customFieldValues'
                        }
                    });
                    ticket = detailResponse.data;
                    break; // Sucesso
                } catch (err) {
                    if (i === retries - 1) {
                        console.error(`⚠️ Falha defintiva ao buscar ticket ${ticketId} após ${retries} tentativas: ${err.message}`);
                    } else {
                        console.log(`⏳ Servidor Movidesk oscilou no ticket ${ticketId} (Erro ${err.response?.status || err.message}). Tentando novamente em 3 segundos...`);
                        await delay(3000);
                    }
                }
            }

            if(!ticket) continue; // Pula para o próximo se todas as tentativas falharem

            processedCount++;
            if (processedCount % 50 === 0) {
                 console.log(`✅ ${processedCount}/${allTickets.length} tickets processados...`);
            }
            
            // 2.1 Sincronizar Pessoa Responsável (Owner)
            if (ticket.owner) {
                const bName = ticket.owner.businessName || ticket.owner.name || null;
                const email = ticket.owner.email || null;
                const phone = ticket.owner.phone || null;

                await connection.execute(`
                    INSERT INTO movidesk_people (id, person_type, profile_type, business_name, email, phone)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        person_type = VALUES(person_type),
                        profile_type = VALUES(profile_type),
                        business_name = VALUES(business_name),
                        email = VALUES(email),
                        phone = VALUES(phone)
                `, [ticket.owner.id, ticket.owner.personType, ticket.owner.profileType, bName, email, phone]);
            }

            const ownerId = ticket.owner ? ticket.owner.id : null;

            // 2.2 Sincronizar o Ticket
            await connection.execute(`
                INSERT INTO movidesk_tickets (
                    id, subject, category, status, base_status, owner_id,
                    service_first_level, service_second_level, service_third_level,
                    created_date, resolved_in, action_count, life_time_working_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    subject = VALUES(subject),
                    category = VALUES(category),
                    status = VALUES(status),
                    base_status = VALUES(base_status),
                    owner_id = VALUES(owner_id),
                    service_first_level = VALUES(service_first_level),
                    service_second_level = VALUES(service_second_level),
                    service_third_level = VALUES(service_third_level),
                    created_date = VALUES(created_date),
                    resolved_in = VALUES(resolved_in),
                    action_count = VALUES(action_count),
                    life_time_working_time = VALUES(life_time_working_time)
            `, [
                ticket.id,
                ticket.subject || null,
                ticket.category || null,
                ticket.status || null,
                ticket.baseStatus || null,
                ownerId,
                ticket.serviceFirstLevel || null,
                ticket.serviceSecondLevel || null,
                ticket.serviceThirdLevel || null,
                formatDate(ticket.createdDate),
                formatDate(ticket.resolvedIn),
                ticket.actionCount || 0,
                ticket.lifetimeWorkingTime || 0
            ]);

            // 2.3 Sincronizar Valores de Campos Personalizados
            if (ticket.customFieldValues && ticket.customFieldValues.length > 0) {
                // Códigos dos campos levantados no plano de implementação
                const allowedFields = [205913, 206220, 208535, 234900, 237387, 238336];
                
                for (const cfv of ticket.customFieldValues) {
                    if (allowedFields.includes(cfv.customFieldId)) {
                        let valText = cfv.value ? String(cfv.value) : null;
                        
                        // Se for uma lista (dropdown), sobrescrevemos o valText com o item selecionado
                        if (cfv.items && cfv.items.length > 0 && cfv.items[0].customFieldItem) {
                            valText = String(cfv.items[0].customFieldItem);
                        }

                        await connection.execute(`
                            INSERT INTO movidesk_custom_field_values (ticket_id, custom_field_id, val_text)
                            VALUES (?, ?, ?)
                            ON DUPLICATE KEY UPDATE val_text = VALUES(val_text)
                        `, [ticket.id, cfv.customFieldId, valText]);
                    }
                }
            }

            // 2.4 Sincronizar Histórico de Ações
            if (ticket.actions && ticket.actions.length > 0) {
                for (const action of ticket.actions) {
                    const createdById = action.createdBy ? action.createdBy.id : null;
                    
                    // Se a ação tem autor e ele não existe no banco, precisa criar para honrar a FK (Foreign Key)
                    if (action.createdBy && createdById) {
                        const bName = action.createdBy.businessName || action.createdBy.name || null;
                        const email = action.createdBy.email || null;
                        const phone = action.createdBy.phone || null;
                        
                        await connection.execute(`
                            INSERT INTO movidesk_people (id, person_type, profile_type, business_name, email, phone)
                            VALUES (?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE business_name = VALUES(business_name)
                        `, [createdById, action.createdBy.personType, action.createdBy.profileType, bName, email, phone]);
                    }

                    await connection.execute(`
                        INSERT INTO movidesk_actions (
                            id, ticket_id, type, origin, description, created_by_id, created_date, status, justification
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            description = VALUES(description),
                            status = VALUES(status),
                            justification = VALUES(justification)
                    `, [
                        action.id,
                        ticket.id,
                        action.type || null,
                        action.origin || null,
                        action.description || null,
                        createdById,
                        formatDate(action.createdDate),
                        action.status || null,
                        action.justification || null
                    ]);
                }
            }

            // 2.5 Sincronizar Tags do Ticket
            if (ticket.tags && ticket.tags.length > 0) {
                for (const tag of ticket.tags) {
                    await connection.execute(`
                        INSERT IGNORE INTO movidesk_ticket_tags (ticket_id, tag)
                        VALUES (?, ?)
                    `, [ticket.id, tag]);
                }
            }
        }

        console.log("🎉 Sincronização concluída com sucesso!");

    } catch (error) {
        console.error("❌ Erro durante a sincronização:");
        if (error.response) {
            console.error("Erro na API do Movidesk:", error.response.data);
        } else if (error.sqlMessage) {
            console.error("Erro no Banco de Dados MySQL:", error.sqlMessage);
        } else {
            console.error(error.message);
        }
    } finally {
        if (connection) {
            await connection.end();
            console.log("Conexão com o banco de dados encerrada.");
        }
    }
}

syncTickets();
