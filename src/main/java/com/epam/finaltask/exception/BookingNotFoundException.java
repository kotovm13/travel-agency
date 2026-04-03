package com.epam.finaltask.exception;

public class BookingNotFoundException extends ResourceNotFoundException {
    public BookingNotFoundException(String message) {
        super(message);
    }
}
