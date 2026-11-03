package ru.itis.dental.controller;

import ru.itis.dental.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
@Slf4j
public class AvailableSlotsController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam Long clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<LocalDateTime> availableSlots;

            if (authentication != null && authentication.isAuthenticated()) {
                availableSlots = appointmentService.getAvailableTimeSlots(doctorId, clinicId, date);
            } else {
                availableSlots = appointmentService.getAvailableTimeSlotsForGuest(doctorId, clinicId, date);
            }

            List<String> formattedSlots = availableSlots.stream()
                    .map(slot -> slot.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("slots", formattedSlots);
            response.put("count", formattedSlots.size());

        } catch (Exception e) {
            log.error("Ошибка получения слотов: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}