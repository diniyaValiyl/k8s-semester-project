package ru.itis.dental.repository;

import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.entity.ReviewEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class CustomClinicRepositoryImpl implements CustomClinicRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ClinicEntity> findClinicsByMinRatingCriteria(double minRating) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClinicEntity> query = cb.createQuery(ClinicEntity.class);
        Root<ClinicEntity> clinic = query.from(ClinicEntity.class);

        Subquery<Double> subquery = query.subquery(Double.class);
        Root<ReviewEntity> review = subquery.from(ReviewEntity.class);
        subquery.select(cb.avg(review.get("rating")))
                .where(cb.equal(review.get("clinic"), clinic));

        query.where(cb.greaterThan(subquery, minRating));
        query.orderBy(cb.desc(subquery));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<ClinicEntity> findClinicsWithWorkingHoursContaining(String text) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClinicEntity> query = cb.createQuery(ClinicEntity.class);
        Root<ClinicEntity> clinic = query.from(ClinicEntity.class);

        query.where(cb.like(cb.lower(clinic.get("workingHours")), "%" + text.toLowerCase() + "%"));

        return entityManager.createQuery(query).getResultList();
    }
}