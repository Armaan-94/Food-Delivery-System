package com.fooddelivery.deliveryservice;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.exception.ServiceUnavailableException;
import com.fooddelivery.common.security.JwtTokenService;
import com.fooddelivery.common.security.Role;
import com.fooddelivery.deliveryservice.client.OrderClient;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeliveryServiceIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtTokenService tokens;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    OrderClient orderClient;

    // ---- access control ----------------------------------------------------------------------

    @Test
    void everyEndpointRequiresAnAdministrator() throws Exception {
        String user = "Bearer " + tokens.issueUserToken(5L, "u@example.com", "U", Role.USER);

        mockMvc.perform(get("/api/deliveries")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/partners")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/deliveries").header("Authorization", user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/partners").header("Authorization", user)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/deliveries").header("Authorization", user)
                .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(1, null))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/partners/1").header("Authorization", user)).andExpect(status().isForbidden());
    }

    // ---- partners ----------------------------------------------------------------------------

    @Test
    void partnersCanBeCreatedReadUpdatedAndDeleted() throws Exception {
        long id = createPartner("Ravi", "9876543210", "Bike", null);

        mockMvc.perform(get("/api/partners/" + id).header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Ravi"))
                .andExpect(jsonPath("$.data.phoneNumber").value("9876543210"))
                .andExpect(jsonPath("$.data.vehicleType").value("Bike"))
                .andExpect(jsonPath("$.data.available").value(true));

        mockMvc.perform(put("/api/partners/" + id).header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ravi K\",\"phoneNumber\":\"9876543211\",\"vehicleType\":\"Scooter\",\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleType").value("Scooter"))
                .andExpect(jsonPath("$.data.available").value(false));

        mockMvc.perform(delete("/api/partners/" + id).header("Authorization", admin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/partners/" + id).header("Authorization", admin())).andExpect(status().isNotFound());
    }

    @Test
    void updatingWithoutTheAvailableFlagKeepsTheCurrentValue() throws Exception {
        long id = createPartner("Sam", null, "Bike", false);

        mockMvc.perform(put("/api/partners/" + id).header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sam\",\"vehicleType\":\"Car\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void partnerValidationRejectsBadInput() throws Exception {
        mockMvc.perform(post("/api/partners").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"phoneNumber\":\"call me maybe\",\"vehicleType\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("name")))
                .andExpect(jsonPath("$.message").value(containsString("phoneNumber")))
                .andExpect(jsonPath("$.message").value(containsString("vehicleType")));
    }

    @Test
    void missingPartnersAreNotFound() throws Exception {
        mockMvc.perform(get("/api/partners/999999").header("Authorization", admin())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/partners/999999").header("Authorization", admin())).andExpect(status().isNotFound());
    }

    // ---- creating deliveries -----------------------------------------------------------------

    @Test
    void deliveryWithoutAPartnerStartsPendingAndKeepsItsLocations() throws Exception {
        String response = mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(100, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.partnerId").value(nullValue()))
                .andExpect(jsonPath("$.data.pickupLocation").value("12 Market Road"))
                .andExpect(jsonPath("$.data.dropoffLocation").value("48 Lake View"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).path("data").path("id").asLong();

        // A NULL partner must come back as null (not 0), and the creation time must not change between reads.
        String first = read(id);
        Thread.sleep(1100);
        String second = read(id);
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(first).path("data").path("partnerId").isNull()).isTrue();
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(first).path("data").path("createdAt"))
                .isEqualTo(objectMapper.readTree(second).path("data").path("createdAt"));
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(first).path("data").path("pickupLocation").asText())
                .isEqualTo("12 Market Road");
    }

    @Test
    void deliveryWithAnAvailablePartnerStartsAssigned() throws Exception {
        long partner = createPartner("Ana", null, "Bike", true);

        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(101, partner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.partnerId").value(partner));
    }

    @Test
    void unavailableOrUnknownPartnersAreRejected() throws Exception {
        long busy = createPartner("Busy", null, "Bike", false);

        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(102, busy)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("not available")));
        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(102, 999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anOrderThatDoesNotExistIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Order not found with id: 103")).when(orderClient).assertOrderExists(103L);

        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(103, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 103"));
    }

    @Test
    void whenTheOrderServiceIsDownTheAnswerIsServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Order service is currently unavailable.", new RuntimeException("refused")))
                .when(orderClient).assertOrderExists(anyLong());

        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(104, null)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Order service is currently unavailable."));
    }

    @Test
    void anOrderCanOnlyHaveOneDelivery() throws Exception {
        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(105, null))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(105, null)))
                .andExpect(status().isConflict());
    }

    @Test
    void deliveryValidationRejectsBadInput() throws Exception {
        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":-1,\"pickupLocation\":\"\",\"dropoffLocation\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("orderId")))
                .andExpect(jsonPath("$.message").value(containsString("pickupLocation")))
                .andExpect(jsonPath("$.message").value(containsString("dropoffLocation")));
    }

    // ---- status changes ----------------------------------------------------------------------

    @Test
    void aDeliveryWalksThroughItsWholeLifecycle() throws Exception {
        long partner = createPartner("Lee", null, "Bike", true);
        long id = createDelivery(200, null);

        updateStatus(id, "ASSIGNED", partner).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.partnerId").value(partner));
        updateStatus(id, "picked_up", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.data.partnerId").value(partner));
        updateStatus(id, "OUT_FOR_DELIVERY", null).andExpect(status().isOk());
        updateStatus(id, "DELIVERED", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    @Test
    void assigningNeedsAPartner() throws Exception {
        long id = createDelivery(201, null);

        updateStatus(id, "ASSIGNED", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("partner must be assigned")));
    }

    @Test
    void impossibleTransitionsAndUnknownStatusesAreRejected() throws Exception {
        long id = createDelivery(202, null);

        updateStatus(id, "DELIVERED", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot move from PENDING to DELIVERED")));
        updateStatus(id, "TELEPORTED", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Unknown status")));
        updateStatus(id, "CANCELLED", null).andExpect(status().isOk());
        updateStatus(id, "ASSIGNED", null).andExpect(status().isBadRequest());
    }

    @Test
    void missingDeliveriesAreNotFound() throws Exception {
        mockMvc.perform(get("/api/deliveries/999999").header("Authorization", admin())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/deliveries/999999").header("Authorization", admin())).andExpect(status().isNotFound());
        updateStatus(999999, "CANCELLED", null).andExpect(status().isNotFound());
    }

    @Test
    void aPartnerWithDeliveriesCannotBeDeleted() throws Exception {
        long partner = createPartner("Kim", null, "Bike", true);
        mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(300, partner))).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/partners/" + partner).header("Authorization", admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("assigned to deliveries")));
    }

    @Test
    void deliveriesCanBeListedAndDeleted() throws Exception {
        long a = createDelivery(400, null);
        createDelivery(401, null);

        mockMvc.perform(get("/api/deliveries").header("Authorization", admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(2)));
        mockMvc.perform(delete("/api/deliveries/" + a).header("Authorization", admin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/deliveries").header("Authorization", admin()))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // ---- helpers -----------------------------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions updateStatus(long id, String status, Long partnerId) throws Exception {
        String body = "{\"status\":\"" + status + "\"" + (partnerId != null ? ",\"partnerId\":" + partnerId : "") + "}";
        RequestBuilder request = patch("/api/deliveries/" + id + "/status").header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(request);
    }

    private String read(long id) throws Exception {
        return mockMvc.perform(get("/api/deliveries/" + id).header("Authorization", admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private long createDelivery(long orderId, Long partnerId) throws Exception {
        String response = mockMvc.perform(post("/api/deliveries").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(deliveryJson(orderId, partnerId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createPartner(String name, String phone, String vehicle, Boolean available) throws Exception {
        String body = "{\"name\":\"" + name + "\","
                + (phone != null ? "\"phoneNumber\":\"" + phone + "\"," : "")
                + (available != null ? "\"available\":" + available + "," : "")
                + "\"vehicleType\":\"" + vehicle + "\"}";
        String response = mockMvc.perform(post("/api/partners").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private static String deliveryJson(long orderId, Long partnerId) {
        return "{\"orderId\":" + orderId + (partnerId != null ? ",\"partnerId\":" + partnerId : "")
                + ",\"pickupLocation\":\"12 Market Road\",\"dropoffLocation\":\"48 Lake View\"}";
    }

    private String admin() {
        return "Bearer " + tokens.issueUserToken(1L, "admin@example.com", "Admin", Role.ADMIN);
    }
}
