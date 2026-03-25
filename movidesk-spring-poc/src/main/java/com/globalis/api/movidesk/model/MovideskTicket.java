package com.globalis.api.movidesk.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movidesk_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovideskTicket {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "subject")
    private String subject;

    @Column(name = "category")
    private String category;

    @Column(name = "status")
    private String status;

    @Column(name = "base_status")
    private String baseStatus;

    // Relacionamento Foreign Key com People (Owner)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private MovideskPerson owner;

    // Relacionamento Foreign Key com People (Creator/Main Client)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private MovideskPerson createdBy;

    @Column(name = "client_reference")
    private String clientReference;

    @Column(name = "organization_business_name")
    private String organizationBusinessName;

    @Column(name = "service_first_level")
    private String serviceFirstLevel;

    @Column(name = "service_second_level")
    private String serviceSecondLevel;

    @Column(name = "service_third_level")
    private String serviceThirdLevel;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "resolved_in")
    private LocalDateTime resolvedIn;

    @Column(name = "action_count")
    private Integer actionCount;

    @Column(name = "life_time_working_time")
    private Integer lifeTimeWorkingTime;
}
