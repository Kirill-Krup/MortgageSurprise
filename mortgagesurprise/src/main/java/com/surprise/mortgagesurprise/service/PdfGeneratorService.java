package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;

public interface PdfGeneratorService {
    byte[] generatePaymentSchedulePdf(String scenarioTitle, PaymentScheduleDocument schedule);
}
