package ru.practicum.shareit.booking.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BookingStateValidator.class)
@Documented
public @interface ValidBookingState {
    String message() default "Unknown booking state";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
