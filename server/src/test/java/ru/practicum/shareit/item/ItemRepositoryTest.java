package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private User owner;
    private User user;
    private ItemRequest request;
    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        // Создаём пользователя
        user = new User(null, "User", "user@mail.ru");
        user = userRepository.save(user);
        // Создаём владельца
        owner = new User(null, "Owner", "owner@mail.ru");
        owner = userRepository.save(owner);

        // Создаём запрос на вещь
        request = new ItemRequest(null, "Need a drill", user, LocalDateTime.now());
        request = itemRequestRepository.save(request);

        // Создаём несколько вещей
        item1 = new Item(null, "Drill", "Hand drill", true, owner, request);
        item2 = new Item(null, "Hammer", "Hand hammer", false, owner, null);
        itemRepository.saveAll(List.of(item1, item2));
    }

    // ---------------------------------------------------------
    // findByOwnerId()
    // ---------------------------------------------------------
    @Test
    void findByOwnerIdShouldReturnItemsOfThatOwner() {
        // ищем по ID владельца
        List<Item> items = itemRepository.findByOwnerId(owner.getId());

        // ожидаем 2 вещи, так как обе принадлежат одному владельцу
        assertThat(items).hasSize(2);
        assertThat(items).extracting(Item::getName)
                         .containsExactlyInAnyOrder("Drill", "Hammer");
    }

    // ---------------------------------------------------------
    // searchAvailableItems()
    // ---------------------------------------------------------
    @Test
    void searchAvailableItemsShouldReturnOnlyAvailableItemsByText() {
        // ищем по слову "drill" (регистронезависимо) и статусу available = true
        List<Item> found = itemRepository.searchAvailableItems("drill");

        // ожидаем только одну вещь
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Drill");
        assertThat(found.get(0).getAvailable()).isTrue();
    }

    @Test
    void searchAvailableItemsShouldReturnMultipleAvailableItemsIfTextMatches() {
        //изменим статус
        item2.setAvailable(Boolean.TRUE);
        itemRepository.save(item2);

        // ищем по слову "hand" (регистронезависимо) и статусу available = true
        List<Item> found = itemRepository.searchAvailableItems("hand");

        // ожидаем 2 вещи
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Item::getName)
                         .containsExactlyInAnyOrder("Drill", "Hammer");
    }

    // ---------------------------------------------------------
    // findByRequestIdOrderById()
    // ---------------------------------------------------------
    @Test
    void findByRequestIdOrderByIdShouldReturnItemsLinkedToRequest() {
        // получаем вещи, связанные с конкретным запросом
        List<Item> found = itemRepository.findByRequestIdOrderById(request.getId());

        // ожидаем одну вещь — ту, что была связана с request
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Drill");
        assertThat(found.get(0).getRequest().getId()).isEqualTo(request.getId());
    }

    @Test
    void findByRequestIdOrderByIdShouldReturnItemsSortedById() {
        //изменим статус и добавим заявку
        item2.setAvailable(Boolean.TRUE);
        item2.setRequest(request);
        itemRepository.save(item2);

        // получаем все вещи, связанные с конкретным запросом
        List<Item> found = itemRepository.findByRequestIdOrderById(request.getId());

        // ожидаем две вещи, связанные с request
        assertThat(found).hasSize(2);

        // проверяем, что обе принадлежат нужному запросу
        assertThat(found).allMatch(
                i -> i.getRequest().getId().equals(request.getId()));

        // проверяем, что список отсортирован по возрастанию ID
        assertThat(found).extracting(Item::getId)
                         .isSorted();
    }

    // ---------------------------------------------------------
    // findAllByRequestIdOrderById()
    // ---------------------------------------------------------
    @Test
    void findAllByRequestIdOrderByIdShouldReturnItemsForMultipleRequests() {
        // создаём дополнительного пользователя и запрос
        User otherUser = userRepository.save(new User(null, "Other", "other@mail.ru"));
        ItemRequest secondRequest = itemRequestRepository.save(
                new ItemRequest(null, "Need hammer", otherUser, LocalDateTime.now())
        );

        // создаём новую вещь, связанную с другим запросом
        Item item3 = new Item(null, "Saw", "Hand saw", true, owner, secondRequest);
        itemRepository.save(item3);

        // ищем вещи по двум ID запросов
        List<Item> found = itemRepository.findAllByRequestIdOrderById(
                List.of(request.getId(), secondRequest.getId())
        );

        // ожидаем 2 вещи (Drill и Saw)
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Item::getName)
                         .containsExactlyInAnyOrder("Drill", "Saw");
    }
}