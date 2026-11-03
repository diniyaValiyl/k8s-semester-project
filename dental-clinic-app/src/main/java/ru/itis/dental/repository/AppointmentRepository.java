package ru.itis.dental.repository;

import ru.itis.dental.entity.AppointmentEntity;
import ru.itis.dental.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    List<AppointmentEntity> findByPatientOrderByDateTimeDesc(UserEntity patient);

    boolean existsByDoctorIdAndDateTime(Long doctorId, LocalDateTime dateTime);

    boolean existsByPatientIdAndDateTime(Long patientId, LocalDateTime dateTime);

    @Query("SELECT a.dateTime FROM AppointmentEntity a WHERE a.doctor.id = :doctorId AND a.dateTime BETWEEN :startDate AND :endDate")
    List<LocalDateTime> findBusyTimeSlotsByDoctorId(@Param("doctorId") Long doctorId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.clinic.id = :clinicId ORDER BY a.dateTime DESC")
    List<AppointmentEntity> findByClinicIdOrderByDateTimeDesc(@Param("clinicId") Long clinicId);
}