package com.udacity.vehicles.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Provide valid price value")
public class InvalidPriceException extends RuntimeException {

    public InvalidPriceException() {
    }

    public InvalidPriceException(String message) {
        super(message);
    }
}
