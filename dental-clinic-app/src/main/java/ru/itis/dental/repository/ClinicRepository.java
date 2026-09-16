package ru.itis.dental.repository;

import ru.itis.dental.entity.ClinicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicRepository extends JpaRepository<ClinicEntity, Long>, CustomClinicRepository {

    List<ClinicEntity> findByNameContainingIgnoreCase(String name);

    @Query("SELECT c FROM ClinicEntity c WHERE (SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.clinic.id = c.id) > :minRating")
    List<ClinicEntity> findClinicsWithRatingAbove(@Param("minRating") double minRating);

    // Загрузить клинику вместе с врачами
    @Query("SELECT DISTINCT c FROM ClinicEntity c LEFT JOIN FETCH c.doctors WHERE c.id = :id")
    Optional<ClinicEntity> findByIdWithDoctors(@Param("id") Long id);

    // Загрузить все клиники вместе с врачами
    @Query("SELECT DISTINCT c FROM ClinicEntity c LEFT JOIN FETCH c.doctors")
    List<ClinicEntity> findAllWithDoctors();
}