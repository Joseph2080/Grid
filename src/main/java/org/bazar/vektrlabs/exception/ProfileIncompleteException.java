package org.bazar.vektrlabs.exception;

import org.springframework.security.access.AccessDeniedException;

public class ProfileIncompleteException extends AccessDeniedException {
    public ProfileIncompleteException(String message) {
        super(message);
    }
}
