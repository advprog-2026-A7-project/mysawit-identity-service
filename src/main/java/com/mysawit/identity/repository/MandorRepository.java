package com.mysawit.identity.repository;

import com.mysawit.identity.model.Mandor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MandorRepository extends JpaRepository<Mandor, String> {
    boolean existsByCertificationNumber(String certificationNumber);
}
