package com.surprise.mortgagesurprise.client;

import com.surprise.common.dto.simulator.RegisterEventRequest;
import com.surprise.common.dto.simulator.TrustEventResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign-клиент для TrustEventController (/api/v1/events) сервиса banksimulator.
 */
@FeignClient(
        name = "bank-simulator-events",
        url = "${bank.simulator.url}"
)
public interface BankSimulatorTrustEventClient {

    @PostMapping("/events")
    TrustEventResponse registerEvent(@RequestBody RegisterEventRequest request);
}
