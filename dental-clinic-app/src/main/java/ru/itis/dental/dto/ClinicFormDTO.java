package ru.itis.dental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;

@Data
public class ClinicFormDTO {
    private Long id;

    @NotBlank(message = "Название клиники обязательно")
    @Size(min = 2, max = 200, message = "Название от 2 до 200 символов")
    private String name;

    @NotBlank(message = "Адрес обязателен")
    @Size(min = 5, max = 300, message = "Адрес от 5 до 300 символов")
    private String address;

    private String phone;
    private String workingHours;
    private String siteUrl;

    private Double latitude;
    private Double longitude;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime workStart;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime workEnd;

    private String workDays;

    private MultipartFile logo;
}