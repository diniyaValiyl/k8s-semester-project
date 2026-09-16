package ru.itis.dental.controller;

import ru.itis.dental.dto.DoctorFormDTO;
import ru.itis.dental.entity.AppointmentEntity;
import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.entity.DoctorEntity;
import ru.itis.dental.entity.UserEntity;
import ru.itis.dental.repository.AppointmentRepository;
import ru.itis.dental.repository.ClinicRepository;
import ru.itis.dental.service.DoctorService;
import ru.itis.dental.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
@Slf4j
public class ClinicAdminController {

    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;
    private final UserService userService;
    private final DoctorService doctorService;

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
        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                return "redirect:/login";
            }

            UserEntity admin = userService.findByEmail(email).orElse(null);

            if (admin == null || admin.getManagedClinic() == null) {
                return "redirect:/login";
            }

            ClinicEntity clinic = admin.getManagedClinic();
            List<AppointmentEntity> appointments = appointmentRepository.findByClinicIdOrderByDateTimeDesc(clinic.getId());

            model.addAttribute("clinic", clinic);
            model.addAttribute("appointments", appointments);
            model.addAttribute("statuses", AppointmentEntity.Status.values());

            return "clinic-admin/dashboard";
        } catch (Exception e) {
            log.error("Ошибка загрузки дашборда: {}", e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/appointments/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                redirectAttributes.addFlashAttribute("error", "Не удалось определить пользователя");
                return "redirect:/login";
            }

            UserEntity admin = userService.findByEmail(email).orElse(null);

            if (admin == null || admin.getManagedClinic() == null) {
                redirectAttributes.addFlashAttribute("error", "Нет доступа");
                return "redirect:/login";
            }

            AppointmentEntity appointment = appointmentRepository.findById(id).orElse(null);

            if (appointment == null) {
                redirectAttributes.addFlashAttribute("error", "Запись не найдена");
                return "redirect:/clinic-admin/dashboard";
            }

            // Проверка: принадлежит ли запись клинике этого админа
            if (!appointment.getClinic().getId().equals(admin.getManagedClinic().getId())) {
                redirectAttributes.addFlashAttribute("error", "Нет доступа к этой записи");
                return "redirect:/clinic-admin/dashboard";
            }

            appointment.setStatus(AppointmentEntity.Status.valueOf(status));
            appointmentRepository.save(appointment);

            redirectAttributes.addFlashAttribute("success", "Статус записи обновлен");
        } catch (Exception e) {
            log.error("Ошибка обновления статуса: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/clinic-admin/dashboard";
    }

    // УПРАВЛЕНИЕ ВРАЧАМИ КЛИНИКИ

    @GetMapping("/doctors")
    public String listDoctors(Model model, Authentication auth) {
        String email = getEmailFromAuthentication(auth);
        if (email == null) {
            return "redirect:/login";
        }

        UserEntity admin = userService.findByEmail(email).orElse(null);

        if (admin == null || admin.getManagedClinic() == null) {
            return "redirect:/login";
        }

        ClinicEntity clinic = admin.getManagedClinic();
        List<DoctorEntity> doctors = doctorService.getDoctorsByClinicId(clinic.getId());

        model.addAttribute("clinic", clinic);
        model.addAttribute("doctors", doctors);
        return "clinic-admin/doctors";
    }

    @GetMapping("/doctors/create")
    public String createDoctorForm(Model model, Authentication auth) {
        String email = getEmailFromAuthentication(auth);
        if (email == null) {
            return "redirect:/login";
        }

        UserEntity admin = userService.findByEmail(email).orElse(null);

        if (admin == null || admin.getManagedClinic() == null) {
            return "redirect:/login";
        }

        model.addAttribute("doctorForm", new DoctorFormDTO());
        model.addAttribute("clinicId", admin.getManagedClinic().getId());
        return "clinic-admin/doctor-form";
    }

    @PostMapping("/doctors/create")
    public String createDoctor(@ModelAttribute DoctorFormDTO formDTO,
                               @RequestParam("photo") MultipartFile photo,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                redirectAttributes.addFlashAttribute("error", "Не удалось определить пользователя");
                return "redirect:/login";
            }

            UserEntity admin = userService.findByEmail(email).orElse(null);

            if (admin == null || admin.getManagedClinic() == null) {
                redirectAttributes.addFlashAttribute("error", "Нет доступа");
                return "redirect:/login";
            }

            ClinicEntity clinic = admin.getManagedClinic();

            // Создаём врача
            DoctorEntity doctor = new DoctorEntity();
            doctor.setName(formDTO.getName());
            doctor.setSpecialization(formDTO.getSpecialization());
            doctor.setExperience(formDTO.getExperience());
            doctor.setEducation(formDTO.getEducation());

            DoctorEntity savedDoctor = doctorService.save(doctor, photo);

            // Привязываем врача к клинике
            doctorService.addDoctorToClinics(savedDoctor.getId(), List.of(clinic.getId()));

            redirectAttributes.addFlashAttribute("success", "Врач успешно добавлен в клинику");
        } catch (Exception e) {
            log.error("Ошибка создания врача: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/clinic-admin/doctors";
    }

    @GetMapping("/doctors/edit/{id}")
    public String editDoctorForm(@PathVariable Long id, Model model, Authentication auth) {
        String email = getEmailFromAuthentication(auth);
        if (email == null) {
            return "redirect:/login";
        }

        UserEntity admin = userService.findByEmail(email).orElse(null);

        if (admin == null || admin.getManagedClinic() == null) {
            return "redirect:/login";
        }

        ClinicEntity clinic = admin.getManagedClinic();
        DoctorEntity doctor = doctorService.getById(id);

        // Проверка, что врач принадлежит этой клинике
        if (doctor.getClinics().stream().noneMatch(c -> c.getId().equals(clinic.getId()))) {
            return "redirect:/clinic-admin/doctors";
        }

        DoctorFormDTO formDTO = new DoctorFormDTO();
        formDTO.setId(doctor.getId());
        formDTO.setName(doctor.getName());
        formDTO.setSpecialization(doctor.getSpecialization());
        formDTO.setExperience(doctor.getExperience());
        formDTO.setEducation(doctor.getEducation());

        model.addAttribute("doctorForm", formDTO);
        model.addAttribute("clinicId", clinic.getId());
        return "clinic-admin/doctor-form";
    }

    @PostMapping("/doctors/edit/{id}")
    public String updateDoctor(@PathVariable Long id,
                               @ModelAttribute DoctorFormDTO formDTO,
                               @RequestParam("photo") MultipartFile photo,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                redirectAttributes.addFlashAttribute("error", "Не удалось определить пользователя");
                return "redirect:/login";
            }

            UserEntity admin = userService.findByEmail(email).orElse(null);

            if (admin == null || admin.getManagedClinic() == null) {
                redirectAttributes.addFlashAttribute("error", "Нет доступа");
                return "redirect:/login";
            }

            ClinicEntity clinic = admin.getManagedClinic();
            DoctorEntity existingDoctor = doctorService.getById(id);

            // Проверка, что врач принадлежит этой клинике
            if (existingDoctor.getClinics().stream().noneMatch(c -> c.getId().equals(clinic.getId()))) {
                redirectAttributes.addFlashAttribute("error", "Нет доступа к этому врачу");
                return "redirect:/clinic-admin/doctors";
            }

            DoctorEntity doctor = new DoctorEntity();
            doctor.setName(formDTO.getName());
            doctor.setSpecialization(formDTO.getSpecialization());
            doctor.setExperience(formDTO.getExperience());
            doctor.setEducation(formDTO.getEducation());

            doctorService.update(id, doctor, photo);

            redirectAttributes.addFlashAttribute("success", "Врач успешно обновлён");
        } catch (Exception e) {
            log.error("Ошибка обновления врача: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/clinic-admin/doctors";
    }

    @GetMapping("/doctors/delete/{id}")
    public String deleteDoctor(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            String email = getEmailFromAuthentication(auth);
            if (email == null) {
                redirectAttributes.addFlashAttribute("error", "Не удалось определить пользователя");
                return "redirect:/login";
            }

            UserEntity admin = userService.findByEmail(email).orElse(null);

            if (admin == null || admin.getManagedClinic() == null) {
                redirectAttributes.addFlashAttribute("error", "Нет доступа");
                return "redirect:/login";
            }

            ClinicEntity clinic = admin.getManagedClinic();
            DoctorEntity doctor = doctorService.getById(id);

            // Проверка, что врач принадлежит этой клинике
            if (doctor.getClinics().stream().noneMatch(c -> c.getId().equals(clinic.getId()))) {
                redirectAttributes.addFlashAttribute("error", "Нет доступа к этому врачу");
                return "redirect:/clinic-admin/doctors";
            }

            // Удаляем связь врача с этой клиникой
            doctor.getClinics().remove(clinic);
            doctorService.save(doctor, null);

            // Если у врача больше нет клиник, удаляем его полностью
            if (doctor.getClinics().isEmpty()) {
                doctorService.delete(id);
            }

            redirectAttributes.addFlashAttribute("success", "Врач удалён из клиники");
        } catch (Exception e) {
            log.error("Ошибка удаления врача: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/clinic-admin/doctors";
    }
}