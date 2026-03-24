package com.globalis.api.movidesk.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movidesk_custom_field_definitions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovideskCustomFieldDefinition {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
