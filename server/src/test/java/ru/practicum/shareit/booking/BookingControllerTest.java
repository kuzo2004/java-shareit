package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoPost;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto owner;
    private UserDto booker;
    private ItemDto item;
    private BookingDtoPost bookingDtoPost;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setup() {
        owner = new UserDto(2L, "Owner", "owner@mail.com");
        booker = new UserDto(1L, "Booker", "booker@mail.com");
        item = new ItemDto(1L, "Drill", "Electric drill",
                true, null, null);

        start = LocalDateTime.now().plusDays(1);
        end = LocalDateTime.now().plusDays(2);
        bookingDtoPost = new BookingDtoPost(item.getId(), start, end);
    }

    // --------------------------------------------------------
    // POST /bookings — создание бронирования
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void createBookingShouldReturnCreatedBooking() {
        BookingDto dto = new BookingDto(1L, start, end, item, booker, BookingStatus.APPROVED);

        Mockito.when(bookingService.createBooking(any(BookingDtoPost.class), eq(booker.getId())))
               .thenReturn(dto);

        mockMvc.perform(post("/bookings")
                       .contentType(MediaType.APPLICATION_JSON)
                       .header("X-Sharer-User-Id", booker.getId())
                       .content(objectMapper.writeValueAsString(bookingDtoPost)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1))
               .andExpect(jsonPath("$.item.id").value(item.getId()))
               .andExpect(jsonPath("$.booker.id").value(booker.getId()))
               .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingService).createBooking(any(BookingDtoPost.class), eq(booker.getId()));
    }

    @Test
    @SneakyThrows
    void createBookingShouldReturnBadRequestOnValidationException() {
        // симулирует любое из нарушений валидации (поля не заполнены или некорректны)
        Mockito.when(bookingService.createBooking(any(), anyLong()))
               .thenThrow(new ValidationException("Invalid booking"));

        mockMvc.perform(post("/bookings")
                       .contentType(MediaType.APPLICATION_JSON)
                       .header("X-Sharer-User-Id", 1L)
                       // устанавливает JSON-строку как тело запроса
                       .content(objectMapper.writeValueAsString(bookingDtoPost)))
               .andExpect(status().isBadRequest());

        verify(bookingService).createBooking(any(), anyLong());
    }

    // --------------------------------------------------------
    // PATCH /bookings/{bookingId}?approved=true|false
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void updateBookingStatusShouldReturnUpdatedBooking() {
        BookingDto updated = new BookingDto(1L, start, end, item, booker, BookingStatus.APPROVED);

        Mockito.when(bookingService.updateBookingStatus(eq(1L), eq(true), eq(owner.getId())))
               .thenReturn(updated);

        mockMvc.perform(patch("/bookings/1")
                       .param("approved", "true")
                       .header("X-Sharer-User-Id", owner.getId()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("APPROVED"))
               .andExpect(jsonPath("$.item.name").value(item.getName()))
               .andExpect(jsonPath("$.booker.name").value(booker.getName()));

        verify(bookingService).updateBookingStatus(1L, true, owner.getId());
    }

    @Test
    @SneakyThrows
    void updateBookingStatusShouldReturnNotFound() {
        Long bookingId = 99L;
        Mockito.when(bookingService.updateBookingStatus(anyLong(), anyBoolean(), anyLong()))
               .thenThrow(new NotFoundException("Бронирование с id=" + bookingId + " не существует"));

        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                       .param("approved", "false")
                       .header("X-Sharer-User-Id", 1L))
               .andExpect(status().isNotFound());

        verify(bookingService).updateBookingStatus(bookingId, false, 1L);
    }

    // --------------------------------------------------------
    // GET /bookings/{bookingId}
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void getBookingShouldReturnBooking() {
        BookingDto dto = new BookingDto(1L, start, end, item, booker, BookingStatus.APPROVED);

        Mockito.when(bookingService.getBookingById(1L, booker.getId())).thenReturn(dto);

        mockMvc.perform(get("/bookings/1")
                       .header("X-Sharer-User-Id", booker.getId()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1))
               .andExpect(jsonPath("$.item.id").value(item.getId()))
               .andExpect(jsonPath("$.booker.id").value(booker.getId()))
               .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingService).getBookingById(1L, booker.getId());
    }

    @Test
    @SneakyThrows
    void getBookingShouldReturnNotFound() {
        Long bookingId = 99L;

        Mockito.when(bookingService.getBookingById(anyLong(), anyLong()))
               .thenThrow(new NotFoundException("Бронирование с id=" + bookingId + " не существует"));

        mockMvc.perform(get("/bookings/{bookingId}", bookingId)
                       .header("X-Sharer-User-Id", booker.getId()))
               .andExpect(status().isNotFound());

        verify(bookingService).getBookingById(bookingId, 1L);
    }

    // --------------------------------------------------------
    // GET /bookings?state=ALL
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void getBookingsByBookerShouldReturnList() {
        BookingDto dto = new BookingDto(1L, start, end, item, booker, BookingStatus.APPROVED);

        Mockito.when(bookingService.getBookingsByBooker(booker.getId(), "ALL"))
               .thenReturn(List.of(dto));

        mockMvc.perform(get("/bookings")
                       .param("state", "ALL")
                       .header("X-Sharer-User-Id", booker.getId()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1))
               .andExpect(jsonPath("$[0].item.id").value(item.getId()))
               .andExpect(jsonPath("$[0].booker.id").value(booker.getId()));

        verify(bookingService).getBookingsByBooker(booker.getId(), "ALL");
    }

    // --------------------------------------------------------
    // GET /bookings/owner?state=ALL
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void getBookingsByOwnerShouldReturnList() {
        BookingDto dto = new BookingDto(1L, start, end, item, booker, BookingStatus.WAITING);

        Mockito.when(bookingService.getBookingsByOwner(owner.getId(), "ALL"))
               .thenReturn(List.of(dto));

        mockMvc.perform(get("/bookings/owner")
                       .param("state", "ALL")
                       .header("X-Sharer-User-Id", owner.getId()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1))
               .andExpect(jsonPath("$[0].status").value("WAITING"))
               .andExpect(jsonPath("$[0].item.name").value(item.getName()))
               .andExpect(jsonPath("$[0].booker.name").value(booker.getName()));

        verify(bookingService).getBookingsByOwner(owner.getId(), "ALL");
    }
}
