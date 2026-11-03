package ru.itis.dental.repository;

import ru.itis.dental.entity.ClinicServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicServiceRepository extends JpaRepository<ClinicServiceEntity, Long> {

    @Query("SELECT cs FROM ClinicServiceEntity cs WHERE cs.clinic.id = :clinicId")
    List<ClinicServiceEntity> findWithPricesByClinicId(@Param("clinicId") Long clinicId);
}