package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.Romariok.orkestro.user.dto.RegisterRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RegisterRequestDTOValidationTest {

   private static Validator buildValidator() {
      ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
      return factory.getValidator();
   }

   @Test
   void validate_locationTooLong_hasViolation() {
      RegisterRequestDTO dto = new RegisterRequestDTO();
      dto.setUsername("user123");
      dto.setPassword("password123");
      dto.setName("User Name");
      dto.setEmail("user@example.com");
      dto.setLocation("a".repeat(256));

      var violations = buildValidator().validate(dto);
      assertTrue(
            violations.stream().anyMatch(v -> "location".equals(v.getPropertyPath().toString())),
            "Expected validation violation for location");
   }

   @Test
   void validate_birthDateInFuture_hasViolation() {
      RegisterRequestDTO dto = new RegisterRequestDTO();
      dto.setUsername("user123");
      dto.setPassword("password123");
      dto.setName("User Name");
      dto.setEmail("user@example.com");
      dto.setBirthDate(LocalDate.now().plusDays(1));

      var violations = buildValidator().validate(dto);
      assertTrue(
            violations.stream().anyMatch(v -> "birthDate".equals(v.getPropertyPath().toString())),
            "Expected validation violation for birthDate");
   }
}
