package com.fooddelivery.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.common.exception.DatabaseAccessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.exception.ServiceUnavailableException;
import com.fooddelivery.common.exception.ValidationException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void clientErrorsKeepTheirStatusAndMessage() throws Exception {
        mockMvc.perform(get("/not-found")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Thing 7 not found"));
        mockMvc.perform(get("/conflict")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already there"));
        mockMvc.perform(get("/invalid")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad input"));
        mockMvc.perform(get("/unavailable")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Order service is currently unavailable."));
    }

    @Test
    void databaseErrorsNeverLeakDetails() throws Exception {
        mockMvc.perform(get("/database")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("jdbc"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("user_db"))));
    }

    @Test
    void unexpectedErrorsAreGeneric() throws Exception {
        mockMvc.perform(get("/boom")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret detail"))));
    }

    @Test
    void accessDeniedIsForbidden() throws Exception {
        mockMvc.perform(get("/denied")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void validationErrorsListTheFields() throws Exception {
        mockMvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"code\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("code")));
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body."));
    }

    @Test
    void wrongMethodKeepsStatusButUsesEnvelope() throws Exception {
        mockMvc.perform(post("/boom")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void badPathVariableIsBadRequest() throws Exception {
        mockMvc.perform(get("/items/abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'."));
    }

    @RestController
    static class FailingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Thing 7 not found");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new DuplicateResourceException("Already there");
        }

        @GetMapping("/invalid")
        void invalid() {
            throw new ValidationException("Bad input");
        }

        @GetMapping("/unavailable")
        void unavailable() {
            throw new ServiceUnavailableException("Order service is currently unavailable.", new RuntimeException("conn refused"));
        }

        @GetMapping("/database")
        void database() {
            throw new DatabaseAccessException("Failed: jdbc:mysql://db/user_db Access denied for user", new RuntimeException("sql"));
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret detail");
        }

        @GetMapping("/denied")
        void denied() {
            throw new AccessDeniedException("nope");
        }

        @GetMapping("/items/{id}")
        void item(@org.springframework.web.bind.annotation.PathVariable Long id) {
        }

        @PostMapping("/echo")
        String echo(@Valid @RequestBody Payload payload) {
            return payload.name();
        }
    }

    record Payload(@NotBlank String name, @Size(min = 3) String code) {
    }
}
