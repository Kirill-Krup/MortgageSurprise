package com.surprise.mortgagesurprise.service;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeRateService{
    Map<String, BigDecimal> getRates();
}
