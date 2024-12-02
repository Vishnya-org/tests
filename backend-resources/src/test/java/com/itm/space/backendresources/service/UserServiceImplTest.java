package com.itm.space.backendresources.service;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
class UserServiceImplTest {

    @MockBean
    private Keycloak keycloakClient;

    @Autowired
    private UserServiceImpl userService;

    @MockBean
    private UserMapper userMapper;

    private final RealmResource realmResource = mock(RealmResource.class);
    private final UsersResource usersResource = mock(UsersResource.class);
    private final UserResource userResource = mock(UserResource.class);
    private final RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
    private final MappingsRepresentation mappingsRepresentation = mock(MappingsRepresentation.class);

    private final String REALM = "ITM";
    private UUID testUserId;

    @BeforeEach
    void setup() {
        when(keycloakClient.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(usersResource.create(any(UserRepresentation.class)))
                .thenReturn(Response.status(201).build());
        testUserId = UUID.fromString("8b6f1550-b431-4cec-9b46-e47ba350c062");
    }

    @Test
    void createUserTest() {
        UserRequest userRequest = new UserRequest("Username", "mail@ya.ru", "test",
                "firstName", "lastName");

        assertDoesNotThrow(() -> userService.createUser(userRequest));
        verify(keycloakClient.realm(REALM).users(), times(1)).create(any(UserRepresentation.class));
    }

    @Test
    void getUserByIdTest() {
        UserResponse userResponse = new UserResponse("firstName", "lastname", "mail@ya.ru",
                Arrays.asList("ROLE_USER"), Arrays.asList("GROUP1", "GROUP2"));
        when(keycloakClient.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUserId.toString())).thenReturn(userResource);
        UserRepresentation mockUser = new UserRepresentation();
        mockUser.setId(testUserId.toString());
        mockUser.setEmail("mail@ya.ru");
        when(userResource.toRepresentation()).thenReturn(mockUser);
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName("ROLE_USER");
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(mappingsRepresentation);
        when(mappingsRepresentation.getRealmMappings()).thenReturn(List.of(roleRepresentation));
        when(userMapper.userRepresentationToUserResponse(any(UserRepresentation.class), anyList(), anyList()))
                .thenReturn(userResponse);

        UserResponse response = userService.getUserById(testUserId);

        assertNotNull(response);
        assertEquals("mail@ya.ru", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertTrue(response.getGroups().contains("GROUP1"));
        assertTrue(response.getGroups().contains("GROUP2"));
    }

    @Test
    void createUser_WhenKeycloakFails_ShouldThrowException() {
        UserRequest userRequest = new UserRequest("Username", "mail@ya.ru", "test",
                "firstName", "lastName");
        when(keycloakClient.realm(REALM).users().create(any(UserRepresentation.class)))
                .thenThrow(new RuntimeException("Error creating user"));

        assertThrows(RuntimeException.class, () -> userService.createUser(userRequest));
    }

    @Test
    void getUserById_WhenKeycloakFails_ShouldThrowException() {
        when(usersResource.get(testUserId.toString())).thenThrow(new RuntimeException("Error fetching user"));

        assertThrows(RuntimeException.class, () -> userService.getUserById(testUserId));
    }

    @Test
    void getUserById_WhenKeycloakResponseIsEmpty_ShouldThrowException() {
        when(usersResource.get(testUserId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> userService.getUserById(testUserId));
    }
}