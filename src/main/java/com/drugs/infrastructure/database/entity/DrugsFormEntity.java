package com.drugs.infrastructure.database.entity;

import com.drugs.controller.dto.DrugsFormDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "drugs_form")
public class DrugsFormEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drugs_form_id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "drugsForm", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<DrugsEntity> drugs;

    public DrugsFormDTO toDTO() {
        return DrugsFormDTO.valueOf(this.name);
    }
}
