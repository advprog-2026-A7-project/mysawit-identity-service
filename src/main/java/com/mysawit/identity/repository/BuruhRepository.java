package com.mysawit.identity.repository;

import com.mysawit.identity.model.Buruh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuruhRepository extends JpaRepository<Buruh, String> {
}
