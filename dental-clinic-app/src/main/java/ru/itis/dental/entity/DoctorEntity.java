package ru.itis.dental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String specialization;

    private Integer experience;

    @Column(columnDefinition = "TEXT")
    private String education;

    @Column(name = "photo_url")
    private String photoUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "clinic_doctors",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "clinic_id")
    )
    @Builder.Default
    private List<ClinicEntity> clinics = new ArrayList<>();

    @OneToMany(mappedBy = "doctor")
    @Builder.Default
    private List<AppointmentEntity> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "doctor")
    @Builder.Default
    private List<MedicalRecordEntity> medicalRecords = new ArrayList<>();
}