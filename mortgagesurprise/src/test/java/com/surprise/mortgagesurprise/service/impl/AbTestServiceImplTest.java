package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.UserAbTestVariant;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.repository.jpa.UserAbTestVariantRepository;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.repository.mongo.UserActionAuditMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.surprise.mortgagesurprise.util.AbTestVariantUtil.calculateDeterministicVariant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbTestServiceImplTest {

    @Mock
    private UserAbTestVariantRepository variantRepository;

    @Mock
    private UserActionAuditMongoRepository auditRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AbTestServiceImpl abTestService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getOrCreateVariant should return existing variant if already assigned in DB")
    void shouldReturnExistingVariant() {
        UserAbTestVariant existing = new UserAbTestVariant(userId, "mortgage_calculate_button", "emerald");
        when(variantRepository.findByUserIdAndExperimentName(userId, "mortgage_calculate_button"))
                .thenReturn(Optional.of(existing));

        var response = abTestService.getOrCreateVariant(userId, "mortgage_calculate_button");

        assertThat(response.getColor()).isEqualTo("emerald");
        verify(variantRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateVariant should deterministically compute variant via hash and salt and save to DB")
    void shouldCreateDeterministicVariant() {
        String expectedColor = calculateDeterministicVariant(userId.toString(), "any");
        var userAbTestVariant = new UserAbTestVariant(userId, "any", expectedColor);
        when(variantRepository.findByUserIdAndExperimentName(any(), anyString()))
                .thenReturn(Optional.empty());
        when(variantRepository.save(any())).thenReturn(userAbTestVariant);

        var response = abTestService.getOrCreateVariant(userId, "any");


        assertThat(response.getColor()).isEqualTo(expectedColor);
        assertThat(response.getUserId()).isEqualTo(userId);
        verify(variantRepository).save(any(UserAbTestVariant.class));
    }

    @Test
    @DisplayName("calculateDeterministicVariant should always return identical variant for same input (idempotent)")
    void shouldReturnConsistentHashVariant() {
        String variantA1 = calculateDeterministicVariant(userId.toString(), "mortgage_calculate_button");
        String variantA2 = calculateDeterministicVariant(userId.toString(), "mortgage_calculate_button");

        assertThat(variantA1).isEqualTo(variantA2);
        assertThat(List.of("blue", "emerald")).contains(variantA1);
    }

    @Test
    @DisplayName("getStats should compare userIds with audit logs and count clicks per user")
    void shouldCompareUserIdsWithAuditAndCountClicks() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        UserAbTestVariant v1 = new UserAbTestVariant(user1, "mortgage_calculate_button", "blue");
        UserAbTestVariant v2 = new UserAbTestVariant(user2, "mortgage_calculate_button", "emerald");

        when(variantRepository.findByExperimentName("mortgage_calculate_button")).thenReturn(List.of(v1, v2));

        UserActionAuditDocument audit1 = new UserActionAuditDocument();
        audit1.setUserId(user1);
        audit1.setUserEmail("u1@bank.com");
        audit1.setActionType(UserAuditActionType.MORTGAGE_CALCULATION);

        UserActionAuditDocument audit2 = new UserActionAuditDocument();
        audit2.setUserId(user1);
        audit2.setUserEmail("u1@bank.com");
        audit2.setActionType(UserAuditActionType.MORTGAGE_CALCULATION);

        when(auditRepository.findByActionTypeOrderByTimestampDesc(UserAuditActionType.MORTGAGE_CALCULATION))
                .thenReturn(List.of(audit1, audit2));

        AbTestStatsResponse stats = abTestService.getStats("mortgage_calculate_button");

        assertThat(stats.totalAssignedUsers()).isEqualTo(2);
        assertThat(stats.totalClicks()).isEqualTo(2);
        assertThat(stats.variants()).hasSize(2);
    }
}
