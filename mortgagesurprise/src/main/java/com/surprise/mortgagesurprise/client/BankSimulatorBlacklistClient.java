package com.surprise.mortgagesurprise.client;

import com.surprise.common.dto.simulator.BlacklistCheckRequest;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign-клиент для BlacklistCheckController (/api/v1/blacklist) сервиса banksimulator.
 */
@FeignClient(
        name = "bank-simulator-blacklist",
        url = "${bank.simulator.url}"
)
public interface BankSimulatorBlacklistClient {

    @PostMapping("/blacklist/check")
    BlacklistCheckResponse check(@RequestBody BlacklistCheckRequest request);
}
