package io.github.Romariok.orkestro.utils.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

      @ExceptionHandler(EntityNotFoundException.class)
      public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
                  EntityNotFoundException ex, HttpServletRequest request) {
            return buildResponse(
                        HttpStatus.NOT_FOUND,
                        "Entity not found",
                        ex.getMessage(),
                        request.getRequestURI(),
                        Collections.emptyList());
      }

      @ExceptionHandler(BusinessException.class)
      public ResponseEntity<ApiErrorResponse> handleBusinessException(
                  BusinessException ex, HttpServletRequest request) {
            return buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Business rule violation",
                        ex.getMessage(),
                        request.getRequestURI(),
                        Collections.emptyList());
      }

      @ExceptionHandler(InternalServiceException.class)
      public ResponseEntity<ApiErrorResponse> handleInternalServiceException(
                  InternalServiceException ex, HttpServletRequest request) {
            return buildResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal service error",
                        ex.getMessage(),
                        request.getRequestURI(),
                        Collections.emptyList());
      }

      @ExceptionHandler(MethodArgumentNotValidException.class)
      public ResponseEntity<ApiErrorResponse> handleValidationException(
                  MethodArgumentNotValidException ex, HttpServletRequest request) {
            List<String> details = ex.getBindingResult().getFieldErrors().stream()
                        .map(this::formatFieldError)
                        .toList();

            return buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        "One or more fields are invalid",
                        request.getRequestURI(),
                        details);
      }

      @ExceptionHandler(BindException.class)
      public ResponseEntity<ApiErrorResponse> handleBindException(
                  BindException ex, HttpServletRequest request) {
            List<String> details = ex.getBindingResult().getFieldErrors().stream()
                        .map(this::formatFieldError)
                        .toList();

            return buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        "One or more fields are invalid",
                        request.getRequestURI(),
                        details);
      }

      @ExceptionHandler(MethodArgumentTypeMismatchException.class)
      public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
                  MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
            String detail = ex.getName() + ": invalid value";
            return buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        detail,
                        request.getRequestURI(),
                        List.of(detail));
      }

      @ExceptionHandler(ConstraintViolationException.class)
      public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
                  ConstraintViolationException ex, HttpServletRequest request) {
            List<String> details = ex.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .toList();

            return buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        "One or more fields are invalid",
                        request.getRequestURI(),
                        details);
      }

      @ExceptionHandler(IllegalArgumentException.class)
      public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
                  IllegalArgumentException ex, HttpServletRequest request) {
            return buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        ex.getMessage(),
                        request.getRequestURI(),
                        Collections.emptyList());
      }

      @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
      public ResponseEntity<ApiErrorResponse> handleMultipartException(
                  Exception ex, HttpServletRequest request) {
            return buildResponse(
                        HttpStatus.valueOf(413),
                        "Validation failed",
                        "Uploaded file is too large (max 30MB)",
                        request.getRequestURI(),
                        Collections.emptyList());
      }

      @ExceptionHandler(BadCredentialsException.class)
      public ResponseEntity<ApiErrorResponse> handleBadCredentials(
                  BadCredentialsException ex, HttpServletRequest request) {
            return buildResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication failed",
                        ex.getMessage(),
                        request.getRequestURI(),
                        Collections.emptyList());
      }

      @ExceptionHandler(AccessDeniedException.class)
      public ResponseEntity<ApiErrorResponse> handleAccessDenied(
                  AccessDeniedException ex, HttpServletRequest request) {
            return buildResponse(
                        HttpStatus.FORBIDDEN,
                        "Access denied",
                        ex.getMessage(),
                        request.getRequestURI(),
                        Collections.emptyList());
      }

      @ExceptionHandler(Exception.class)
      public ResponseEntity<ApiErrorResponse> handleGeneric(
                  Exception ex, HttpServletRequest request) {
            return buildResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unexpected error",
                        ex.getMessage(),
                        request.getRequestURI(),
                        Collections.emptyList());
      }

      private ResponseEntity<ApiErrorResponse> buildResponse(
                  HttpStatus status,
                  String error,
                  String message,
                  String path,
                  List<String> details) {
            ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(), error, message, path, details);
            return ResponseEntity.status(status).body(body);
      }

      private String formatFieldError(FieldError fieldError) {
            return fieldError.getField() + ": " + fieldError.getDefaultMessage();
      }
}
