package ru.itis.dental.controller;

import ru.itis.dental.dto.ClinicFormDTO;
import ru.itis.dental.dto.DoctorFormDTO;
import ru.itis.dental.dto.RegistrationDTO;
import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.entity.DoctorEntity;
import ru.itis.dental.entity.AppointmentEntity;
import ru.itis.dental.entity.UserEntity;
import ru.itis.dental.repository.AppointmentRepository;
import ru.itis.dental.service.ClinicService;
import ru.itis.dental.service.DoctorService;
import ru.itis.dental.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ClinicService clinicService;
    private final DoctorService doctorService;
    private final AppointmentRepository appointmentRepository;
    private final UserService userService;

    @GetMapping
    public String dashboard(Model model) {
        try {
            List<ClinicEntity> allClinics = clinicService.getAll();
            List<DoctorEntity> allDoctors = doctorService.getAll();
            List<AppointmentEntity> allAppointments = appointmentRepository.findAll();

            long totalClinics = allClinics.size();
            long totalDoctors = allDoctors.size();
            long totalAppointments = allAppointments.size();

            long pendingAppointments = 0;
            long todayAppointments = 0;
            LocalDate today = LocalDate.now();

            for (AppointmentEntity a : allAppointments) {
                if (a.getStatus() == AppointmentEntity.Status.PENDING) {
                    pendingAppointments++;
                }
                if (a.getDateTime().toLocalDate().equals(today)) {
                    todayAppointments++;
                }
            }

            Map<String, Long> stats = new HashMap<>();
            stats.put("totalClinics", totalClinics);
            stats.put("totalDoctors", totalDoctors);
            stats.put("totalAppointments", totalAppointments);
            stats.put("pendingAppointments", pendingAppointments);
            stats.put("todayAppointments", todayAppointments);

            model.addAttribute("stats", stats);
            return "admin/dashboard";
        } catch (Exception e) {
            log.error("Ошибка загрузки админ-панели: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка загрузки данных: " + e.getMessage());
            return "error";
        }
    }

    //УПРАВЛЕНИЕ КЛИНИКАМИ

    @GetMapping("/clinics")
    public String listClinics(Model model) {
        try {
            model.addAttribute("clinics", clinicService.getAll());
            return "admin/clinics";
        } catch (Exception e) {
            log.error("Ошибка загрузки списка клиник: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка загрузки клиник: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/clinics/create")
    public String createClinicForm(Model model) {
        model.addAttribute("clinicForm", new ClinicFormDTO());
        return "admin/clinic-form";
    }

    @PostMapping("/clinics/create")
    public String createClinic(@Valid @ModelAttribute ClinicFormDTO formDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMessages = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации: " + errorMessages);
            return "redirect:/admin/clinics/create";
        }

        try {
            ClinicEntity clinic = new ClinicEntity();
            clinic.setName(formDTO.getName());
            clinic.setAddress(formDTO.getAddress());
            clinic.setPhone(formDTO.getPhone());
            clinic.setWorkingHours(formDTO.getWorkingHours());
            clinic.setSiteUrl(formDTO.getSiteUrl());
            clinic.setLatitude(formDTO.getLatitude());
            clinic.setLongitude(formDTO.getLongitude());
            clinic.setWorkStart(formDTO.getWorkStart() != null ? formDTO.getWorkStart() : LocalTime.of(9, 0));
            clinic.setWorkEnd(formDTO.getWorkEnd() != null ? formDTO.getWorkEnd() : LocalTime.of(20, 0));
            clinic.setWorkDays(formDTO.getWorkDays() != null && !formDTO.getWorkDays().isEmpty()
                    ? formDTO.getWorkDays() : "MON,TUE,WED,THU,FRI");

            clinicService.create(clinic, formDTO.getLogo());
            redirectAttributes.addFlashAttribute("success", "Клиника успешно добавлена");
            log.info("Клиника создана: {}", clinic.getName());
        } catch (Exception e) {
            log.error("Ошибка создания клиники: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/clinics";
    }

    @GetMapping("/clinics/edit/{id}")
    public String editClinicForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ClinicEntity clinic = clinicService.getById(id);
            ClinicFormDTO formDTO = new ClinicFormDTO();
            formDTO.setId(clinic.getId());
            formDTO.setName(clinic.getName());
            formDTO.setAddress(clinic.getAddress());
            formDTO.setPhone(clinic.getPhone());
            formDTO.setWorkingHours(clinic.getWorkingHours());
            formDTO.setSiteUrl(clinic.getSiteUrl());
            formDTO.setLatitude(clinic.getLatitude());
            formDTO.setLongitude(clinic.getLongitude());
            formDTO.setWorkStart(clinic.getWorkStart());
            formDTO.setWorkEnd(clinic.getWorkEnd());
            formDTO.setWorkDays(clinic.getWorkDays());

            model.addAttribute("clinicForm", formDTO);
            return "admin/clinic-form";
        } catch (Exception e) {
            log.error("Ошибка загрузки формы редактирования клиники {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Клиника не найдена");
            return "redirect:/admin/clinics";
        }
    }

    @PostMapping("/clinics/edit/{id}")
    public String updateClinic(@PathVariable Long id,
                               @Valid @ModelAttribute ClinicFormDTO formDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMessages = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации: " + errorMessages);
            return "redirect:/admin/clinics/edit/" + id;
        }

        try {
            ClinicEntity clinic = new ClinicEntity();
            clinic.setName(formDTO.getName());
            clinic.setAddress(formDTO.getAddress());
            clinic.setPhone(formDTO.getPhone());
            clinic.setWorkingHours(formDTO.getWorkingHours());
            clinic.setSiteUrl(formDTO.getSiteUrl());
            clinic.setLatitude(formDTO.getLatitude());
            clinic.setLongitude(formDTO.getLongitude());
            clinic.setWorkStart(formDTO.getWorkStart());
            clinic.setWorkEnd(formDTO.getWorkEnd());
            clinic.setWorkDays(formDTO.getWorkDays());

            clinicService.update(id, clinic, formDTO.getLogo());
            redirectAttributes.addFlashAttribute("success", "Клиника успешно обновлена");
            log.info("Клиника обновлена: id={}", id);
        } catch (Exception e) {
            log.error("Ошибка обновления клиники {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/clinics";
    }

    @GetMapping("/clinics/delete/{id}")
    public String deleteClinic(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            clinicService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Клиника успешно удалена");
            log.info("Клиника удалена: id={}", id);
        } catch (Exception e) {
            log.error("Ошибка удаления клиники {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/clinics";
    }

    //УПРАВЛЕНИЕ ВРАЧАМИ

    @GetMapping("/doctors")
    public String listDoctors(Model model) {
        try {
            model.addAttribute("doctors", doctorService.getAll());
            return "admin/doctors";
        } catch (Exception e) {
            log.error("Ошибка загрузки списка врачей: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка загрузки врачей: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/doctors/create")
    public String createDoctorForm(Model model) {
        model.addAttribute("doctorForm", new DoctorFormDTO());
        model.addAttribute("allClinics", clinicService.getAll());
        model.addAttribute("selectedClinicIds", new ArrayList<>());
        return "admin/doctor-form";
    }

    @PostMapping("/doctors/create")
    public String saveDoctor(@Valid @ModelAttribute DoctorFormDTO formDTO,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMessages = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации: " + errorMessages);
            return "redirect:/admin/doctors/create";
        }

        try {
            if (formDTO.getId() != null && formDTO.getId() > 0) {
                // Обновление существующего врача
                DoctorEntity doctor = new DoctorEntity();
                doctor.setName(formDTO.getName());
                doctor.setSpecialization(formDTO.getSpecialization());
                doctor.setExperience(formDTO.getExperience());
                doctor.setEducation(formDTO.getEducation());

                doctorService.update(formDTO.getId(), doctor, formDTO.getPhoto());
                doctorService.updateDoctorClinics(formDTO.getId(), formDTO.getClinicIds());

                redirectAttributes.addFlashAttribute("success", "Врач успешно обновлен");
                log.info("Врач обновлен: id={}", formDTO.getId());
            } else {
                // Создание нового врача
                DoctorEntity doctor = new DoctorEntity();
                doctor.setName(formDTO.getName());
                doctor.setSpecialization(formDTO.getSpecialization());
                doctor.setExperience(formDTO.getExperience());
                doctor.setEducation(formDTO.getEducation());

                DoctorEntity savedDoctor = doctorService.save(doctor, formDTO.getPhoto());

                if (formDTO.getClinicIds() != null && !formDTO.getClinicIds().isEmpty()) {
                    doctorService.addDoctorToClinics(savedDoctor.getId(), formDTO.getClinicIds());
                }

                redirectAttributes.addFlashAttribute("success", "Врач успешно добавлен");
                log.info("Врач создан: {}", savedDoctor.getName());
            }
        } catch (Exception e) {
            log.error("Ошибка сохранения врача: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    @GetMapping("/doctors/edit/{id}")
    public String editDoctorForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            DoctorEntity doctor = doctorService.getById(id);
            DoctorFormDTO formDTO = new DoctorFormDTO();
            formDTO.setId(doctor.getId());
            formDTO.setName(doctor.getName());
            formDTO.setSpecialization(doctor.getSpecialization());
            formDTO.setExperience(doctor.getExperience());
            formDTO.setEducation(doctor.getEducation());

            List<Long> selectedClinicIds = doctor.getClinics().stream()
                    .map(ClinicEntity::getId)
                    .collect(Collectors.toList());

            model.addAttribute("doctorForm", formDTO);
            model.addAttribute("allClinics", clinicService.getAll());
            model.addAttribute("selectedClinicIds", selectedClinicIds);

            return "admin/doctor-form";
        } catch (Exception e) {
            log.error("Ошибка загрузки формы редактирования врача {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Врач не найден");
            return "redirect:/admin/doctors";
        }
    }

    @GetMapping("/doctors/delete/{id}")
    public String deleteDoctor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            doctorService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Врач успешно удален");
            log.info("Врач удален: id={}", id);
        } catch (Exception e) {
            log.error("Ошибка удаления врача {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    //  УПРАВЛЕНИЕ АДМИНАМИ КЛИНИК

    @GetMapping("/create-clinic-admin")
    public String createClinicAdminForm(Model model, @RequestParam(required = false) Long clinicId) {
        model.addAttribute("clinics", clinicService.getAll());
        model.addAttribute("registrationDTO", new RegistrationDTO());
        model.addAttribute("selectedClinicId", clinicId);
        return "admin/create-clinic-admin";
    }

    @PostMapping("/create-clinic-admin")
    public String createClinicAdmin(@Valid @ModelAttribute RegistrationDTO dto,
                                    BindingResult bindingResult,
                                    @RequestParam Long clinicId,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации: " + bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/create-clinic-admin";
        }

        try {
            ClinicEntity clinic = clinicService.getById(clinicId);

            // Регистрируем нового пользователя с ролью CLINIC_ADMIN
            UserEntity admin = userService.register(
                    dto.getName(),
                    dto.getEmail(),
                    dto.getPassword(),
                    UserEntity.Role.CLINIC_ADMIN
            );

            // Привязываем админа к клинике
            admin.setManagedClinic(clinic);

            // Сохраняем обновленного админа
            UserEntity savedAdmin = userService.save(admin);

            redirectAttributes.addFlashAttribute("success",
                    "Админ клиники \"" + clinic.getName() + "\" успешно создан");
            log.info("Создан админ для клиники: {} (ID: {})", clinic.getName(), savedAdmin.getId());

        } catch (Exception e) {
            log.error("Ошибка создания админа клиники: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/clinics";
    }
}