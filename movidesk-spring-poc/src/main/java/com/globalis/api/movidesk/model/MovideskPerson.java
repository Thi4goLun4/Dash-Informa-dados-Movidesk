package com.globalis.api.movidesk.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movidesk_people")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovideskPerson {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "person_type")
    private Integer personType;

    @Column(name = "profile_type")
    private Integer profileType;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;
}
