package com.surprise.mortgagesurprise.client;

import com.surprise.common.dto.simulator.CreateSubjectRequest;
import com.surprise.common.dto.simulator.SubjectResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign-клиент для SubjectController (/api/v1/subjects) сервиса banksimulator.
 */
@FeignClient(
        name = "bank-simulator-subjects",
        url = "${bank.simulator.url}"
)
public interface BankSimulatorSubjectClient {

    @PostMapping("/subjects")
    SubjectResponse createSubject(@RequestBody CreateSubjectRequest request);

    @GetMapping("/subjects/by-document/{documentHash}")
    SubjectResponse getSubjectByDocumentHash(@PathVariable("documentHash") String documentHash);
}
