package ru.itis.dental.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.entity.DoctorEntity;
import ru.itis.dental.repository.ClinicRepository;
import ru.itis.dental.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;
    private final FileService fileService;

    @Cacheable(value = "doctors", key = "'all'")
    @Transactional(readOnly = true)
    public List<DoctorEntity> getAll() {
        log.info("Getting all doctors");
        List<DoctorEntity> doctors = doctorRepository.findAll();
        log.info("Found {} doctors", doctors.size());
        return doctors;
    }

    @Cacheable(value = "doctors", key = "#id")
    @Transactional(readOnly = true)
    public DoctorEntity getById(Long id) {
        log.info("Getting doctor by id: {}", id);
        return doctorRepository.findByIdWithClinics(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));
    }

    @CacheEvict(value = {"doctors", "clinics"}, allEntries = true)
    @Transactional
    public DoctorEntity save(DoctorEntity doctor, MultipartFile photo) {
        log.info("Saving doctor: {}", doctor.getName());

        if (photo != null && !photo.isEmpty()) {
            String photoUrl = fileService.saveFile(photo, "doctors");
            doctor.setPhotoUrl(photoUrl);
        }
        return doctorRepository.save(doctor);
    }

    @CacheEvict(value = {"doctors", "clinics"}, allEntries = true)
    @Transactional
    public DoctorEntity update(Long id, DoctorEntity doctor, MultipartFile photo) {
        log.info("Updating doctor with id: {}", id);
        DoctorEntity existing = getById(id);
        existing.setName(doctor.getName());
        existing.setSpecialization(doctor.getSpecialization());
        existing.setExperience(doctor.getExperience());
        existing.setEducation(doctor.getEducation());

        if (photo != null && !photo.isEmpty()) {
            if (existing.getPhotoUrl() != null) {
                fileService.deleteFile(existing.getPhotoUrl());
            }
            String photoUrl = fileService.saveFile(photo, "doctors");
            existing.setPhotoUrl(photoUrl);
        }

        return doctorRepository.save(existing);
    }

    @CacheEvict(value = {"doctors", "clinics"}, allEntries = true)
    @Transactional
    public void delete(Long id) {
        log.info("Deleting doctor with id: {}", id);
        DoctorEntity doctor = getById(id);
        if (doctor.getPhotoUrl() != null) {
            fileService.deleteFile(doctor.getPhotoUrl());
        }
        doctorRepository.deleteById(id);
    }

    @CacheEvict(value = {"doctors", "clinics"}, allEntries = true)
    @Transactional
    public void addDoctorToClinics(Long doctorId, List<Long> clinicIds) {
        log.info("Adding doctor {} to clinics: {}", doctorId, clinicIds);

        if (clinicIds == null || clinicIds.isEmpty()) {
            log.info("No clinics to add");
            return;
        }

        DoctorEntity doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));

        List<ClinicEntity> clinics = clinicRepository.findAllById(clinicIds);

        for (ClinicEntity clinic : clinics) {
            if (!doctor.getClinics().contains(clinic)) {
                doctor.getClinics().add(clinic);
                log.info("Added doctor {} to clinic {}", doctor.getName(), clinic.getName());
            }
        }

        doctorRepository.save(doctor);
        log.info("Doctor {} added to {} clinics", doctor.getName(), clinics.size());
    }

    @CacheEvict(value = {"doctors", "clinics"}, allEntries = true)
    @Transactional
    public void updateDoctorClinics(Long doctorId, List<Long> clinicIds) {
        log.info("Updating doctor {} clinics to: {}", doctorId, clinicIds);

        DoctorEntity doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));

        doctor.getClinics().clear();

        if (clinicIds != null && !clinicIds.isEmpty()) {
            List<ClinicEntity> clinics = clinicRepository.findAllById(clinicIds);
            doctor.getClinics().addAll(clinics);
            log.info("Doctor {} now has {} clinics", doctor.getName(), clinics.size());
        }

        doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public List<DoctorEntity> getDoctorsByClinicId(Long clinicId) {
        log.info("Getting doctors by clinic id: {}", clinicId);
        return doctorRepository.findDoctorsByClinicId(clinicId);
    }
}