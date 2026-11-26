package com.surprise.mortgagesurprise.service;

import com.surprise.common.dto.simulator.BlacklistCheckResponse;

import java.util.Optional;
import java.util.UUID;

public interface BankSimulatorIntegrationService {

    /**
     * Проверяет, находится ли пользователь в чёрном списке banksimulator.
     */
    Optional<BlacklistCheckResponse> checkBlacklist(UUID userId);

    /**
     * Отправляет запрос на создание субъекта в banksimulator при оформлении первого микрозайма.
     */
    void ensureSubjectCreatedForFirstMicroloan(UUID userId);

    /**
     * Отправляет событие погашения микрозайма (вовремя или с просрочкой) для пересчёта trustScore.
     */
    void recordMicroloanRepayment(UUID userId, String applicationId, boolean overdue, long overdueDays);
}
