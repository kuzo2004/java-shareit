package ru.practicum.shareit.booking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.model.BookingStateParam;

@Component
public class BookingStateValidator implements ConstraintValidator<ValidBookingState, String> {

    @Override
    public void initialize(ValidBookingState constraintAnnotation) {
    }

    @Override
    public boolean isValid(String state, ConstraintValidatorContext context) {
        if (state == null) {
            return true; // @NotNull обрабатывается отдельно
        }
        return BookingStateParam.isValid(state);
    }
}
