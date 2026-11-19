package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PdfGeneratorServiceImpl Tests")
class PdfGeneratorServiceImplTest {

    private PdfGeneratorServiceImpl pdfGeneratorService;

    @BeforeEach
    void setUp() {
        pdfGeneratorService = new PdfGeneratorServiceImpl();
    }

    @Nested
    @DisplayName("PDF Generation")
    class GeneratePdf {

        @Test
        @DisplayName("Should generate valid non-empty PDF byte array with PDF file header signature")
        void generatePaymentSchedulePdf_shouldGenerateValidPdfBytes_whenScheduleDocumentProvided() {
            // Arrange
            PaymentScheduleItem item1 = new PaymentScheduleItem(
                    1,
                    LocalDate.of(2026, 1, 1),
                    TestConstants.PROPERTY_PRICE,
                    new BigDecimal("25000.00"),
                    new BigDecimal("15000.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("40000.00"),
                    new BigDecimal("4975000.00")
            );

            PaymentScheduleDocument document = new PaymentScheduleDocument(
                    TestConstants.SCHEDULE_ID,
                    TestConstants.CALCULATION_HASH,
                    new BigDecimal("6000000.00"),
                    new BigDecimal("1000000.00"),
                    new BigDecimal("5000000.00"),
                    240,
                    240,
                    new BigDecimal("9.5"),
                    PaymentType.ANNUITY,
                    new BigDecimal("40000.00"),
                    new BigDecimal("2500000.00"),
                    new BigDecimal("7500000.00"),
                    List.of(item1),
                    Instant.now()
            );

            // Act
            byte[] pdfBytes = pdfGeneratorService.generatePaymentSchedulePdf("Моя Ипотека", document);

            // Assert
            assertThat(pdfBytes)
                    .isNotNull()
                    .isNotEmpty();
            assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        }
    }
}
