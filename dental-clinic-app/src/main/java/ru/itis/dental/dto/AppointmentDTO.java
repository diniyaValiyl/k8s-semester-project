package ru.itis.dental.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class AppointmentDTO {
    @NotNull(message = "Пожалуйста, выберите врача")
    private Long doctorId;

    @NotNull(message = "Пожалуйста, выберите клинику")
    private Long clinicId;

    @NotNull(message = "Пожалуйста, выберите дату и время")
    @Future(message = "Дата и время должны быть в будущем")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dateTime;
}