package com.fintrack.controller.contact;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.fintrack.application.contact.TrustedContactService;
import com.fintrack.application.user.UserService;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Role;

@WebMvcTest(TrustedContactController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("TrustedContactController Tests")
class TrustedContactControllerTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrustedContactService trustedContactService;

    @MockBean
    private UserService userService;

    private User owner;
    private TrustedContact contact;

    @BeforeEach
    void setUp() {
        owner = User.createLocalUser("Owner", OWNER_EMAIL, "password", Set.of(Role.USER));
        ReflectionTestUtils.setField(owner, "id", 1L);

        contact = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", "family", "my wife");
        ReflectionTestUtils.setField(contact, "id", 10L);

        when(userService.getCurrentUser(anyString())).thenReturn(owner);
    }

    @Nested
    @DisplayName("List contacts")
    class ListTests {

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return list of contacts successfully")
        void shouldReturnListOfContactsSuccessfully() throws Exception {
            // Given
            when(trustedContactService.findByOwner(eq(owner), any())).thenReturn(List.of(contact));

            // When & Then
            mockMvc.perform(get("/api/trusted-contacts").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(10))
                    .andExpect(jsonPath("$[0].name").value("Sabrina"))
                    .andExpect(jsonPath("$[0].email").value("sabrina@example.com"))
                    .andExpect(jsonPath("$[0].tags").value("family"))
                    .andExpect(jsonPath("$[0].note").value("my wife"));
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return empty list when no contacts")
        void shouldReturnEmptyListWhenNoContacts() throws Exception {
            // Given
            when(trustedContactService.findByOwner(eq(owner), any())).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/trusted-contacts").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should pass search parameter to service")
        void shouldPassSearchParameterToService() throws Exception {
            // Given
            when(trustedContactService.findByOwner(eq(owner), eq("sabrina"))).thenReturn(List.of(contact));

            // When & Then
            mockMvc.perform(get("/api/trusted-contacts").param("search", "sabrina")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(trustedContactService).findByOwner(eq(owner), eq("sabrina"));
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return 400 when user not found")
        void shouldReturn400WhenUserNotFound() throws Exception {
            // Given
            when(userService.getCurrentUser(anyString())).thenThrow(new IllegalArgumentException("User not found"));

            // When & Then
            mockMvc.perform(get("/api/trusted-contacts").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }

    @Nested
    @DisplayName("Create contact")
    class CreateTests {

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should create contact successfully")
        void shouldCreateContactSuccessfully() throws Exception {
            // Given
            when(trustedContactService.create(eq(owner), eq("Sabrina"), eq("sabrina@example.com"),
                    eq("family"), eq("my wife"))).thenReturn(contact);

            String body = """
                    {"name":"Sabrina","email":"sabrina@example.com","tags":"family","note":"my wife"}
                    """;

            // When & Then
            mockMvc.perform(post("/api/trusted-contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("Sabrina"))
                    .andExpect(jsonPath("$.email").value("sabrina@example.com"));
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            String body = """
                    {"name":"","email":"sabrina@example.com"}
                    """;

            mockMvc.perform(post("/api/trusted-contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            String body = """
                    {"name":"Sabrina","email":"not-an-email"}
                    """;

            mockMvc.perform(post("/api/trusted-contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return 400 when duplicate email")
        void shouldReturn400WhenDuplicateEmail() throws Exception {
            // Given
            String msg = "A contact with this email already exists in your circle.";
            when(trustedContactService.create(eq(owner), eq("Sabrina"), eq("sabrina@example.com"), any(), any()))
                    .thenThrow(new IllegalArgumentException(msg));

            String body = """
                    {"name":"Sabrina","email":"sabrina@example.com"}
                    """;

            // When & Then
            mockMvc.perform(post("/api/trusted-contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(msg));
        }
    }

    @Nested
    @DisplayName("Update contact")
    class UpdateTests {

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should update contact successfully")
        void shouldUpdateContactSuccessfully() throws Exception {
            // Given
            TrustedContact updated = TrustedContact.create(
                    owner, "Sabrina Updated", "sabrina@example.com", "family", null);
            ReflectionTestUtils.setField(updated, "id", 10L);
            when(trustedContactService.update(eq(owner), eq(10L), eq("Sabrina Updated"),
                    eq("sabrina@example.com"), eq("family"), eq(null))).thenReturn(updated);

            String body = """
                    {"name":"Sabrina Updated","email":"sabrina@example.com","tags":"family","note":null}
                    """;

            // When & Then
            mockMvc.perform(put("/api/trusted-contacts/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("Sabrina Updated"));
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return 400 when contact not found")
        void shouldReturn400WhenContactNotFound() throws Exception {
            // Given
            when(trustedContactService.update(eq(owner), eq(999L), any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Contact not found"));

            String body = """
                    {"name":"Sabrina","email":"sabrina@example.com"}
                    """;

            // When & Then
            mockMvc.perform(put("/api/trusted-contacts/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Contact not found"));
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return 400 when user not found")
        void shouldReturn400WhenUserNotFound() throws Exception {
            when(userService.getCurrentUser(anyString())).thenThrow(new IllegalArgumentException("User not found"));

            String body = """
                    {"name":"Sabrina","email":"sabrina@example.com"}
                    """;

            mockMvc.perform(put("/api/trusted-contacts/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }

    @Nested
    @DisplayName("Delete contact")
    class DeleteTests {

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should delete contact successfully")
        void shouldDeleteContactSuccessfully() throws Exception {
            // Given
            // trustedContactService.delete(owner, 10L) returns void

            // When & Then
            mockMvc.perform(delete("/api/trusted-contacts/10").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(trustedContactService).delete(eq(owner), eq(10L));
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return 400 when contact not found on delete")
        void shouldReturn400WhenContactNotFoundOnDelete() throws Exception {
            // Given
            doThrow(new IllegalArgumentException("Contact not found"))
                    .when(trustedContactService).delete(eq(owner), eq(999L));

            // When & Then
            mockMvc.perform(delete("/api/trusted-contacts/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Contact not found"));
        }

        @Test
        @WithMockUser(username = OWNER_EMAIL)
        @DisplayName("Should return 400 when user not found on delete")
        void shouldReturn400WhenUserNotFoundOnDelete() throws Exception {
            when(userService.getCurrentUser(anyString())).thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(delete("/api/trusted-contacts/10").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }
}
