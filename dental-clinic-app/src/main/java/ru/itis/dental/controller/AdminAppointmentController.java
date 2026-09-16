package ru.itis.dental.controller;

import ru.itis.dental.entity.AppointmentEntity;
import ru.itis.dental.repository.AppointmentRepository;
import ru.itis.dental.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/appointments")
@RequiredArgsConstructor
@Slf4j
public class AdminAppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;

    @GetMapping
    public String listAppointments(Model model,
                                   @RequestParam(required = false) Long clinicId,
                                   @RequestParam(required = false) String status) {

        List<AppointmentEntity> appointments = appointmentRepository.findAll();

        if (clinicId != null && clinicId > 0) {
            appointments = appointments.stream()
                    .filter(a -> a.getClinic().getId().equals(clinicId))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isEmpty()) {
            appointments = appointments.stream()
                    .filter(a -> a.getStatus().name().equals(status))
                    .collect(Collectors.toList());
        }

        appointments.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));

        model.addAttribute("appointments", appointments);
        model.addAttribute("clinics", clinicRepository.findAll());
        model.addAttribute("selectedClinicId", clinicId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", AppointmentEntity.Status.values());

        return "admin/appointments";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            AppointmentEntity appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Запись не найдена"));

            AppointmentEntity.Status newStatus = AppointmentEntity.Status.valueOf(status);
            appointment.setStatus(newStatus);
            appointmentRepository.save(appointment);

            redirectAttributes.addFlashAttribute("success", "Статус записи обновлен");
        } catch (Exception e) {
            log.error("Ошибка обновления статуса: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/admin/appointments";
    }
}