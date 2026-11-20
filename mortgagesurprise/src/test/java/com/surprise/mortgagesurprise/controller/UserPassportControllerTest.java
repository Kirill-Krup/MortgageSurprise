package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.profile.PassportDataDTO;
import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.UserPassportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPassportControllerTest {

    @Mock
    private UserPassportService userPassportService;

    @InjectMocks
    private UserPassportController controller;

    private CustomUserDetails userDetails;
    private UUID userId;
    private PassportDataDTO sampleDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = new User("test@example.com", "pass", "Ivan", "Ivanov", Role.ROLE_USER);
        user.setId(userId);
        userDetails = new CustomUserDetails(user);

        sampleDto = new PassportDataDTO(
                "Иванов Иван Иванович",
                "4515 123456",
                "2020-05-15",
                "770-001",
                "ОВД Тверского района г. Москвы"
        );
    }

    @Test
    @DisplayName("getPassportData should return 200 with data if present")
    void shouldReturnPassportDataIfPresent() {
        when(userPassportService.getPassportData(userId)).thenReturn(Optional.of(sampleDto));

        ResponseEntity<PassportDataDTO> response = controller.getPassportData(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleDto);
    }

    @Test
    @DisplayName("getPassportData should return 204 if empty")
    void shouldReturn204IfNoPassportData() {
        when(userPassportService.getPassportData(userId)).thenReturn(Optional.empty());

        ResponseEntity<PassportDataDTO> response = controller.getPassportData(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("savePassportData should save and return 200 with dto")
    void shouldSavePassportData() {
        when(userPassportService.saveOrUpdatePassportData(userId, sampleDto)).thenReturn(sampleDto);

        ResponseEntity<PassportDataDTO> response = controller.savePassportData(userDetails, sampleDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleDto);
        verify(userPassportService).saveOrUpdatePassportData(userId, sampleDto);
    }

    @Test
    @DisplayName("deletePassportData should delete and return 204")
    void shouldDeletePassportData() {
        ResponseEntity<Void> response = controller.deletePassportData(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userPassportService).deletePassportData(userId);
    }
}
