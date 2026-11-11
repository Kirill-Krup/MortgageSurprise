package com.surprise.mortgagesurprise.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.service.PdfGeneratorService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

    @Override
    public byte[] generatePaymentSchedulePdf(String scenarioTitle, PaymentScheduleDocument schedule) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
            Paragraph title = new Paragraph("График платежей: " + (scenarioTitle != null ? scenarioTitle : "Ипотечный расчет"), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Paragraph summary = new Paragraph(
                    String.format("Сумма кредита: %s руб. | Ставка: %s%% | Срок: %d мес. | Ежемесячный платеж: %s руб. | Переплата: %s руб.",
                            schedule.getLoanAmount(),
                            schedule.getAnnualInterestRate(),
                            schedule.getActualTermMonths(),
                            schedule.getMonthlyPayment(),
                            schedule.getTotalInterest()),
                    summaryFont
            );
            summary.setAlignment(Element.ALIGN_CENTER);
            summary.setSpacingAfter(15);
            document.add(summary);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.0f, 2.5f, 2.5f, 2.5f, 2.0f, 2.5f, 2.5f});

            String[] headers = {"№", "Дата", "Остаток начало", "Осн. долг", "Проценты", "Досрочно", "Всего платеж", "Остаток конец"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(41, 128, 185)); // Синий фон
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            boolean alternate = false;
            Color altColor = new Color(245, 247, 250);

            if (schedule.getItems() != null) {
                for (PaymentScheduleItem item : schedule.getItems()) {
                    Color bg = alternate ? altColor : Color.WHITE;
                    addCell(table, String.valueOf(item.monthNumber()), dataFont, Element.ALIGN_CENTER, bg);
                    addCell(table, item.paymentDate() != null ? item.paymentDate().toString() : "-", dataFont, Element.ALIGN_CENTER, bg);
                    addCell(table, item.openingBalance() != null ? item.openingBalance().toString() : "0.00", dataFont, Element.ALIGN_RIGHT, bg);
                    addCell(table, item.principalPayment() != null ? item.principalPayment().toString() : "0.00", dataFont, Element.ALIGN_RIGHT, bg);
                    addCell(table, item.interestPayment() != null ? item.interestPayment().toString() : "0.00", dataFont, Element.ALIGN_RIGHT, bg);
                    addCell(table, item.earlyPayment() != null ? item.earlyPayment().toString() : "0.00", dataFont, Element.ALIGN_RIGHT, bg);
                    addCell(table, item.totalMonthlyPayment() != null ? item.totalMonthlyPayment().toString() : "0.00", dataFont, Element.ALIGN_RIGHT, bg);
                    addCell(table, item.closingBalance() != null ? item.closingBalance().toString() : "0.00", dataFont, Element.ALIGN_RIGHT, bg);
                    alternate = !alternate;
                }
            }

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при генерации PDF графика платежей: " + e.getMessage(), e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        table.addCell(cell);
    }
}