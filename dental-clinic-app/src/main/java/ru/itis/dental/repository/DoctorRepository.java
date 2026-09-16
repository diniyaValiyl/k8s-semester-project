package ru.itis.dental.repository;

import ru.itis.dental.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<DoctorEntity, Long> {

    List<DoctorEntity> findBySpecializationContainingIgnoreCase(String specialization);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DoctorEntity d JOIN d.clinics c WHERE d.id = :doctorId AND c.id = :clinicId")
    boolean existsByIdAndClinicsId(@Param("doctorId") Long doctorId, @Param("clinicId") Long clinicId);

    @Query("SELECT DISTINCT d FROM DoctorEntity d LEFT JOIN FETCH d.clinics WHERE d.id = :id")
    Optional<DoctorEntity> findByIdWithClinics(@Param("id") Long id);

    @Query("SELECT DISTINCT d FROM DoctorEntity d LEFT JOIN FETCH d.clinics")
    List<DoctorEntity> findAllWithClinics();

    @Query("SELECT d FROM DoctorEntity d JOIN d.clinics c WHERE c.id = :clinicId ORDER BY d.name")
    List<DoctorEntity> findDoctorsByClinicId(@Param("clinicId") Long clinicId);

}