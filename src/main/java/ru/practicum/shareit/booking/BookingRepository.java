package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Базовая часть запроса для устранения дублирования JOIN FETCH
    String BASE_FETCH = "SELECT b FROM Booking b " +
            "JOIN FETCH b.item i " +
            "JOIN FETCH i.owner " +
            "JOIN FETCH b.booker ";

    // --- Бронирования арендатора (booker) ---

    // ALL for booker: все бронирования арендатора, загружаем item и owner и booker чтобы избежать N+1
    @Query(BASE_FETCH +
            "WHERE b.booker.id = :userId " +
            "ORDER BY b.start DESC")
    List<Booking> findAllByBookerIdOrderByStartDesc(@Param("userId") Long userId);

    // WAITING or REJECTED or APPROVED by status for booker
    List<Booking> findByBookerIdAndStatusOrderByStartDesc(Long bookerId, BookingStatus status);

    // CURRENT: start <= now < end  (Запросный метод не подойдет, т.к. нужна строгая граница  start <= now)
    @Query(BASE_FETCH +
            "WHERE b.booker.id = :userId AND b.start <= :now AND b.end > :now " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentByBookerId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // PAST: end <= now
    @Query(BASE_FETCH +
            "WHERE b.booker.id = :userId AND b.end <= :now " +
            "ORDER BY b.start DESC")
    List<Booking> findPastByBookerId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // FUTURE: start > now
    @Query(BASE_FETCH +
            "WHERE b.booker.id = :userId AND b.start > :now " +
            "ORDER BY b.start DESC")
    List<Booking> findFutureByBookerId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /* --- Аналогичные методы для владельца (owner) --- */

    @Query(BASE_FETCH +
            "WHERE i.owner.id = :ownerId " +
            "ORDER BY b.start DESC")
    List<Booking> findAllByOwnerIdOrderByStartDesc(@Param("ownerId") Long ownerId);

    List<Booking> findByItemOwnerIdAndStatusOrderByStartDesc(Long ownerId, BookingStatus status);

    @Query(BASE_FETCH +
            "WHERE i.owner.id = :ownerId AND b.start <= :now AND b.end > :now " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentByOwnerId(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Query(BASE_FETCH +
            "WHERE i.owner.id = :ownerId AND b.end <= :now " +
            "ORDER BY b.start DESC")
    List<Booking> findPastByOwnerId(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Query(BASE_FETCH +
            "WHERE i.owner.id = :ownerId AND b.start > :now " +
            "ORDER BY b.start DESC")
    List<Booking> findFutureByOwnerId(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);


    // получить все APPROVED бронирования владельца
    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item i " +     // в вызываемом методе есть обращение в цикле booking.getItem().getId()
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.status = 'APPROVED' " +
            "ORDER BY b.start DESC")
    List<Booking> findBookingsByOwner(@Param("ownerId") Long ownerId);
}

