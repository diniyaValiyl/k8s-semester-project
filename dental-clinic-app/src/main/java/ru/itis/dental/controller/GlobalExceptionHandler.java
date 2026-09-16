package ru.itis.dental.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Object handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("Ошибка: {}", ex.getMessage(), ex);

        String ajaxHeader = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(ajaxHeader)) {
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("error", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        String ajaxHeader = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(ajaxHeader)) {
            return ResponseEntity.badRequest().body(errors);
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("error", "Ошибка валидации: " + errors.toString());
        return mav;
    }
}