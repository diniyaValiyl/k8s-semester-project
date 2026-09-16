package ru.itis.dental.service;

import ru.itis.dental.entity.AppointmentEntity;
import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.entity.DoctorEntity;
import ru.itis.dental.entity.UserEntity;
import ru.itis.dental.repository.AppointmentRepository;
import ru.itis.dental.repository.ClinicRepository;
import ru.itis.dental.repository.DoctorRepository;
import ru.itis.dental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;

    private static final int APPOINTMENT_HOURS_BEFORE = 1;

    @Transactional
    public AppointmentEntity createAppointment(Long patientId, Long doctorId, Long clinicId, LocalDateTime dateTime) {
        log.info("Создание записи: пациент {}, врач {}, клиника {}, время {}", patientId, doctorId, clinicId, dateTime);

        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Нельзя записаться на прошедшее время");
        }

        if (dateTime.isBefore(LocalDateTime.now().plusHours(APPOINTMENT_HOURS_BEFORE))) {
            throw new RuntimeException("Запись возможна не менее чем за 1 час до приема");
        }

        UserEntity patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Пациент не найден"));
        DoctorEntity doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Врач не найден"));
        ClinicEntity clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new RuntimeException("Клиника не найдена"));

        // Проверка рабочих часов клиники
        LocalTime appointmentTime = dateTime.toLocalTime();
        LocalTime workStart = clinic.getWorkStart() != null ? clinic.getWorkStart() : LocalTime.of(9, 0);
        LocalTime workEnd = clinic.getWorkEnd() != null ? clinic.getWorkEnd() : LocalTime.of(20, 0);

        if (appointmentTime.isBefore(workStart) || appointmentTime.isAfter(workEnd.minusMinutes(1))) {
            throw new RuntimeException("Клиника работает с " + workStart + " до " + workEnd + ". Выберите другое время");
        }

        // Проверка рабочих дней
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        String workDays = clinic.getWorkDays() != null ? clinic.getWorkDays() : "MON,TUE,WED,THU,FRI";
        List<String> workingDaysList = Arrays.asList(workDays.split(","));

        String dayAbbreviation = getDayAbbreviation(dayOfWeek);
        if (!workingDaysList.contains(dayAbbreviation)) {
            throw new RuntimeException("Клиника не работает в " + getRussianDayName(dayOfWeek) + ". Выберите другой день");
        }

        // Проверка: только каждый час
        if (appointmentTime.getMinute() != 0) {
            throw new RuntimeException("Запись возможна только каждый час (например, 09:00, 10:00, 11:00)");
        }

        boolean doctorAlreadyBooked = appointmentRepository.existsByDoctorIdAndDateTime(doctorId, dateTime);
        if (doctorAlreadyBooked) {
            throw new RuntimeException("Это время уже занято у выбранного врача");
        }

        boolean patientAlreadyBooked = appointmentRepository.existsByPatientIdAndDateTime(patientId, dateTime);
        if (patientAlreadyBooked) {
            throw new RuntimeException("У вас уже есть запись на это время");
        }

        boolean doctorWorksInClinic = doctorRepository.existsByIdAndClinicsId(doctorId, clinicId);
        if (!doctorWorksInClinic) {
            throw new RuntimeException("Этот врач не работает в выбранной клинике");
        }

        AppointmentEntity appointment = AppointmentEntity.builder()
                .patient(patient)
                .doctor(doctor)
                .clinic(clinic)
                .dateTime(dateTime)
                .status(AppointmentEntity.Status.PENDING)
                .build();

        return appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableTimeSlots(Long doctorId, Long clinicId, LocalDate date) {
        List<LocalDateTime> allSlots = new ArrayList<>();

        ClinicEntity clinic = clinicRepository.findById(clinicId).orElse(null);
        if (clinic == null) {
            return allSlots;
        }

        LocalTime workStart = clinic.getWorkStart() != null ? clinic.getWorkStart() : LocalTime.of(9, 0);
        LocalTime workEnd = clinic.getWorkEnd() != null ? clinic.getWorkEnd() : LocalTime.of(20, 0);

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String workDays = clinic.getWorkDays() != null ? clinic.getWorkDays() : "MON,TUE,WED,THU,FRI";
        List<String> workingDaysList = Arrays.asList(workDays.split(","));
        String dayAbbreviation = getDayAbbreviation(dayOfWeek);

        if (!workingDaysList.contains(dayAbbreviation)) {
            return allSlots;
        }

        for (int hour = workStart.getHour(); hour <= workEnd.getHour() - 1; hour++) {
            LocalDateTime slot = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slot.isAfter(LocalDateTime.now().plusHours(APPOINTMENT_HOURS_BEFORE))) {
                allSlots.add(slot);
            }
        }

        List<LocalDateTime> bookedSlots = appointmentRepository.findBusyTimeSlotsByDoctorId(doctorId,
                date.atStartOfDay(), date.atTime(23, 59, 59));

        allSlots.removeAll(bookedSlots);

        return allSlots;
    }

    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableTimeSlotsForGuest(Long doctorId, Long clinicId, LocalDate date) {
        return getAvailableTimeSlots(doctorId, clinicId, date);
    }

    private String getDayAbbreviation(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }

    private String getRussianDayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "понедельник";
            case TUESDAY -> "вторник";
            case WEDNESDAY -> "среда";
            case THURSDAY -> "четверг";
            case FRIDAY -> "пятница";
            case SATURDAY -> "субботу";
            case SUNDAY -> "воскресенье";
        };
    }

    @Transactional
    public void updateAppointmentStatus(Long appointmentId, AppointmentEntity.Status status) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentEntity> getUserAppointments(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return appointmentRepository.findByPatientOrderByDateTimeDesc(user);
    }

    @Transactional(readOnly = true)
    public AppointmentEntity getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));
    }
}