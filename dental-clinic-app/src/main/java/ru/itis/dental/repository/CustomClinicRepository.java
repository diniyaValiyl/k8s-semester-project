package ru.itis.dental.repository;

import ru.itis.dental.entity.ClinicEntity;
import java.util.List;

public interface CustomClinicRepository {
    List<ClinicEntity> findClinicsByMinRatingCriteria(double minRating);
    List<ClinicEntity> findClinicsWithWorkingHoursContaining(String text);
}