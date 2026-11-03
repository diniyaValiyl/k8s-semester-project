package ru.itis.dental.service;

import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.entity.ReviewEntity;
import ru.itis.dental.entity.UserEntity;
import ru.itis.dental.repository.ClinicRepository;
import ru.itis.dental.repository.ReviewRepository;
import ru.itis.dental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;

    @Transactional
    public ReviewEntity addReview(Long patientId, Long clinicId, Integer rating, String comment) {
        log.info("Adding review for clinic {} from patient {} with rating {}", clinicId, patientId, rating);

        UserEntity patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        ClinicEntity clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new RuntimeException("Clinic not found"));

        ReviewEntity review = ReviewEntity.builder()
                .patient(patient)
                .clinic(clinic)
                .rating(rating)
                .comment(comment)
                .build();

        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewEntity> getClinicReviews(Long clinicId) {
        return reviewRepository.findByClinicIdOrderByCreatedAtDesc(clinicId);
    }

    @Transactional(readOnly = true)
    public Double getClinicAverageRating(Long clinicId) {
        Double avg = reviewRepository.getAverageRatingForClinic(clinicId);
        return avg != null ? avg : 0.0;
    }
}