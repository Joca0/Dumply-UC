package com.dumply.service;

import com.dumply.common.dto.LoginRequestDTO;
import com.dumply.common.exception.AccountBlockedException;
import com.dumply.model.User;
import com.dumply.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceBruteForceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.dumply.config.security.TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldIncrementFailedAttemptsOnWrongPassword() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("encodedPassword");
        user.setFailedLoginAttempts(0);

        LoginRequestDTO request = new LoginRequestDTO("test@test.com", "wrongPassword");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertEquals(1, user.getFailedLoginAttempts());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void shouldLockAccountAfterMaxAttempts() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("encodedPassword");
        user.setFailedLoginAttempts(4); // Próxima falha bloqueia (limite é 5)

        LoginRequestDTO request = new LoginRequestDTO("test@test.com", "wrongPassword");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLocktime());
        assertFalse(user.isAccountNonLocked());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void shouldThrowExceptionIfAccountIsLocked() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setLocktime(LocalDateTime.now().plusMinutes(10));

        LoginRequestDTO request = new LoginRequestDTO("test@test.com", "anyPassword");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        assertThrows(AccountBlockedException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldResetAttemptsOnSuccessfulLogin() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("encodedPassword");
        user.setFailedLoginAttempts(3);

        LoginRequestDTO request = new LoginRequestDTO("test@test.com", "correctPassword");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

        authService.login(request);

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLocktime());
        assertTrue(user.isAccountNonLocked());
        verify(userRepository, times(1)).save(user);
    }
}
