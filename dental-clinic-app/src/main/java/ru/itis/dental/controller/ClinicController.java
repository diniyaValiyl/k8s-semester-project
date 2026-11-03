package ru.itis.dental.controller;

import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.service.ClinicService;
import ru.itis.dental.service.ReviewService;
import ru.itis.dental.repository.ClinicServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/clinics")
@RequiredArgsConstructor
@Slf4j
public class ClinicController {

    private final ClinicService clinicService;
    private final ReviewService reviewService;
    private final ClinicServiceRepository clinicServiceRepository;

    @GetMapping
    public String listClinics(Model model) {
        model.addAttribute("clinics", clinicService.getAll());
        return "clinic/list";
    }

    @GetMapping("/{id}")
    public String clinicDetail(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            ClinicEntity clinic = clinicService.getById(id);
            model.addAttribute("clinic", clinic);
            model.addAttribute("avgRating", reviewService.getClinicAverageRating(id));
            model.addAttribute("reviews", reviewService.getClinicReviews(id));
            model.addAttribute("services", clinicServiceRepository.findWithPricesByClinicId(id));

            if (authentication != null && authentication.isAuthenticated()) {
                model.addAttribute("userEmail", authentication.getName());
            }

            return "clinic/detail";
        } catch (Exception e) {
            log.error("Ошибка загрузки клиники {}: {}", id, e.getMessage());
            model.addAttribute("error", "Клиника не найдена");
            return "error";
        }
    }

    @GetMapping("/search")
    public String search(@RequestParam String query, Model model) {
        List<ClinicEntity> clinics = clinicService.search(query);
        model.addAttribute("clinics", clinics);
        model.addAttribute("query", query);
        return "clinic/list";
    }

    @GetMapping("/map")
    public String mapView(Model model) {
        List<ClinicEntity> clinics = clinicService.getAll();
        model.addAttribute("clinics", clinics);
        return "clinic/map";
    }

    @GetMapping("/top-rated")
    public String topRated(Model model) {
        model.addAttribute("clinics", clinicService.getTopRated());
        return "clinic/list";
    }
}