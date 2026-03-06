package com.mysawit.identity.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "buruhs")
public class Buruh extends User {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mandor_id")
    private Mandor mandor;
}
