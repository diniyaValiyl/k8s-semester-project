package ru.itis.dental.controller;

import ru.itis.dental.dto.AppointmentDTO;
import ru.itis.dental.dto.DoctorDto;
import ru.itis.dental.dto.ReviewDTO;
import ru.itis.dental.entity.AppointmentEntity;
import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.entity.DoctorEntity;
import ru.itis.dental.entity.UserEntity;
import ru.itis.dental.service.AppointmentService;
import ru.itis.dental.service.UserService;
import ru.itis.dental.service.ReviewService;
import ru.itis.dental.repository.ClinicRepository;
import ru.itis.dental.repository.DoctorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/patient")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final ReviewService reviewService;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;

    // Вспомогательный метод для получения email из Authentication
    private String getEmailFromAuthentication(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof OAuth2User) {
            return ((OAuth2User) principal).getAttribute("email");
        } else {
            return auth.getName();
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        String email = getEmailFromAuthentication(auth);
        if (email == null) {
            return "redirect:/login";
        }

        UserEntity patient = userService.findByEmail(email).orElse(null);
        if (patient == null) {
            log.error("Patient not found with email: {}", email);
            return "redirect:/login";
        }

        List<AppointmentEntity> appointments = appointmentService.getUserAppointments(patient.getId());

        log.info("Пациент {} имеет {} записей", patient.getName(), appointments.size());
        for (AppointmentEntity a : appointments) {
            log.info("Запись ID: {}, статус: {}, дата: {}", a.getId(), a.getStatus(), a.getDateTime());
        }

        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointments);
        return "patient/dashboard";
    }

    @GetMapping("/appointments/create")
    public String showAppointmentForm(Model model, @RequestParam(required = false) Long clinicId, Authentication auth) {
        model.addAttribute("clinics", clinicRepository.findAll());

        Long patientId = null;
        if (auth != null && auth.isAuthenticated()) {
            String email = getEmailFromAuthentication(auth);
            if (email != null) {
                UserEntity patient = userService.findByEmail(email).orElse(null);
                if (patient != null) {
                    patientId = patient.getId();
                }
            }
        }
        model.addAttribute("patientId", patientId);

        List<DoctorEntity> allDoctors = doctorRepository.findAllWithClinics();

        Map<Long, List<DoctorDto>> doctorsByClinic = new HashMap<>();

        for (DoctorEntity doctor : allDoctors) {
            if (doctor.getClinics() != null && !doctor.getClinics().isEmpty()) {
                DoctorDto doctorDto = new DoctorDto();
                doctorDto.setId(doctor.getId());
                doctorDto.setName(doctor.getName());
                doctorDto.setSpecialization(doctor.getSpecialization() != null ? doctor.getSpecialization() : "Стоматолог");
                doctorDto.setExperience(doctor.getExperience());
                doctorDto.setPhotoUrl(doctor.getPhotoUrl());

                for (ClinicEntity clinic : doctor.getClinics()) {
                    doctorsByClinic.computeIfAbsent(clinic.getId(), k -> new ArrayList<>()).add(doctorDto);
                }
            }
        }

        model.addAttribute("doctorsByClinic", doctorsByClinic);

        AppointmentDTO appointmentDTO = new AppointmentDTO();
        if (clinicId != null) {
            appointmentDTO.setClinicId(clinicId);
        }
        model.addAttribute("appointment", appointmentDTO);

        return "patient/appointment-create";
    }

    @PostMapping("/appointments/create")
    public String createAppointment(@Valid @ModelAttribute("appointment") AppointmentDTO dto,
                                    BindingResult bindingResult,
                                    Authentication auth,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("clinics", clinicRepository.findAll());

            List<DoctorEntity> allDoctors = doctorRepository.findAllWithClinics();
            Map<Long, List<DoctorDto>> doctorsByClinic = new HashMap<>();
            for (DoctorEntity doctor : allDoctors) {
                if (doctor.getClinics() != null && !doctor.getClinics().isEmpty()) {
                    DoctorDto doctorDto = new DoctorDto();
                    doctorDto.setId(doctor.getId());
                    doctorDto.setName(doctor.getName());
                    doctorDto.setSpecialization(doctor.getSpecialization() != null ? doctor.getSpecialization() : "Стоматолог");
                    doctorDto.setExperience(doctor.getExperience());
                    doctorDto.setPhotoUrl(doctor.getPhotoUrl());

                    for (ClinicEntity clinic : doctor.getClinics()) {
                        doctorsByClinic.computeIfAbsent(clinic.getId(), k -> new ArrayList<>()).add(doctorDto);
                    }
                }
            }
            model.addAttribute("doctorsByClinic", doctorsByClinic);
            return "patient/appointment-create";
        }

        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                redirectAttributes.addFlashAttribute("error", "Не удалось определить email пользователя");
                return "redirect:/login";
            }

            UserEntity patient = userService.findByEmail(email).orElse(null);
            if (patient == null) {
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/login";
            }

            appointmentService.createAppointment(
                    patient.getId(),
                    dto.getDoctorId(),
                    dto.getClinicId(),
                    dto.getDateTime()
            );
            redirectAttributes.addFlashAttribute("success", "Запись успешно создана. Ожидает подтверждения.");
        } catch (Exception e) {
            log.error("Ошибка создания записи: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient/dashboard";
    }

    @GetMapping("/appointments/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                redirectAttributes.addFlashAttribute("error", "Не удалось определить email пользователя");
                return "redirect:/login";
            }

            UserEntity patient = userService.findByEmail(email).orElse(null);
            if (patient == null) {
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/login";
            }

            AppointmentEntity appointment = appointmentService.getAppointmentById(id);

            if (!appointment.getPatient().getId().equals(patient.getId())) {
                redirectAttributes.addFlashAttribute("error", "Вы можете отменять только свои записи");
                return "redirect:/patient/dashboard";
            }

            if (appointment.getStatus() == AppointmentEntity.Status.CANCELLED) {
                redirectAttributes.addFlashAttribute("error", "Эта запись уже отменена");
                return "redirect:/patient/dashboard";
            }

            if (appointment.getStatus() == AppointmentEntity.Status.COMPLETED) {
                redirectAttributes.addFlashAttribute("error", "Нельзя отменить завершённую запись");
                return "redirect:/patient/dashboard";
            }

            appointmentService.updateAppointmentStatus(id, AppointmentEntity.Status.CANCELLED);
            redirectAttributes.addFlashAttribute("success", "Запись успешно отменена");
        } catch (Exception e) {
            log.error("Ошибка отмены записи: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient/dashboard";
    }

    @GetMapping("/reviews/add/{clinicId}")
    public String showReviewForm(@PathVariable Long clinicId, Model model, Authentication auth) {
        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                return "redirect:/login";
            }

            UserEntity patient = userService.findByEmail(email).orElse(null);
            if (patient == null) {
                return "redirect:/login";
            }
            model.addAttribute("clinicId", clinicId);
            model.addAttribute("patient", patient);
            model.addAttribute("review", new ReviewDTO());
            return "patient/review-form";
        } catch (Exception e) {
            log.error("Ошибка при показе формы отзыва: {}", e.getMessage());
            return "redirect:/clinics/" + clinicId;
        }
    }

    @PostMapping("/reviews/add/{clinicId}")
    public String addReview(@PathVariable Long clinicId,
                            @Valid @ModelAttribute("review") ReviewDTO reviewDTO,
                            BindingResult bindingResult,
                            Authentication auth,
                            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Пожалуйста, поставьте оценку от 1 до 5");
            return "redirect:/clinics/" + clinicId;
        }

        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                redirectAttributes.addFlashAttribute("error", "Не удалось определить email пользователя");
                return "redirect:/login";
            }

            UserEntity patient = userService.findByEmail(email).orElse(null);
            if (patient == null) {
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/login";
            }
            reviewService.addReview(patient.getId(), clinicId, reviewDTO.getRating(), reviewDTO.getComment());
            redirectAttributes.addFlashAttribute("success", "Спасибо за ваш отзыв!");
        } catch (Exception e) {
            log.error("Ошибка добавления отзыва: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/clinics/" + clinicId;
    }
}