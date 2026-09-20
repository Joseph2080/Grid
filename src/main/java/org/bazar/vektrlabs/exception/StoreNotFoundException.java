package org.bazar.vektrlabs.exception;

import org.jericho.common.exception.EntityNotFoundException;

public class StoreNotFoundException extends EntityNotFoundException {
    
    public StoreNotFoundException(String message) {
        super(message);
    }
}