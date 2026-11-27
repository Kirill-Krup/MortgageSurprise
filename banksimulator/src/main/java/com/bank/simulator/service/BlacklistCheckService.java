package com.bank.simulator.service;

import com.surprise.common.dto.simulator.BlacklistCheckRequest;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;

public interface BlacklistCheckService {
    BlacklistCheckResponse check(BlacklistCheckRequest request);
}
