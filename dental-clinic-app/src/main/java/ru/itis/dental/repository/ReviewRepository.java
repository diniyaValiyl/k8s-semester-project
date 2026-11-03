package ru.itis.dental.repository;

import ru.itis.dental.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findByClinicIdOrderByCreatedAtDesc(Long clinicId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.clinic.id = :clinicId")
    Double getAverageRatingForClinic(@Param("clinicId") Long clinicId);
}