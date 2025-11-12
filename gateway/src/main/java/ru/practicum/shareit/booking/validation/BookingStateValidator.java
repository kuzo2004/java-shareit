package ru.practicum.shareit.booking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.dto.BookingStateParam;

@Component
public class BookingStateValidator implements ConstraintValidator<ValidBookingState, String> {


    @Override
    public void initialize(ValidBookingState constraintAnnotation) {
    }

    @Override
    public boolean isValid(String state, ConstraintValidatorContext context) {
        // null и пустая строка считаются корректными (используется defaultValue="ALL")
        if (state == null || state.isBlank()) {
            return true;
        }
        return BookingStateParam.isValid(state);
    }
}
