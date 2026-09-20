package org.bazar.vektrlabs.exception;

import lombok.extern.slf4j.Slf4j;
import org.bazar.vektrlabs.config.SecurityErrorResponses;
import org.bazar.vektrlabs.config.SecurityToolExecutionExceptionProcessor;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.exception.EntityReferenceException;
import org.jericho.common.exception.InvalidParameterException;
import org.jericho.common.util.RestUtil;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ToolExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleToolExecutionException(ToolExecutionException ex) {
        Throwable cause = SecurityToolExecutionExceptionProcessor.unwrap(ex);
        if (cause instanceof ProfileNotFoundException missing) {
            return handleProfileNotFoundException(missing);
        }
        if (cause instanceof ProfileIncompleteException incomplete) {
            return handleProfileIncompleteException(incomplete);
        }
        if (cause instanceof AuthenticationException authentication) {
            return handleAuthenticationException(authentication);
        }
        if (cause instanceof AccessDeniedException denied) {
            return handleAccessDeniedException(denied);
        }
        return handleGenericException(ex);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProfileNotFoundException(ProfileNotFoundException ex) {
        return SecurityErrorResponses.response(HttpStatus.NOT_FOUND,
                "PROFILE_NOT_FOUND", "Complete your profile to continue.");
    }

    @ExceptionHandler(ProfileIncompleteException.class)
    public ResponseEntity<Map<String, Object>> handleProfileIncompleteException(ProfileIncompleteException ex) {
        return SecurityErrorResponses.response(HttpStatus.FORBIDDEN,
                "PROFILE_INCOMPLETE", "Complete your first and last name to continue.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.error("Entity not found: {}", ex.getMessage());
        return RestUtil.handleException(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // Thrown by CurrentUserUtil when a gated action (cart, checkout, store
    // creation) is attempted without an authenticated session.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication required: {}", ex.getMessage());
        return SecurityErrorResponses.response(HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Authentication required.");
    }

    // Thrown by CartService when a cart operation is attempted on a cart
    // that does not belong to the signed-in user.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return SecurityErrorResponses.response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied.");
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidParameterException(InvalidParameterException ex) {
        log.error("Invalid parameter: {}", ex.getMessage());
        return RestUtil.handleException(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        return RestUtil.handleException(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityReferenceException.class)
    public ResponseEntity<Map<String, Object>> handleEntityReferenceException(EntityReferenceException ex) {
        log.error("Entity reference error: {}", ex.getMessage());
        return RestUtil.handleException(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);

        return RestUtil.handleException("Validation failed", HttpStatus.BAD_REQUEST, errors);
    }

    // remove later, chrome was trying to fetch some config!
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(
            NoResourceFoundException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);
        return RestUtil.handleException("An unexpected error occurred. Please try again later.", 
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
