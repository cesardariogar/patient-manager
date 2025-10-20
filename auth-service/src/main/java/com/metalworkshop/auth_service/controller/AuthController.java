package com.metalworkshop.auth_service.controller;

import com.metalworkshop.auth_service.dto.LoginRequestDto;
import com.metalworkshop.auth_service.dto.LoginResponseDto;
import com.metalworkshop.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Generate token on user login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginRequestDto requestDto) {

        System.out.println(">>>>>>>>>>>>>>>>> request: " + requestDto.toString());
        Optional<String> tokenOptional = authService.authenticate(requestDto);

        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = tokenOptional.get();
        System.out.println(">>>>>>>>>>>>>>>>> token: " + token);
        return ResponseEntity.ok(new LoginResponseDto(token));
    }
}
