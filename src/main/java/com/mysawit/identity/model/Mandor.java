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
@Table(name = "mandors")
public class Mandor extends User {

    @Column(nullable = false, unique = true, length = 50)
    private String certificationNumber;
}
