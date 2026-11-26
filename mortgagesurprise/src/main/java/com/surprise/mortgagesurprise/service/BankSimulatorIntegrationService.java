package com.surprise.mortgagesurprise.service;

import java.util.UUID;

public interface BankSimulatorIntegrationService {

    /**
     * Отправляет запрос на создание субъекта в banksimulator при оформлении первого микрозайма.
     */
    void ensureSubjectCreatedForFirstMicroloan(UUID userId);

    /**
     * Отправляет событие погашения микрозайма (вовремя или с просрочкой) для пересчёта trustScore.
     */
    void recordMicroloanRepayment(UUID userId, String applicationId, boolean overdue, long overdueDays);
}
