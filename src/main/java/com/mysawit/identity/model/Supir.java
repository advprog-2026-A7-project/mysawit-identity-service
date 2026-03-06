package com.mysawit.identity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supirs")
public class Supir extends User {

    // Will become @ManyToOne Kebun when the Kebun entity is available from the plantation module
    @Column(name = "kebun_id", length = 36)
    private String kebunId;
}
