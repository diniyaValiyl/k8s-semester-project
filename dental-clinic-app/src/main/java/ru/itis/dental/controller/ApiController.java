package ru.itis.dental.controller;

import ru.itis.dental.entity.ClinicEntity;
import ru.itis.dental.service.ClinicService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinics API", description = "API для работы со стоматологическими клиниками")
public class ApiController {

    private final ClinicService clinicService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/clinics")
    public ResponseEntity<List<Map<String, Object>>> getAllClinics() {
        List<ClinicEntity> clinics = clinicService.getAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (ClinicEntity clinic : clinics) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", clinic.getId());
            map.put("name", clinic.getName());
            map.put("address", clinic.getAddress());
            map.put("latitude", clinic.getLatitude());
            map.put("longitude", clinic.getLongitude());
            map.put("phone", clinic.getPhone());
            map.put("workingHours", clinic.getWorkingHours());
            map.put("logoUrl", clinic.getLogoUrl());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/clinics/{id}")
    public ResponseEntity<Map<String, Object>> getClinic(@PathVariable Long id) {
        ClinicEntity clinic = clinicService.getById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("id", clinic.getId());
        map.put("name", clinic.getName());
        map.put("address", clinic.getAddress());
        map.put("latitude", clinic.getLatitude());
        map.put("longitude", clinic.getLongitude());
        map.put("phone", clinic.getPhone());
        map.put("workingHours", clinic.getWorkingHours());
        map.put("logoUrl", clinic.getLogoUrl());
        map.put("siteUrl", clinic.getSiteUrl());
        return ResponseEntity.ok(map);
    }

    @GetMapping("/distance")
    public ResponseEntity<Map<String, Object>> getDistance(
            @RequestParam double userLat,
            @RequestParam double userLon,
            @RequestParam Long clinicId,
            @RequestParam(defaultValue = "foot") String mode) {

        ClinicEntity clinic = clinicService.getById(clinicId);

        if (clinic.getLatitude() == null || clinic.getLongitude() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "У клиники не указаны координаты");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result;

        if (mode.equals("driving")) {
            result = getDrivingDistance(userLat, userLon, clinic.getLatitude().doubleValue(), clinic.getLongitude().doubleValue());
        } else {
            result = getWalkingDistance(userLat, userLon, clinic.getLatitude().doubleValue(), clinic.getLongitude().doubleValue());
        }

        result.put("clinic_id", clinic.getId());
        result.put("clinic_name", clinic.getName());
        result.put("clinic_address", clinic.getAddress());
        result.put("mode", mode);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/distance/all")
    public ResponseEntity<List<Map<String, Object>>> getAllDistances(
            @RequestParam double userLat,
            @RequestParam double userLon,
            @RequestParam(defaultValue = "foot") String mode) {

        List<ClinicEntity> clinics = clinicService.getAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (ClinicEntity clinic : clinics) {
            if (clinic.getLatitude() != null && clinic.getLongitude() != null) {
                Map<String, Object> dist;

                if (mode.equals("driving")) {
                    dist = getDrivingDistance(userLat, userLon, clinic.getLatitude().doubleValue(), clinic.getLongitude().doubleValue());
                } else {
                    dist = getWalkingDistance(userLat, userLon, clinic.getLatitude().doubleValue(), clinic.getLongitude().doubleValue());
                }

                dist.put("clinic_id", clinic.getId());
                dist.put("clinic_name", clinic.getName());
                dist.put("clinic_address", clinic.getAddress());
                dist.put("latitude", clinic.getLatitude());
                dist.put("longitude", clinic.getLongitude());
                results.add(dist);
            }
        }

        results.sort(Comparator.comparingDouble(d -> {
            Object distance = d.get("distance");
            return distance instanceof Double ? (Double) distance : Double.MAX_VALUE;
        }));

        return ResponseEntity.ok(results);
    }

    private Map<String, Object> getDrivingDistance(double userLat, double userLon, double clinicLat, double clinicLon) {
        Map<String, Object> result = new HashMap<>();

        try {
            String url = "https://router.project-osrm.org/route/v1/driving/" +
                    userLon + "," + userLat + ";" +
                    clinicLon + "," + clinicLat + "?overview=false";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonNode root = objectMapper.readTree(response.toString());
                if (root.path("code").asText().equals("Ok") && root.path("routes").size() > 0) {
                    JsonNode route = root.path("routes").get(0);
                    double distanceKm = route.path("distance").asDouble() / 1000;
                    double durationMin = route.path("duration").asDouble() / 60;

                    result.put("success", true);
                    result.put("distance", Math.round(distanceKm * 10) / 10.0);
                    result.put("duration", Math.round(durationMin));
                    result.put("method", "osrm_driving");
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("OSRM driving error: {}", e.getMessage());
        }

        double distance = calculateStraightDistance(userLat, userLon, clinicLat, clinicLon);
        result.put("success", true);
        result.put("distance", Math.round(distance * 10) / 10.0);
        result.put("duration", Math.round(distance * 3));
        result.put("method", "straight_line");
        result.put("warning", "Автомобильный маршрут временно недоступен");
        return result;
    }

    private Map<String, Object> getWalkingDistance(double userLat, double userLon, double clinicLat, double clinicLon) {
        Map<String, Object> result = new HashMap<>();

        double distance = calculateStraightDistance(userLat, userLon, clinicLat, clinicLon);
        double walkingSpeedKmh = 5.0;
        double durationMin = (distance / walkingSpeedKmh) * 60;

        result.put("success", true);
        result.put("distance", Math.round(distance * 10) / 10.0);
        result.put("duration", Math.round(durationMin));
        result.put("method", "walking_straight");
        return result;
    }

    private double calculateStraightDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "API работает");
        return ResponseEntity.ok(response);
    }
}