package com.metalworkshop.auth_service.exceptions;


import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<Map<String, String>> jwtSignatureException(SignatureException ex) {
        logger.warn("Invalid JWT signature exception: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        errors.put("message", "Invalid JWT Signature.");

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, String>> jwtException(JwtException ex) {
        logger.warn("Invalid JWT tokern exception: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        errors.put("message", "Invalid JWT Token.");

        return ResponseEntity.badRequest().body(errors);
    }
}
