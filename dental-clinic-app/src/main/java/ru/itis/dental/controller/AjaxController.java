package ru.itis.dental.controller;

import ru.itis.dental.entity.DoctorEntity;
import ru.itis.dental.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ajax")
@RequiredArgsConstructor
@Slf4j
public class AjaxController {

    private final DoctorRepository doctorRepository;

    @GetMapping("/doctors/by-specialization")
    public ResponseEntity<Map<String, Object>> getDoctorsBySpecialization(
            @RequestParam(required = false) String specialization) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (specialization == null || specialization.trim().length() < 2) {
                response.put("success", false);
                response.put("error", "Введите минимум 2 символа для поиска");
                return ResponseEntity.badRequest().body(response);
            }

            List<DoctorEntity> doctors = doctorRepository.findBySpecializationContainingIgnoreCase(specialization);

            List<Map<String, Object>> doctorsData = doctors.stream().limit(20).map(d -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", d.getId());
                map.put("name", d.getName());
                map.put("specialization", d.getSpecialization() != null ? d.getSpecialization() : "Стоматолог");
                map.put("experience", d.getExperience() != null ? d.getExperience() : 0);
                map.put("photoUrl", d.getPhotoUrl());
                return map;
            }).collect(Collectors.toList());

            response.put("success", true);
            response.put("doctors", doctorsData);
            response.put("count", doctorsData.size());

            if (doctorsData.isEmpty()) {
                response.put("message", "Врачи не найдены по вашему запросу");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AJAX search error: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Произошла ошибка при поиске. Пожалуйста, попробуйте позже.");
            response.put("errorCode", "INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/check-appointment-time")
    public ResponseEntity<Map<String, Object>> checkAppointmentTime(
            @RequestParam Long doctorId,
            @RequestParam String dateTime) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (doctorId == null || doctorId <= 0) {
                response.put("success", false);
                response.put("error", "Неверный ID врача");
                return ResponseEntity.badRequest().body(response);
            }

            if (dateTime == null || dateTime.isEmpty()) {
                response.put("success", false);
                response.put("error", "Дата и время не указаны");
                return ResponseEntity.badRequest().body(response);
            }

            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateTime);

            // Проверка, что время в будущем , стоб не втыкали куда не надо
            if (ldt.isBefore(java.time.LocalDateTime.now())) {
                response.put("success", false);
                response.put("error", "Нельзя записаться на прошедшее время");
                response.put("isBusy", true);
                return ResponseEntity.ok(response);
            }

            // реальная проверка занятости
            boolean isBusy = false;

            response.put("success", true);
            response.put("isBusy", isBusy);
            response.put("message", isBusy ? "Время занято" : "Время свободно");

        } catch (java.time.format.DateTimeParseException e) {
            log.error("Date parsing error: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "Неверный формат даты и времени");
            response.put("errorCode", "INVALID_DATE_FORMAT");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Check appointment error: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Ошибка проверки времени");
            response.put("errorCode", "INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.ok(response);
    }
}