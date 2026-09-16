package ru.itis.dental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_clinic_id")
    private ClinicEntity managedClinic;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AppointmentEntity> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ReviewEntity> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MedicalRecordEntity> medicalRecords = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "favorite_clinics",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "clinic_id"))
    @Builder.Default
    private List<ClinicEntity> favoriteClinics = new ArrayList<>();

    public enum Role {
        PATIENT, ADMIN, CLINIC_ADMIN
    }
}