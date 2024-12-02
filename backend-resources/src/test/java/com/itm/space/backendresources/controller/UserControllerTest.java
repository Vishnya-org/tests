package com.itm.space.backendresources.controller;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.util.JsonSerialization.mapper;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest extends BaseIntegrationTest {

    @MockBean
    private Keycloak keycloak;
    @MockBean
    private UserService userService;


    @Value("${keycloak.realm}")
    private String realmITM;
    private UserRequest userRequest;
    private UserResponse userResponse;
    private RealmResource realmMock;
    private UserResource user;
    private UserRepresentation userRepresentation;


    @BeforeEach
    void setUp() {
        userRequest = new UserRequest("Username", "mail@ya.ru", "test",
                "firstName", "lastName");
        userResponse = new UserResponse("firstName", "lastname", "mail@ya.ru",
                Arrays.asList("ROLE_USER"), Arrays.asList("GROUP1", "GROUP2"));
        realmMock = mock(RealmResource.class);
        user = mock(UserResource.class);
        userRepresentation = mock(UserRepresentation.class);
    }

    @Test
    @SneakyThrows
    @WithMockUser(roles = "MODERATOR")
    void createTest() {
        MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"), userRequest))
                .andReturn().getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    @SneakyThrows
    @WithMockUser(username = "Username", password = "test", roles = "MODERATOR")
    void getUserByIdTest() {
        UUID userId = UUID.randomUUID();
        when(keycloak.realm(realmITM)).thenReturn(realmMock);
        when(realmMock.users()).thenReturn(mock(UsersResource.class));
        when(realmMock.users().get(eq(String.valueOf(userId)))).thenReturn(user);
        when(user.toRepresentation()).thenReturn(userRepresentation);
        when(userRepresentation.getId()).thenReturn(String.valueOf(userId));

        Mockito.when(userService.getUserById(Mockito.any(UUID.class))).thenReturn(userResponse);
        mvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithMockUser(username = "Username", password = "test", roles = "MODERATOR")
    void helloTest() {
        MockHttpServletResponse response = mvc.perform(get("http://backend-gateway-client:9090/api/users/hello")).andReturn().getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("Username", response.getContentAsString());
    }

    @Test
    @SneakyThrows
    void createUserTestWithException() {
        mvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(userRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @SneakyThrows
    @WithMockUser(username = "Username", password = "test", roles = "MODERATOR")
    void getUserByIdNotFoundTest() {
        UUID userId = UUID.randomUUID();

        when(keycloak.realm(realmITM)).thenReturn(realmMock);
        when(realmMock.users()).thenReturn(mock(UsersResource.class));

        Mockito.when(userService.getUserById(Mockito.any(UUID.class)))
                .thenThrow(new RuntimeException("User not found"));

        mvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertEquals("User not found", result.getResolvedException().getMessage()));
    }
}