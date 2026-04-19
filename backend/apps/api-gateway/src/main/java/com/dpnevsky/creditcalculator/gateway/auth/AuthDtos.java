package com.dpnevsky.creditcalculator.gateway.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(max = 64) String firstName,
            @NotBlank @Size(max = 64) String lastName,
            @Size(max = 64) String middleName,
            @PastOrPresent @JsonFormat(pattern = "yyyy-MM-dd") LocalDate birthDate
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record LogoutRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType
    ) {
    }

    public record CurrentUserResponse(
            String id,
            String email,
            String firstName,
            String lastName,
            String middleName,
            String birthDate,
            List<String> roles
    ) {
    }
}
