package com.metalworkshop.PatientService.repository

;

import com.metalworkshop.PatientService.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByNameIgnoreCase(String name);

    Page<Patient> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(
            String email,
            UUID id);
}
