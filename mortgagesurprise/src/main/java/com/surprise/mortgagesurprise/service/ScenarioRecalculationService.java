package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;

/**
 * Single responsibility: Recalculate affected mortgage scenarios and notify users upon bank rate changes.
 */
public interface ScenarioRecalculationService {

    void processRateChange(RateChangedEvent event);
}
