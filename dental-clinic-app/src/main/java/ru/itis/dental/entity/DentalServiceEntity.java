package ru.itis.dental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dental_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DentalServiceEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;

    @ManyToMany(mappedBy = "services", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ClinicEntity> clinics = new ArrayList<>();
}