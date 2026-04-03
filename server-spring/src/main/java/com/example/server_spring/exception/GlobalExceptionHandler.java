package com.example.server_spring.exception;

import com.futurecity.shared.packets.resonse.LoginResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<LoginResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        LoginResponse resp = new LoginResponse();
        resp.status = "FAIL";
        resp.message = ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "FAIL");
        response.put("message", "Database constraint violation occurred.");
        // Log the actual error for debugging
        System.err.println("[GlobalExceptionHandler] DataIntegrityViolationException: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "FAIL");
        response.put("message", "An unexpected error occurred.");
        System.err.println("[GlobalExceptionHandler] Unexpected Exception: " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
