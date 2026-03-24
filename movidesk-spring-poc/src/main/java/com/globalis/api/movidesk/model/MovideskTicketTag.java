package com.globalis.api.movidesk.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "movidesk_ticket_tags")
@IdClass(MovideskTicketTag.MovideskTicketTagId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovideskTicketTag {

    @Id
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Id
    @Column(name = "tag")
    private String tag;

    // Chave Primária Composta
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class MovideskTicketTagId implements Serializable {
        private Integer ticketId;
        private String tag;
    }
}
