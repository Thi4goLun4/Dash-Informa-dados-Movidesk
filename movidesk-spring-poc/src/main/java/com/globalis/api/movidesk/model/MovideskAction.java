package com.globalis.api.movidesk.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "movidesk_actions")
@IdClass(MovideskAction.MovideskActionId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovideskAction {

    @Id
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "type")
    private Integer type;

    @Column(name = "origin")
    private Integer origin;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private MovideskPerson createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "status")
    private String status;

    @Column(name = "justification")
    private String justification;

    // Classe para representar a Chave Primária Composta
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class MovideskActionId implements Serializable {
        private Integer ticketId;
        private Integer id;
    }
}
