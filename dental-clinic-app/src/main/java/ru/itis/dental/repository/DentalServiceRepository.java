package ru.itis.dental.repository;

import ru.itis.dental.entity.DentalServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DentalServiceRepository extends JpaRepository<DentalServiceEntity, Long> {

    List<DentalServiceEntity> findByNameContainingIgnoreCase(String name);

    @Query("SELECT DISTINCT cs.service FROM ClinicServiceEntity cs WHERE cs.clinic.id = :clinicId")
    List<DentalServiceEntity> findServicesByClinicId(@Param("clinicId") Long clinicId);
}