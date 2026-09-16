package ru.itis.dental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class DoctorFormDTO {
    private Long id;

    @NotBlank(message = "ФИО врача обязательно")
    @Size(min = 2, max = 150, message = "ФИО от 2 до 150 символов")
    private String name;

    private String specialization;
    private Integer experience;
    private String education;
    private MultipartFile photo;
    private List<Long> clinicIds;
}