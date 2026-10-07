package com.fooddelivery.userservice;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.security.JwtTokenService;
import com.fooddelivery.common.security.Role;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtTokenService tokens;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JdbcTemplate jdbcTemplate;

    // ---- authentication ----------------------------------------------------------------------

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void garbageAndTamperedTokensAreRejected() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        String token = adminToken();
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---- authorization -----------------------------------------------------------------------

    @Test
    void onlyAdministratorsCanListUsers() throws Exception {
        long userId = createUser("u1@example.com");

        mockMvc.perform(get("/api/users").header("Authorization", bearer(userId, Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2))); // bootstrap admin + u1
    }

    @Test
    void usersCanReadOnlyTheirOwnAccount() throws Exception {
        long me = createUser("me@example.com");
        long other = createUser("other@example.com");

        mockMvc.perform(get("/api/users/" + me).header("Authorization", bearer(me, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("me@example.com"));
        mockMvc.perform(get("/api/users/" + other).header("Authorization", bearer(me, Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/" + other).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void currentUserEndpointReturnsTheCaller() throws Exception {
        long me = createUser("me2@example.com");

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(me, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(me));
    }

    @Test
    void usersCannotDeleteOtherAccountsButCanDeleteTheirOwn() throws Exception {
        long me = createUser("del-me@example.com");
        long other = createUser("del-other@example.com");

        mockMvc.perform(delete("/api/users/" + other).header("Authorization", bearer(me, Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/" + me).header("Authorization", bearer(me, Role.USER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/" + me).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotPromoteThemselvesButAdminsCanChangeRoles() throws Exception {
        long me = createUser("promote@example.com");
        String body = "{\"name\":\"Me\",\"email\":\"promote@example.com\",\"role\":\"ADMIN\"}";

        mockMvc.perform(put("/api/users/" + me).header("Authorization", bearer(me, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/users/" + me).header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void serviceTokensCannotUseThePublicApi() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + tokens.issueServiceToken("auth-service")))
                .andExpect(status().isForbidden());
    }

    // ---- behaviour ---------------------------------------------------------------------------

    @Test
    void createdUsersNeverExposePasswordsOrHashes() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nia\",\"email\":\"nia@example.com\",\"password\":\"password-123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2"))));
        assertStoredPasswordIsHashed("nia@example.com", "password-123");
        result.andReturn();
    }

    @Test
    void duplicateEmailsAreRejectedCaseInsensitively() throws Exception {
        createUser("dup@example.com");

        mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dup\",\"email\":\"DUP@example.com\",\"password\":\"password-123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void invalidInputIsRejectedWithFieldMessages() throws Exception {
        mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("email")))
                .andExpect(jsonPath("$.message").value(containsString("password")))
                .andExpect(jsonPath("$.message").value(containsString("name")));
    }

    @Test
    void missingUsersAreNotFoundNotServerErrors() throws Exception {
        String admin = "Bearer " + adminToken();

        mockMvc.perform(get("/api/users/999999").header("Authorization", admin)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/users/999999").header("Authorization", admin)).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/users/999999").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingAUserCanChangeThePassword() throws Exception {
        long me = createUser("pw@example.com");

        mockMvc.perform(put("/api/users/" + me).header("Authorization", bearer(me, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Me\",\"email\":\"pw@example.com\",\"password\":\"new-password-1\"}"))
                .andExpect(status().isOk());

        assertStoredPasswordIsHashed("pw@example.com", "new-password-1");
    }

    @Test
    void updateToAnotherUsersEmailConflicts() throws Exception {
        long a = createUser("a@example.com");
        createUser("b@example.com");

        mockMvc.perform(put("/api/users/" + a).header("Authorization", bearer(a, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"b@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void bootstrapAdministratorExistsAndCanBeVerified() throws Exception {
        mockMvc.perform(post("/internal/users/verify").header("Authorization", serviceBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"boot.admin@example.com\",\"password\":\"bootstrap-pass-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    // ---- internal endpoints (used by auth-service) ---------------------------------------------

    @Test
    void internalEndpointsRequireAServiceToken() throws Exception {
        String body = "{\"name\":\"N\",\"email\":\"n@example.com\",\"password\":\"password-123\"}";

        mockMvc.perform(post("/internal/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/users/register").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerCreatesAUserRoleAccountEvenIfAnAdminRoleIsSent() throws Exception {
        mockMvc.perform(post("/internal/users/register").header("Authorization", serviceBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rae\",\"email\":\"rae@example.com\",\"password\":\"password-123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void verifyAcceptsTheRightPasswordAndRejectsEverythingElseTheSameWay() throws Exception {
        mockMvc.perform(post("/internal/users/register").header("Authorization", serviceBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vee\",\"email\":\"vee@example.com\",\"password\":\"password-123\"}"));

        mockMvc.perform(verify("vee@example.com", "password-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("vee@example.com"));

        String wrongPassword = mockMvc.perform(verify("vee@example.com", "wrong-password"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownEmail = mockMvc.perform(verify("nobody@example.com", "password-123"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    // ---- helpers -----------------------------------------------------------------------------

    private org.springframework.test.web.servlet.RequestBuilder verify(String email, String password) {
        return post("/internal/users/verify").header("Authorization", serviceBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private long createUser(String email) throws Exception {
        String response = mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("data").path("id").asLong();
    }

    private String adminToken() {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'boot.admin@example.com'", Long.class);
        return tokens.issueUserToken(adminId, "boot.admin@example.com", "Boot Admin", Role.ADMIN);
    }

    private String bearer(long userId, Role role) {
        return "Bearer " + tokens.issueUserToken(userId, "u" + userId + "@example.com", "User " + userId, role);
    }

    private String serviceBearer() {
        return "Bearer " + tokens.issueServiceToken("auth-service");
    }

    private void assertStoredPasswordIsHashed(String email, String plaintext) {
        String stored = jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        org.assertj.core.api.Assertions.assertThat(stored).isNotEqualTo(plaintext).startsWith("$2");
        org.assertj.core.api.Assertions.assertThat(encoder.matches(plaintext, stored)).isTrue();
    }
}
