package com.fooddelivery.orderservice;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fooddelivery.common.security.JwtTokenService;
import com.fooddelivery.common.security.Role;
import com.fooddelivery.orderservice.model.FoodOrder;
import com.fooddelivery.orderservice.repository.FoodOrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderServiceIntegrationTest {

    private static final long ALICE = 11L;
    private static final long BOB = 12L;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtTokenService tokens;
    @Autowired
    FoodOrderRepository orders;

    long aliceOrder1;
    long aliceOrder2;
    long bobOrder;

    @BeforeEach
    void seed() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);
        aliceOrder1 = save(ALICE, "Alice", "10.00", "PLACED", base);
        aliceOrder2 = save(ALICE, "Alice", "30.00", "DELIVERED", base.plusHours(1));
        bobOrder = save(BOB, "Bob", "20.00", "PLACED", base.plusHours(2));
    }

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void serviceTokensAreNotAllowedHere() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + tokens.issueServiceToken("x")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usersSeeOnlyTheirOwnOrdersNewestFirst() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].id").value(contains((int) aliceOrder2, (int) aliceOrder1)))
                .andExpect(jsonPath("$.data.content[*].customerName").value(not(contains("Bob"))));
    }

    @Test
    void administratorsSeeEveryOrder() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    void pagingAndSortingWork() throws Exception {
        mockMvc.perform(get("/api/orders?page=0&size=2&sortBy=totalAmount&direction=asc").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].totalAmount").value(10.00))
                .andExpect(jsonPath("$.data.content[1].totalAmount").value(20.00))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    void responsesUseOurPageShapeNotSpringsInternalOne() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", admin()))
                .andExpect(content().string(not(containsString("pageable"))))
                .andExpect(content().string(not(containsString("\"sort\""))));
    }

    @Test
    void unknownSortFieldsAreBadRequestsNotServerErrors() throws Exception {
        mockMvc.perform(get("/api/orders?sortBy=password; DROP TABLE x").header("Authorization", admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot sort by")));
        mockMvc.perform(get("/api/orders?sortBy=doesNotExist").header("Authorization", admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPagingParametersAreRejected() throws Exception {
        mockMvc.perform(get("/api/orders?page=-1").header("Authorization", admin())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/orders?size=0").header("Authorization", admin())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/orders?size=101").header("Authorization", admin())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/orders?direction=sideways").header("Authorization", admin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/orders?page=abc").header("Authorization", admin())).andExpect(status().isBadRequest());
    }

    @Test
    void usersCanFetchTheirOwnOrderById() throws Exception {
        mockMvc.perform(get("/api/orders/" + aliceOrder1).header("Authorization", user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(aliceOrder1))
                .andExpect(jsonPath("$.data.orderStatus").value("PLACED"));
    }

    @Test
    void someoneElsesOrderLooksLikeAMissingOne() throws Exception {
        String forBob = mockMvc.perform(get("/api/orders/" + aliceOrder1).header("Authorization", user(BOB)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missing = mockMvc.perform(get("/api/orders/999999").header("Authorization", user(BOB)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(forBob.replace(String.valueOf(aliceOrder1), "N"))
                .isEqualTo(missing.replace("999999", "N"));
    }

    @Test
    void administratorsCanFetchAnyOrder() throws Exception {
        mockMvc.perform(get("/api/orders/" + bobOrder).header("Authorization", admin()))
                .andExpect(status().isOk());
    }

    private long save(long userId, String name, String amount, String status, LocalDateTime at) {
        FoodOrder order = new FoodOrder();
        order.setUserId(userId);
        order.setCustomerName(name);
        order.setOrderDetails("test");
        order.setTotalAmount(new BigDecimal(amount));
        order.setOrderStatus(status);
        order.setOrderTimestamp(at);
        return orders.saveAndFlush(order).getId();
    }

    private String user(long id) {
        return "Bearer " + tokens.issueUserToken(id, "u" + id + "@example.com", "User " + id, Role.USER);
    }

    private String admin() {
        return "Bearer " + tokens.issueUserToken(1L, "admin@example.com", "Admin", Role.ADMIN);
    }
}
