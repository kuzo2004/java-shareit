package ru.practicum.shareit.booking.dto;


public enum BookingStateParam {
    ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED;

    public static boolean isValid(String state) {
        if (state == null) return false;
        try {
            BookingStateParam.valueOf(state.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}