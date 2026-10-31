package com.payvault.user;

import com.payvault.user.dto.UserRequest;
import com.payvault.user.dto.UserResponse;
import com.payvault.user.entity.User;
import com.payvault.user.exception.UserNotFoundException;
import com.payvault.user.repository.UserRepository;
import com.payvault.user.service.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceApplicationTests {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        userService = new UserServiceImpl(
                userRepository
        );
    }

    // ---------------------------------------------------------
    // 1. Create User - Success
    // ---------------------------------------------------------

    @Test
    void createUserShouldCreateSuccessfully() {

        UserRequest request =
                new UserRequest(
                        "Test User",
                        "test@example.com",
                        "9876543210"
                );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        User savedUser =
                new User(
                        "Test User",
                        "test@example.com",
                        "9876543210"
                );

        savedUser.setId(10L);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        UserResponse response =
                userService.createUser(request);

        assertNotNull(response);

        assertEquals(
                10L,
                response.getId()
        );

        assertEquals(
                "Test User",
                response.getName()
        );

        assertEquals(
                "test@example.com",
                response.getEmail()
        );

        assertEquals(
                "9876543210",
                response.getPhone()
        );

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(userRepository)
                .save(any(User.class));
    }

    // ---------------------------------------------------------
    // 2. Create User - Duplicate Email
    // ---------------------------------------------------------

    @Test
    void createUserShouldRejectDuplicateEmail() {

        UserRequest request =
                new UserRequest(
                        "Test User",
                        "existing@example.com",
                        "9876543210"
                );

        User existingUser =
                new User(
                        "Existing User",
                        "existing@example.com",
                        "9999999999"
                );

        when(userRepository.findByEmail(
                "existing@example.com"
        )).thenReturn(Optional.of(existingUser));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.createUser(request)
                );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail("existing@example.com");

        verify(userRepository, never())
                .save(any(User.class));
    }

    // ---------------------------------------------------------
    // 3. Get User - Success
    // ---------------------------------------------------------

    @Test
    void getUserByIdShouldReturnUser() {

        User user =
                new User(
                        "Sai",
                        "sai@test.com",
                        "9876543210"
                );

        user.setId(2L);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getUserById(2L);

        assertNotNull(response);

        assertEquals(
                2L,
                response.getId()
        );

        assertEquals(
                "Sai",
                response.getName()
        );

        assertEquals(
                "sai@test.com",
                response.getEmail()
        );

        assertEquals(
                "9876543210",
                response.getPhone()
        );

        verify(userRepository)
                .findById(2L);
    }

    // ---------------------------------------------------------
    // 4. Get User - Not Found
    // ---------------------------------------------------------

    @Test
    void getUserByIdShouldThrowWhenNotFound() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        UserNotFoundException exception =
                assertThrows(
                        UserNotFoundException.class,
                        () -> userService.getUserById(999L)
                );

        assertEquals(
                "User not found with id: 999",
                exception.getMessage()
        );

        verify(userRepository)
                .findById(999L);
    }

    // ---------------------------------------------------------
    // 5. Get All Users
    // ---------------------------------------------------------

    @Test
    void getAllUsersShouldReturnUsers() {

        User user1 =
                new User(
                        "Sai",
                        "sai@test.com",
                        "9876543210"
                );

        user1.setId(2L);

        User user2 =
                new User(
                        "Rahul",
                        "rahul@test.com",
                        "9876543211"
                );

        user2.setId(3L);

        when(userRepository.findAll())
                .thenReturn(List.of(user1, user2));

        List<UserResponse> response =
                userService.getAllUsers();

        assertNotNull(response);

        assertEquals(
                2,
                response.size()
        );

        assertEquals(
                "Sai",
                response.get(0).getName()
        );

        assertEquals(
                "Rahul",
                response.get(1).getName()
        );

        verify(userRepository)
                .findAll();
    }

    // ---------------------------------------------------------
    // 6. Update User - Success
    // ---------------------------------------------------------

    @Test
    void updateUserShouldUpdateSuccessfully() {

        UserRequest request =
                new UserRequest(
                        "Updated Sai",
                        "updated@test.com",
                        "9999999999"
                );

        User existingUser =
                new User(
                        "Sai",
                        "sai@test.com",
                        "9876543210"
                );

        existingUser.setId(2L);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        UserResponse response =
                userService.updateUser(
                        2L,
                        request
                );

        assertNotNull(response);

        assertEquals(
                2L,
                response.getId()
        );

        assertEquals(
                "Updated Sai",
                response.getName()
        );

        assertEquals(
                "updated@test.com",
                response.getEmail()
        );

        assertEquals(
                "9999999999",
                response.getPhone()
        );

        verify(userRepository)
                .findById(2L);

        verify(userRepository)
                .save(existingUser);
    }

    // ---------------------------------------------------------
    // 7. Update User - Not Found
    // ---------------------------------------------------------

    @Test
    void updateUserShouldThrowWhenNotFound() {

        UserRequest request =
                new UserRequest(
                        "Updated User",
                        "updated@test.com",
                        "9999999999"
                );

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        UserNotFoundException exception =
                assertThrows(
                        UserNotFoundException.class,
                        () -> userService.updateUser(
                                999L,
                                request
                        )
                );

        assertEquals(
                "User not found with id: 999",
                exception.getMessage()
        );

        verify(userRepository)
                .findById(999L);

        verify(userRepository, never())
                .save(any(User.class));
    }

    // ---------------------------------------------------------
    // 8. Delete User - Success
    // ---------------------------------------------------------

    @Test
    void deleteUserShouldDeleteSuccessfully() {

        User user =
                new User(
                        "Sai",
                        "sai@test.com",
                        "9876543210"
                );

        user.setId(2L);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        userService.deleteUser(2L);

        verify(userRepository)
                .findById(2L);

        verify(userRepository)
                .delete(user);
    }

    // ---------------------------------------------------------
    // 9. Delete User - Not Found
    // ---------------------------------------------------------

    @Test
    void deleteUserShouldThrowWhenNotFound() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        UserNotFoundException exception =
                assertThrows(
                        UserNotFoundException.class,
                        () -> userService.deleteUser(999L)
                );

        assertEquals(
                "User not found with id: 999",
                exception.getMessage()
        );

        verify(userRepository)
                .findById(999L);

        verify(userRepository, never())
                .delete(any(User.class));
    }
}