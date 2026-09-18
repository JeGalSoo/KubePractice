package com.example.reservation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatAlreadyHeldException.class)
    public ResponseEntity<Map<String, Object>> handleSeatAlreadyHeld(SeatAlreadyHeldException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(409, e.getMessage()));
    }

    @ExceptionHandler(ConcertException.class)
    public ResponseEntity<Map<String, Object>> handleConcertException(ConcertException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(400, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "서버 오류가 발생했습니다."));
    }

    private Map<String, Object> errorBody(int code, String message) {
        return Map.of(
                "code", code,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
