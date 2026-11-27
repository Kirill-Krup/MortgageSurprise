package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.dto.abtest.VariantStat;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.AbTestVariant;
import com.surprise.mortgagesurprise.model.entity.UserAbTestVariant;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.repository.jpa.AbTestVariantsRepository;
import com.surprise.mortgagesurprise.repository.jpa.UserAbTestVariantRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbTestServiceImplTest {

    private static final String EXPERIMENT = "mortgage_calculate_button";

    @Mock
    private UserAbTestVariantRepository userVariantRepository;

    @Mock
    private AbTestVariantsRepository variantsRepository;

    @Mock
    private UserActionAuditMongoRepository auditRepository;

    @InjectMocks
    private AbTestServiceImpl abTestService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private static AbTestVariant variant(int id, String name, int percent) {
        var v = new AbTestVariant(EXPERIMENT, name, percent, 0);
        v.setId(id);
        return v;
    }

    @Test
    @DisplayName("getOrCreateVariant returns the existing assignment and does not touch experiment definitions")
    void shouldReturnExistingVariant() {
        var existing = new UserAbTestVariant(userId, variant(2, "emerald", 50));
        when(userVariantRepository.findByUserIdAndExperimentName(userId, EXPERIMENT))
                .thenReturn(Optional.of(existing));

        var response = abTestService.getOrCreateVariant(userId, EXPERIMENT);

        assertThat(response.getAbTestVariant().getVariantName()).isEqualTo("emerald");
        verify(userVariantRepository, never()).save(any());
        verifyNoInteractions(variantsRepository);
    }

    @Test
    @DisplayName("getOrCreateVariant assigns a variant from the latest DB version and persists it")
    void shouldAssignVariantFromDatabaseDefinition() {
        var blue = variant(1, "blue", 100);
        var emerald = variant(2, "emerald", 0);
        when(userVariantRepository.findByUserIdAndExperimentName(userId, EXPERIMENT)).thenReturn(Optional.empty());
        when(variantsRepository.findLatestVersion(EXPERIMENT)).thenReturn(Optional.of(0));
        when(variantsRepository.findAllByExperimentNameAndVersion(EXPERIMENT, 0)).thenReturn(Set.of(blue, emerald));
        when(userVariantRepository.save(any(UserAbTestVariant.class))).thenAnswer(i -> i.getArgument(0));

        var response = abTestService.getOrCreateVariant(userId, "  " + EXPERIMENT + "  ");

        assertThat(response.getAbTestVariant()).isSameAs(blue);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getExperimentName()).isEqualTo(EXPERIMENT);
        verify(userVariantRepository).save(any(UserAbTestVariant.class));
    }

    @Test
    @DisplayName("getOrCreateVariant fails with 404 for an experiment that is not defined in the DB")
    void shouldFailForUnknownExperiment() {
        when(userVariantRepository.findByUserIdAndExperimentName(userId, "nope")).thenReturn(Optional.empty());
        when(variantsRepository.findLatestVersion("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> abTestService.getOrCreateVariant(userId, "nope"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateVariant fails when probabilities do not cover the whole bucket range")
    void shouldFailWhenDistributionIsBroken() {
        when(userVariantRepository.findByUserIdAndExperimentName(userId, EXPERIMENT)).thenReturn(Optional.empty());
        when(variantsRepository.findLatestVersion(EXPERIMENT)).thenReturn(Optional.of(0));
        when(variantsRepository.findAllByExperimentNameAndVersion(EXPERIMENT, 0))
                .thenReturn(Set.of(variant(1, "blue", 0)));

        assertThatThrownBy(() -> abTestService.getOrCreateVariant(userId, EXPERIMENT))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.AB_TEST_INVALID_DISTRIBUTION));
        verify(userVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("getStats counts clicks per user and lists every defined variant")
    void shouldCompareUserIdsWithAuditAndCountClicks() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        var blue = variant(1, "blue", 50);
        var emerald = variant(2, "emerald", 50);

        when(userVariantRepository.findByExperimentName(EXPERIMENT)).thenReturn(List.of(
                new UserAbTestVariant(user1, blue),
                new UserAbTestVariant(user2, emerald)));
        when(variantsRepository.findLatestVersion(EXPERIMENT)).thenReturn(Optional.of(0));
        when(variantsRepository.findAllByExperimentNameAndVersion(EXPERIMENT, 0)).thenReturn(Set.of(blue, emerald));

        UserActionAuditDocument audit1 = new UserActionAuditDocument();
        audit1.setUserId(user1);
        audit1.setActionType(UserAuditActionType.MORTGAGE_CALCULATION);
        UserActionAuditDocument audit2 = new UserActionAuditDocument();
        audit2.setUserId(user1);
        audit2.setActionType(UserAuditActionType.MORTGAGE_CALCULATION);
        when(auditRepository.findByActionTypeOrderByTimestampDesc(UserAuditActionType.MORTGAGE_CALCULATION))
                .thenReturn(List.of(audit1, audit2));

        AbTestStatsResponse stats = abTestService.getStats(EXPERIMENT);

        assertThat(stats.totalAssignedUsers()).isEqualTo(2);
        assertThat(stats.totalClicks()).isEqualTo(2);
        assertThat(stats.variants()).extracting(VariantStat::color).containsExactly("blue", "emerald");
        assertThat(stats.variants().get(0).totalClicks()).isEqualTo(2);
        assertThat(stats.variants().get(1).totalClicks()).isZero();
    }
}
