package com.bank.simulator.service;

import com.surprise.common.dto.simulator.RegisterEventRequest;
import com.surprise.common.dto.simulator.TrustEventResponse;

public interface TrustEventService {
    TrustEventResponse registerEvent(RegisterEventRequest request);
}
