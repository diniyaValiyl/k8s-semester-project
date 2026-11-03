package ru.itis.dental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clinics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String phone;

    @Column(name = "hours")
    private String workingHours;

    @Column(name = "site_url")
    private String siteUrl;

    @Column(name = "latitude", columnDefinition = "FLOAT8")
    private Double latitude;

    @Column(name = "longitude", columnDefinition = "FLOAT8")
    private Double longitude;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "work_start")
    private LocalTime workStart;

    @Column(name = "work_end")
    private LocalTime workEnd;

    @Column(name = "work_days")
    private String workDays;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "clinic_doctors",
            joinColumns = @JoinColumn(name = "clinic_id"),
            inverseJoinColumns = @JoinColumn(name = "doctor_id")
    )
    @Builder.Default
    private List<DoctorEntity> doctors = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "clinic_services",
            joinColumns = @JoinColumn(name = "clinic_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private List<DentalServiceEntity> services = new ArrayList<>();

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReviewEntity> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "clinic", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AppointmentEntity> appointments = new ArrayList<>();

}