package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class ItemRequestRepositoryTest {

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private UserRepository userRepository;

    private User requester;
    private User otherUser;

    private ItemRequest request1;
    private ItemRequest request2;
    private ItemRequest request3;

    @BeforeEach
    void setUp() {
        // создаём пользователей
        requester = userRepository.save(
                new User(null, "Requester", "requester@mail.ru"));
        otherUser = userRepository.save(
                new User(null, "OtherUser", "other@mail.ru"));

        // создаём несколько запросов
        request1 = itemRequestRepository.save(
                new ItemRequest(null, "Request 1",
                        requester, LocalDateTime.now().minusDays(2)));
        request2 = itemRequestRepository.save(
                new ItemRequest(null, "Request 2",
                        requester, LocalDateTime.now().minusDays(1)));
        request3 = itemRequestRepository.save(
                new ItemRequest(null, "Request 3",
                        otherUser, LocalDateTime.now()));
    }

    // ---------------------- findAllByRequesterIdOrderByCreatedDesc ----------------------
    @Test
    void findAllByRequesterIdOrderByCreatedDescShouldReturnRequestsOfSpecificUserInDescOrder() {
        List<ItemRequest> requests =
                itemRequestRepository.findAllByRequesterIdOrderByCreatedDesc(requester.getId());

        // ожидаем только запросы "requester", отсортированные по created DESC
        assertThat(requests).hasSize(2);
        // проверяем, что первый элемент - более свежий (request2), второй - старый (request1)
        assertThat(requests.get(0).getId()).isEqualTo(request2.getId());
        assertThat(requests.get(1).getId()).isEqualTo(request1.getId());

        // проверяем, что все запросы принадлежат нужному пользователю
        assertThat(requests).allMatch(
                r -> r.getRequester().getId().equals(requester.getId()));
    }

    // ---------------------- findAllByRequesterIdNotOrderByCreatedDesc ----------------------
    @Test
    void findAllByRequesterIdNotOrderByCreatedDescShouldReturnRequestsOfOtherUsersInDescOrder() {
        List<ItemRequest> requests =
                itemRequestRepository.findAllByRequesterIdNotOrderByCreatedDesc(requester.getId());

        // ожидаем только запросы, НЕ принадлежащие "requester"
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getId()).isEqualTo(request3.getId());

        // проверяем, что все запросы НЕ принадлежат "requester"
        assertThat(requests).allMatch(
                r -> !r.getRequester().getId().equals(requester.getId()));
    }
}
