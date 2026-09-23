package com.linkedin.backend.features.authentication.dto;

import com.linkedin.backend.features.authentication.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecruiterRegistrationDto(
        @NotBlank(message = "Email is mandatory")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Password is mandatory")
        @StrongPassword
        String password,

        @NotBlank(message = "First name is mandatory")
        String firstName,

        @NotBlank(message = "Last name is mandatory")
        String lastName,

        String position,
        String location,

        @NotBlank(message = "Company name is mandatory")
        String companyName,

        @Email(message = "Company email should be valid")
        String companyEmail,

        String companyWebsite,
        String companyDescription,
        String companyLocation
) {
}
