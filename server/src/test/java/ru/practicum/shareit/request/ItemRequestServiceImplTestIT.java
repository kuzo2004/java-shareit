package ru.practicum.shareit.request;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoPost;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class ItemRequestServiceImplTestIT {

    @Autowired
    private ItemRequestServiceImpl itemRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemService itemService;

    private User requester;
    private User otherUser;
    private ItemRequest request;

    @BeforeEach
    void setUp() {
        requester = new User(null, "Requester", "requester@mail.ru");
        requester = userRepository.save(requester);

        otherUser = new User(null, "OtherUser", "other@mail.ru");
        otherUser = userRepository.save(otherUser);

        Item item = new Item(null, "Drill", "Cordless drill",
                true, otherUser, null);
        item = itemRepository.save(item);
    }

    @AfterEach
    void clear() {
        itemRepository.deleteAll();
        itemRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------------------- CREATE REQUEST ----------------------
    @Test
    void createRequestShouldSaveRequest() {
        ItemRequestDtoPost postDto = new ItemRequestDtoPost("Need a drill");

        ItemRequestDto created = itemRequestService.createRequest(postDto, requester.getId());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getDescription()).isEqualTo("Need a drill");
        assertThat(created.getItems()).isNull();
    }

    // ---------------------- GET MY REQUESTS ----------------------
    @Test
    void getMyRequestsShouldReturnRequesterRequests() {
        // создаём запрос
        ItemRequestDtoPost postDto = new ItemRequestDtoPost("Need a drill");
        ItemRequestDto created = itemRequestService.createRequest(postDto, requester.getId());

        List<ItemRequestDto> requests = itemRequestService.getMyRequests(requester.getId());

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getId()).isEqualTo(created.getId());
        assertThat(requests.get(0).getDescription()).isEqualTo("Need a drill");
    }

    // ---------------------- GET ALL REQUESTS ----------------------
    @Test
    void getAllRequestsShouldReturnRequestsOfOtherUsers() {
        // создаём запрос для requester
        ItemRequestDtoPost postDto = new ItemRequestDtoPost("Need a drill");
        itemRequestService.createRequest(postDto, requester.getId());

        // другой пользователь получает все запросы кроме своих
        List<ItemRequestDto> requests = itemRequestService.getAllRequests(otherUser.getId());

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getDescription()).isEqualTo("Need a drill");
    }

    // ---------------------- GET REQUEST BY ID ----------------------
    @Test
    void getRequestByIdShouldReturnRequestWithItems() {
        // создаём запрос
        ItemRequestDtoPost postDto = new ItemRequestDtoPost("Need a chair");
        ItemRequestDto created = itemRequestService.createRequest(postDto, requester.getId());
        ItemRequest createdRequest = itemRequestRepository.findById(created.getId()).orElseThrow();

        // создаем новый item на основании заявки
        Item itemWithRequest = new Item(null, "Chair",
                "Wooden chair", true, otherUser, createdRequest);
        itemRepository.save(itemWithRequest);

        // любой пользователь может посмотреть заявку
        User user = new User(null, "User", "user@mail.ru");
        user = userRepository.save(user);

        // поиск заявки по id
        ItemRequestDto found = itemRequestService.getRequestById(created.getId(), user.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getDescription()).isEqualTo("Need a chair");
        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getName()).isEqualTo("Chair");
    }
}
