package ru.itis.dental.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicService {

    private final ClinicRepository clinicRepository;
    private final FileService fileService;

    @Cacheable(value = "clinics", key = "#id")
    @Transactional(readOnly = true)
    public ClinicEntity getById(Long id) {
        log.info("Getting clinic by id: {}", id);
        return clinicRepository.findByIdWithDoctors(id)
                .orElseThrow(() -> new RuntimeException("Clinic not found with id: " + id));
    }

    @Cacheable(value = "clinics", key = "'all'")
    @Transactional(readOnly = true)
    public List<ClinicEntity> getAll() {
        log.info("Getting all clinics from database");
        List<ClinicEntity> clinics = clinicRepository.findAllWithDoctors();
        log.info("Found {} clinics", clinics.size());
        return clinics;
    }

    @CacheEvict(value = {"clinics", "doctors"}, allEntries = true)
    @Transactional
    public ClinicEntity create(ClinicEntity clinic, MultipartFile logo) {
        log.info("Creating new clinic: {}", clinic.getName());
        if (logo != null && !logo.isEmpty()) {
            String logoUrl = fileService.saveFile(logo, "clinics");
            clinic.setLogoUrl(logoUrl);
        }
        return clinicRepository.save(clinic);
    }

    @CacheEvict(value = {"clinics", "doctors"}, allEntries = true)
    @Transactional
    public void delete(Long id) {
        log.info("Deleting clinic with id: {}", id);

        ClinicEntity clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Clinic not found with id: " + id));

        if (clinic.getLogoUrl() != null && !clinic.getLogoUrl().isEmpty()) {
            try {
                fileService.deleteFile(clinic.getLogoUrl());
                log.info("Deleted logo file: {}", clinic.getLogoUrl());
            } catch (Exception e) {
                log.error("Failed to delete logo file: {}", e.getMessage());
            }
        }

        clinicRepository.deleteById(id);
        log.info("Clinic deleted successfully");
    }

    @Transactional(readOnly = true)
    public List<ClinicEntity> search(String query) {
        return clinicRepository.findByNameContainingIgnoreCase(query);
    }

    @CacheEvict(value = {"clinics", "doctors"}, allEntries = true)
    @Transactional
    public ClinicEntity update(Long id, ClinicEntity clinic, MultipartFile logo) {
        log.info("Updating clinic with id: {}", id);
        ClinicEntity existing = getById(id);
        existing.setName(clinic.getName());
        existing.setAddress(clinic.getAddress());
        existing.setPhone(clinic.getPhone());
        existing.setWorkingHours(clinic.getWorkingHours());
        existing.setLatitude(clinic.getLatitude());
        existing.setLongitude(clinic.getLongitude());
        existing.setSiteUrl(clinic.getSiteUrl());

        if (clinic.getWorkStart() != null) {
            existing.setWorkStart(clinic.getWorkStart());
        }
        if (clinic.getWorkEnd() != null) {
            existing.setWorkEnd(clinic.getWorkEnd());
        }
        if (clinic.getWorkDays() != null && !clinic.getWorkDays().isEmpty()) {
            existing.setWorkDays(clinic.getWorkDays());
        }

        if (logo != null && !logo.isEmpty()) {
            if (existing.getLogoUrl() != null && !existing.getLogoUrl().isEmpty()) {
                fileService.deleteFile(existing.getLogoUrl());
            }
            String logoUrl = fileService.saveFile(logo, "clinics");
            existing.setLogoUrl(logoUrl);
        }
        return clinicRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<ClinicEntity> getTopRated() {
        log.info("Getting top rated clinics (rating > 4.0)");
        return clinicRepository.findClinicsWithRatingAbove(4.0);
    }
}