package com.firstaid.infrastructure.database.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "drugs")
public class DrugEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drug_id")
    private Integer drugId;

    @Column(name = "drug_name")
    private String drugName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "drug_form_id", nullable = false)
    private DrugFormEntity drugForm;

    @Column(name = "expiration_date")
    private OffsetDateTime expirationDate;

    @Column(name = "drug_description")
    private String drugDescription;

    @Builder.Default
    @Column(name = "alert_sent", nullable = false)
    private boolean alertSent = false;
}
