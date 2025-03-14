package com.example.artshop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ArtNotFoundException extends RuntimeException {
    public ArtNotFoundException(String message) {
        super(message);
    }
}