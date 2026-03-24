package com.globalis.api.movidesk.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "movidesk_custom_field_values")
@IdClass(MovideskCustomFieldValue.MovideskCustomFieldValueId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovideskCustomFieldValue {

    @Id
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Id
    @Column(name = "custom_field_id")
    private Integer customFieldId;

    @Column(name = "val_text", columnDefinition = "TEXT")
    private String valText;

    // Chave Primária Composta
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class MovideskCustomFieldValueId implements Serializable {
        private Integer ticketId;
        private Integer customFieldId;
    }
}
