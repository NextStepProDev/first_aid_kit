package com.drugs.infrastructure.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "drugs")
public class DrugsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drugs_id")
    private Integer drugsId;

    @Column(name = "drugs_name")
    private String drugsName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "drugs_form_id", nullable = false)
    private DrugsFormEntity drugsForm;

    @Column(name = "expiration_date")
    private OffsetDateTime expirationDate;

    @Column(name = "drugs_description")
    private String drugsDescription;

    @Column(name = "alert_sent")
    private Boolean alertSent;
}
