package com.surprise.mortgagesurprise.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.service.PdfGeneratorService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

    @Override
    public byte[] generatePaymentSchedulePdf(String scenarioTitle, PaymentScheduleDocument schedule) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFontRegular = getBaseFont(false);
            BaseFont baseFontBold = getBaseFont(true);

            Font titleFont = new Font(baseFontBold, 16, Font.NORMAL, Color.DARK_GRAY);
            Paragraph title = new Paragraph("График платежей: " + (scenarioTitle != null ? scenarioTitle : "Ипотечный расчет"), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            String curr = schedule.getCurrency() != null ? schedule.getCurrency().name() : "RUB";
            Font summaryFont = new Font(baseFontRegular, 10, Font.NORMAL, Color.BLACK);
            Paragraph summary = new Paragraph(
                    String.format("Сумма кредита: %s %s | Ставка: %s%% | Срок: %d мес. | Ежемесячный платеж: %s %s | Переплата: %s %s",
                            schedule.getLoanAmount(), curr,
                            schedule.getAnnualInterestRate(),
                            schedule.getActualTermMonths(),
                            schedule.getMonthlyPayment(), curr,
                            schedule.getTotalInterest(), curr),
                    summaryFont
            );
            summary.setAlignment(Element.ALIGN_CENTER);
            summary.setSpacingAfter(15);
            document.add(summary);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.0f, 2.5f, 2.5f, 2.5f, 2.0f, 2.5f, 2.5f});

            String[] headers = {"№", "Дата", "Остаток начало", "Осн. долг", "Проценты", "Досрочно", "Всего платеж", "Остаток конец"};
            Font headerFont = new Font(baseFontBold, 9, Font.NORMAL, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(41, 128, 185)); // Синий фон
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font dataFont = new Font(baseFontRegular, 8, Font.NORMAL, Color.BLACK);
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

    private BaseFont getBaseFont(boolean bold) {
        String resourcePath = bold ? "/fonts/arialbd.ttf" : "/fonts/arial.ttf";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                byte[] fontBytes = is.readAllBytes();
                return BaseFont.createFont(bold ? "arialbd.ttf" : "arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
            }
        } catch (Exception ignored) {
        }
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить шрифт PDF: " + e.getMessage(), e);
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
