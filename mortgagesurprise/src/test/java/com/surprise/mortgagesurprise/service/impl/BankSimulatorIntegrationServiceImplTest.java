package com.surprise.mortgagesurprise.service.impl;

import com.surprise.common.dto.simulator.*;
import com.surprise.mortgagesurprise.client.BankSimulatorSubjectClient;
import com.surprise.mortgagesurprise.client.BankSimulatorTrustEventClient;
import com.surprise.mortgagesurprise.model.entity.UserPassportData;
import com.surprise.mortgagesurprise.repository.jpa.UserPassportDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankSimulatorIntegrationServiceImplTest {

    @Mock
    private BankSimulatorSubjectClient subjectClient;

    @Mock
    private BankSimulatorTrustEventClient trustEventClient;

    @Mock
    private UserPassportDataRepository passportDataRepository;

    @InjectMocks
    private BankSimulatorIntegrationServiceImpl service;

    @Test
    @DisplayName("Should create subject in banksimulator when user has passport data")
    void ensureSubjectCreated_whenPassportExists_callsSubjectClient() {
        UUID userId = UUID.randomUUID();
        UserPassportData passport = new UserPassportData();
        passport.setPassportHash("abc123hash");
        when(passportDataRepository.findByUserId(userId)).thenReturn(Optional.of(passport));

        service.ensureSubjectCreatedForFirstMicroloan(userId);

        ArgumentCaptor<CreateSubjectRequest> captor = ArgumentCaptor.forClass(CreateSubjectRequest.class);
        verify(subjectClient).createSubject(captor.capture());
        assertThat(captor.getValue().documentType()).isEqualTo(DocumentType.PASSPORT);
        assertThat(captor.getValue().documentHash()).isEqualTo("abc123hash");
        verifyNoInteractions(trustEventClient);
    }

    @Test
    @DisplayName("Should skip subject creation when user has no passport data")
    void ensureSubjectCreated_whenPassportMissing_doesNotCallClient() {
        UUID userId = UUID.randomUUID();
        when(passportDataRepository.findByUserId(userId)).thenReturn(Optional.empty());

        service.ensureSubjectCreatedForFirstMicroloan(userId);

        verifyNoInteractions(subjectClient);
        verifyNoInteractions(trustEventClient);
    }

    @Test
    @DisplayName("Should send PAYMENT_ON_TIME event when microloan is repaid on time")
    void recordRepayment_onTime_sendsPaymentOnTimeEvent() {
        UUID userId = UUID.randomUUID();
        UserPassportData passport = new UserPassportData();
        passport.setPassportHash("abc123hash");
        when(passportDataRepository.findByUserId(userId)).thenReturn(Optional.of(passport));
        when(trustEventClient.registerEvent(any())).thenReturn(
                TrustEventResponse.builder().delta(15).scoreAfter(715).build()
        );

        service.recordMicroloanRepayment(userId, "MS-10001", false, 0);

        ArgumentCaptor<RegisterEventRequest> captor = ArgumentCaptor.forClass(RegisterEventRequest.class);
        verify(trustEventClient).registerEvent(captor.capture());
        assertThat(captor.getValue().documentHash()).isEqualTo("abc123hash");
        assertThat(captor.getValue().eventType()).isEqualTo(TrustEventType.PAYMENT_ON_TIME);
        assertThat(captor.getValue().eventKey()).isEqualTo("microloan-repay-MS-10001-PAYMENT_ON_TIME");
        assertThat(captor.getValue().source()).isEqualTo("mortgagesurprise");
        verifyNoInteractions(subjectClient);
    }

    @Test
    @DisplayName("Should send PAYMENT_DELAY event when microloan is repaid with short overdue")
    void recordRepayment_shortOverdue_sendsPaymentDelayEvent() {
        UUID userId = UUID.randomUUID();
        UserPassportData passport = new UserPassportData();
        passport.setPassportHash("abc123hash");
        when(passportDataRepository.findByUserId(userId)).thenReturn(Optional.of(passport));
        when(trustEventClient.registerEvent(any())).thenReturn(
                TrustEventResponse.builder().delta(-20).scoreAfter(680).build()
        );

        service.recordMicroloanRepayment(userId, "MS-10002", true, 5);

        ArgumentCaptor<RegisterEventRequest> captor = ArgumentCaptor.forClass(RegisterEventRequest.class);
        verify(trustEventClient).registerEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(TrustEventType.PAYMENT_DELAY);
    }

    @Test
    @DisplayName("Should send OVERDUE_30_DAYS event when microloan is overdue >= 30 days")
    void recordRepayment_overdue30Days_sendsOverdue30DaysEvent() {
        UUID userId = UUID.randomUUID();
        UserPassportData passport = new UserPassportData();
        passport.setPassportHash("abc123hash");
        when(passportDataRepository.findByUserId(userId)).thenReturn(Optional.of(passport));
        when(trustEventClient.registerEvent(any())).thenReturn(
                TrustEventResponse.builder().delta(-50).scoreAfter(650).build()
        );

        service.recordMicroloanRepayment(userId, "MS-10003", true, 35);

        ArgumentCaptor<RegisterEventRequest> captor = ArgumentCaptor.forClass(RegisterEventRequest.class);
        verify(trustEventClient).registerEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(TrustEventType.OVERDUE_30_DAYS);
    }
}
