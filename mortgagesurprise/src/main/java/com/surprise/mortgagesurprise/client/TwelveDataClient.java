package com.surprise.mortgagesurprise.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.JsonNode;

@FeignClient(
        name = "twelve-data-api",
        contextId = "twelve-data-client",
        url = "${twelvedata.base-url:https://api.twelvedata.com}"
)
public interface TwelveDataClient {

    @GetMapping("/price")
    JsonNode getPrice(@RequestParam("symbol") String symbol, @RequestParam("apikey") String apiKey);
}
