package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import com.dpnevsky.creditcalculator.calculator.service.util.ServiceForCalculate;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GenerateApplicationContractPdfService {

    private static final String CREDIT_AGREEMENT_DOCUMENT_TYPE = "CREDIT_AGREEMENT";
    private static final String PAYMENT_TYPE_ANNUITY = "ANNUITY";
    private static final String PAYMENT_TYPE_DIFFERENTIAL = "DIFFERENTIAL";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Locale RU_LOCALE = Locale.forLanguageTag("ru-RU");
    private static final String PDF_FONT_FAMILY = "PdfArial";
    private static final List<String> PDF_FONT_CANDIDATE_PATHS = List.of(
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/arialuni.ttf",
            "C:/Windows/Fonts/segoeui.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
            "/usr/share/fonts/opentype/noto/NotoSans-Regular.ttf",
            "/Library/Fonts/Arial Unicode.ttf",
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
    );

    private final ApplicationRepository applicationRepository;
    private final OfferRepository offerRepository;

    public GenerateApplicationContractPdfService(
            ApplicationRepository applicationRepository,
            OfferRepository offerRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.offerRepository = offerRepository;
    }

    public boolean supports(String documentType, String format) {
        return CREDIT_AGREEMENT_DOCUMENT_TYPE.equals(documentType) && "PDF".equalsIgnoreCase(format);
    }

    public byte[] generate(UUID applicationId, OffsetDateTime generatedAt) {
        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("Application not found. applicationId=" + applicationId));

        OfferEntity offer = offerRepository.findFirstByApplicationIdAndSelectedTrue(applicationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Selected offer not found for contract generation. applicationId=" + applicationId
                ));

        String paymentType = normalizePaymentType(application.getPaymentType());
        List<PaymentScheduleRow> schedule = buildPaymentSchedule(
                offer.getTotalAmount(),
                offer.getTermMonths(),
                offer.getRate(),
                generatedAt.toLocalDate(),
                paymentType
        );

        BigDecimal totalPayment = schedule.stream()
                .map(PaymentScheduleRow::paymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalInterest = schedule.stream()
                .map(PaymentScheduleRow::interestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal psk = calculatePsk(offer, schedule, paymentType);

        ContractData contractData = new ContractData(
                buildBorrowerName(application),
                buildPassportDisplay(application),
                offer.getTotalAmount().setScale(2, RoundingMode.HALF_UP),
                offer.getTermMonths(),
                offer.getRate().setScale(2, RoundingMode.HALF_UP),
                schedule.get(0).paymentAmount().setScale(2, RoundingMode.HALF_UP),
                totalPayment,
                totalInterest,
                psk,
                getPaymentTypeLabel(paymentType),
                generatedAt.toLocalDate(),
                schedule.get(0).paymentDate(),
                schedule.get(schedule.size() - 1).paymentDate()
        );

        String html = buildContractHtml(contractData, schedule, paymentType);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            configureFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate application contract PDF", exception);
        }
    }

    private List<PaymentScheduleRow> buildPaymentSchedule(
            BigDecimal creditAmount,
            Integer termMonths,
            BigDecimal annualRate,
            LocalDate generatedDate,
            String paymentType
    ) {
        return PAYMENT_TYPE_DIFFERENTIAL.equals(paymentType)
                ? buildDifferentiatedSchedule(creditAmount, termMonths, annualRate, generatedDate)
                : buildAnnuitySchedule(creditAmount, termMonths, annualRate, generatedDate);
    }

    private List<PaymentScheduleRow> buildAnnuitySchedule(
            BigDecimal creditAmount,
            Integer termMonths,
            BigDecimal annualRate,
            LocalDate generatedDate
    ) {
        List<PaymentScheduleRow> rows = new ArrayList<>();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal balance = creditAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal standardPayment = ServiceForCalculate.calculateMonthlyPayment(creditAmount, annualRate, termMonths)
                .setScale(2, RoundingMode.HALF_UP);

        for (int month = 1; month <= termMonths; month += 1) {
            BigDecimal interest = monthlyRate.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

            BigDecimal paymentAmount = standardPayment;
            BigDecimal principalAmount = paymentAmount.subtract(interest).setScale(2, RoundingMode.HALF_UP);

            if (month == termMonths || principalAmount.compareTo(balance) > 0) {
                principalAmount = balance;
                paymentAmount = principalAmount.add(interest).setScale(2, RoundingMode.HALF_UP);
            }

            balance = balance.subtract(principalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            rows.add(new PaymentScheduleRow(
                    month,
                    generatedDate.plusMonths(month),
                    paymentAmount,
                    principalAmount,
                    interest,
                    balance
            ));
        }

        return rows;
    }

    private List<PaymentScheduleRow> buildDifferentiatedSchedule(
            BigDecimal creditAmount,
            Integer termMonths,
            BigDecimal annualRate,
            LocalDate generatedDate
    ) {
        List<PaymentScheduleRow> rows = new ArrayList<>();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal balance = creditAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal principalPart = creditAmount
                .divide(BigDecimal.valueOf(termMonths), 10, RoundingMode.HALF_UP);

        for (int month = 1; month <= termMonths; month += 1) {
            BigDecimal interest = monthlyRate.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalAmount = month == termMonths
                    ? balance
                    : principalPart.setScale(2, RoundingMode.HALF_UP);
            BigDecimal paymentAmount = principalAmount.add(interest).setScale(2, RoundingMode.HALF_UP);

            balance = balance.subtract(principalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            rows.add(new PaymentScheduleRow(
                    month,
                    generatedDate.plusMonths(month),
                    paymentAmount,
                    principalAmount,
                    interest,
                    balance
            ));
        }

        return rows;
    }

    private BigDecimal calculatePsk(OfferEntity offer, List<PaymentScheduleRow> schedule, String paymentType) {
        if (PAYMENT_TYPE_DIFFERENTIAL.equals(paymentType)) {
            return ServiceForCalculate.calculatePSK(
                    offer.getTotalAmount(),
                    schedule.stream().map(PaymentScheduleRow::paymentAmount).toList(),
                    offer.getRate()
            );
        }

        return ServiceForCalculate.calculatePSK(
                offer.getTotalAmount(),
                schedule.get(0).paymentAmount(),
                offer.getTermMonths(),
                offer.getRate()
        );
    }

    private String normalizePaymentType(String paymentType) {
        return PAYMENT_TYPE_DIFFERENTIAL.equalsIgnoreCase(paymentType)
                ? PAYMENT_TYPE_DIFFERENTIAL
                : PAYMENT_TYPE_ANNUITY;
    }

    private String getPaymentTypeLabel(String paymentType) {
        return PAYMENT_TYPE_DIFFERENTIAL.equals(paymentType) ? "Дифференцированный" : "Аннуитетный";
    }

    private String buildBorrowerName(ApplicationEntity application) {
        return Stream.of(application.getLastName(), application.getFirstName(), application.getMiddleName())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse("Заемщик");
    }

    private String buildPassportDisplay(ApplicationEntity application) {
        return "Паспорт " + application.getPassportSeries() + " " + application.getPassportNumber();
    }

    private String buildContractHtml(ContractData contractData, List<PaymentScheduleRow> schedule, String paymentType) {
        String scheduleRows = schedule.stream()
                .map(row -> """
                        <tr>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                        """.formatted(
                        row.monthNumber(),
                        escapeHtml(formatDate(row.paymentDate())),
                        escapeHtml(formatMoney(row.paymentAmount())),
                        escapeHtml(formatMoney(row.principalAmount())),
                        escapeHtml(formatMoney(row.interestAmount())),
                        escapeHtml(formatMoney(row.remainingDebt()))
                ))
                .collect(Collectors.joining());

        String paymentSummary = PAYMENT_TYPE_DIFFERENTIAL.equals(paymentType)
                ? "от " + formatMoney(schedule.get(schedule.size() - 1).paymentAmount()) + " до " + formatMoney(schedule.get(0).paymentAmount())
                : formatMoney(contractData.monthlyPayment());

        return """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                    <meta charset="UTF-8"/>
                    <title>Кредитный договор</title>
                    <style>
                        @page {
                            size: A4;
                            margin: 20mm 16mm 18mm 16mm;
                        }
                        body {
                            font-family: PdfArial, Arial, sans-serif;
                            font-size: 11px;
                            line-height: 1.45;
                            color: #1f2937;
                        }
                        .header {
                            padding-bottom: 12px;
                            border-bottom: 2px solid #2563eb;
                            margin-bottom: 16px;
                        }
                        .eyebrow {
                            font-size: 9px;
                            letter-spacing: 0.16em;
                            text-transform: uppercase;
                            color: #64748b;
                            margin-bottom: 6px;
                        }
                        h1 {
                            font-size: 24px;
                            margin: 0 0 6px 0;
                            color: #0f172a;
                        }
                        h2 {
                            font-size: 15px;
                            margin: 18px 0 10px 0;
                            color: #0f172a;
                        }
                        .lead {
                            color: #475569;
                            margin-bottom: 0;
                        }
                        .hero {
                            border: 1px solid #bfdbfe;
                            border-radius: 14px;
                            background: linear-gradient(135deg, #eff6ff, #f8fbff);
                            padding: 14px 16px;
                            margin-bottom: 16px;
                        }
                        .meta {
                            width: 100%%;
                            border-collapse: separate;
                            border-spacing: 0 8px;
                        }
                        .meta-label {
                            width: 170px;
                            color: #64748b;
                            font-size: 10px;
                            padding-right: 10px;
                        }
                        .meta-value {
                            color: #111827;
                            font-weight: bold;
                        }
                        .tiles {
                            width: 100%%;
                            border-collapse: separate;
                            border-spacing: 10px;
                            margin: 0 -10px 6px -10px;
                        }
                        .tile {
                            width: 33.33%%;
                            border: 1px solid #dbeafe;
                            border-radius: 12px;
                            background: #ffffff;
                            padding: 12px;
                        }
                        .tile-label {
                            font-size: 9px;
                            text-transform: uppercase;
                            letter-spacing: 0.08em;
                            color: #64748b;
                            margin-bottom: 6px;
                        }
                        .tile-value {
                            font-size: 16px;
                            font-weight: bold;
                            color: #0f172a;
                        }
                        .tile-note {
                            margin-top: 4px;
                            color: #64748b;
                            font-size: 9px;
                        }
                        .section-box {
                            border: 1px solid #e2e8f0;
                            border-radius: 12px;
                            padding: 14px 16px;
                            background: #ffffff;
                        }
                        .schedule {
                            width: 100%%;
                            border-collapse: collapse;
                            margin-top: 10px;
                            font-size: 9.5px;
                        }
                        .schedule th {
                            background: #e0f2fe;
                            color: #0f172a;
                            border: 1px solid #bae6fd;
                            padding: 7px 6px;
                            text-align: center;
                        }
                        .schedule td {
                            border: 1px solid #e2e8f0;
                            padding: 6px 6px;
                            text-align: right;
                        }
                        .schedule td:first-child,
                        .schedule td:nth-child(2) {
                            text-align: center;
                        }
                        .schedule tbody tr:nth-child(even) td {
                            background: #f8fafc;
                        }
                        .footer-note {
                            margin-top: 12px;
                            font-size: 9px;
                            color: #64748b;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="eyebrow">Потребительский кредит</div>
                        <h1>Кредитный договор</h1>
                        <p class="lead">Документ сформирован на основе логики расчётов calculator и включает график платежей по выбранному виду платежа.</p>
                    </div>

                    <div class="hero">
                        <table class="meta">
                            <tr>
                                <td class="meta-label">Заемщик</td>
                                <td class="meta-value">%s</td>
                            </tr>
                            <tr>
                                <td class="meta-label">Документ</td>
                                <td class="meta-value">%s</td>
                            </tr>
                            <tr>
                                <td class="meta-label">Дата договора</td>
                                <td class="meta-value">%s</td>
                            </tr>
                            <tr>
                                <td class="meta-label">Вид платежа</td>
                                <td class="meta-value">%s</td>
                            </tr>
                        </table>
                    </div>

                    <h2>Основные условия</h2>
                    <table class="tiles">
                        <tr>
                            <td class="tile">
                                <div class="tile-label">Сумма кредита</div>
                                <div class="tile-value">%s</div>
                            </td>
                            <td class="tile">
                                <div class="tile-label">Срок</div>
                                <div class="tile-value">%d мес.</div>
                            </td>
                            <td class="tile">
                                <div class="tile-label">Ставка</div>
                                <div class="tile-value">%s%%</div>
                                <div class="tile-note">годовых</div>
                            </td>
                        </tr>
                        <tr>
                            <td class="tile">
                                <div class="tile-label">Платёж</div>
                                <div class="tile-value">%s</div>
                            </td>
                            <td class="tile">
                                <div class="tile-label">Общая сумма выплат</div>
                                <div class="tile-value">%s</div>
                            </td>
                            <td class="tile">
                                <div class="tile-label">Переплата по процентам</div>
                                <div class="tile-value">%s</div>
                            </td>
                        </tr>
                        <tr>
                            <td class="tile">
                                <div class="tile-label">Полная стоимость кредита</div>
                                <div class="tile-value">%s%%</div>
                            </td>
                            <td class="tile">
                                <div class="tile-label">Первый платёж</div>
                                <div class="tile-value">%s</div>
                            </td>
                            <td class="tile">
                                <div class="tile-label">Последний платёж</div>
                                <div class="tile-value">%s</div>
                            </td>
                        </tr>
                    </table>

                    <h2>Условия договора</h2>
                    <div class="section-box">
                        <p>Кредитор предоставляет заемщику денежные средства на условиях возвратности, срочности и платности.</p>
                        <p>Заемщик обязуется вносить платежи согласно графику, указанному ниже, и соблюдать условия обслуживания кредита.</p>
                        <p>Первый платёж подлежит внесению до <strong>%s</strong>, последний платёж - до <strong>%s</strong>.</p>
                    </div>

                    <h2>График платежей</h2>
                    <table class="schedule">
                        <thead>
                            <tr>
                                <th>Месяц</th>
                                <th>Дата платежа</th>
                                <th>Платёж</th>
                                <th>Основной долг</th>
                                <th>Проценты</th>
                                <th>Остаток долга</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>

                    <p class="footer-note">Все суммы указаны в рублях. Для дифференцированного графика сумма платежа уменьшается от месяца к месяцу.</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(contractData.borrowerName()),
                escapeHtml(contractData.passportDisplay()),
                escapeHtml(formatDate(contractData.generatedDate())),
                escapeHtml(contractData.paymentTypeLabel()),
                escapeHtml(formatMoney(contractData.creditAmount())),
                contractData.termMonths(),
                escapeHtml(formatRate(contractData.annualRate())),
                escapeHtml(paymentSummary),
                escapeHtml(formatMoney(contractData.totalPayment())),
                escapeHtml(formatMoney(contractData.totalInterest())),
                escapeHtml(formatRate(contractData.psk())),
                escapeHtml(formatMoney(schedule.get(0).paymentAmount())),
                escapeHtml(formatMoney(schedule.get(schedule.size() - 1).paymentAmount())),
                escapeHtml(formatDate(contractData.firstPaymentDate())),
                escapeHtml(formatDate(contractData.lastPaymentDate())),
                scheduleRows
        );
    }

    private void configureFonts(PdfRendererBuilder builder) {
        PDF_FONT_CANDIDATE_PATHS.stream()
                .filter(this::fontExists)
                .findFirst()
                .ifPresent(path -> builder.useFont(new File(path), PDF_FONT_FAMILY));
    }

    private boolean fontExists(String path) {
        return new File(path).exists();
    }

    private String formatMoney(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(RU_LOCALE);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(value) + " руб.";
    }

    private String formatRate(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(LocalDate value) {
        return DATE_FORMATTER.format(value);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record ContractData(
            String borrowerName,
            String passportDisplay,
            BigDecimal creditAmount,
            Integer termMonths,
            BigDecimal annualRate,
            BigDecimal monthlyPayment,
            BigDecimal totalPayment,
            BigDecimal totalInterest,
            BigDecimal psk,
            String paymentTypeLabel,
            LocalDate generatedDate,
            LocalDate firstPaymentDate,
            LocalDate lastPaymentDate
    ) {
    }

    private record PaymentScheduleRow(
            int monthNumber,
            LocalDate paymentDate,
            BigDecimal paymentAmount,
            BigDecimal principalAmount,
            BigDecimal interestAmount,
            BigDecimal remainingDebt
    ) {
    }
}
