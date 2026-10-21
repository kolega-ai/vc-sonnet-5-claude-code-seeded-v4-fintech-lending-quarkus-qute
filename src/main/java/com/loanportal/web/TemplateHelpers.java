package com.loanportal.web;

import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.Document;
import com.loanportal.model.DocumentStatus;
import com.loanportal.model.LoanApplication;
import io.quarkus.qute.TemplateExtension;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.ZoneOffset;
import java.util.Locale;

/** Qute template extension methods - display formatting helpers shared across all pages. */
@TemplateExtension
public class TemplateHelpers {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneOffset.UTC).withLocale(Locale.US);

    public static String badgeClass(ApplicationStatus status) {
        return switch (status) {
            case DRAFT, WITHDRAWN -> "badge-neutral";
            case SUBMITTED, IN_REVIEW -> "badge-info";
            case NEEDS_DOCUMENTS, COUNTEROFFERED -> "badge-warning";
            case APPROVED -> "badge-success";
            case REJECTED -> "badge-danger";
        };
    }

    public static String badgeClass(DocumentStatus status) {
        return switch (status) {
            case REQUESTED, UPLOADED -> "badge-info";
            case ACCEPTED -> "badge-success";
            case REJECTED, EXPIRED -> "badge-danger";
            case NEEDS_RESUBMISSION -> "badge-warning";
        };
    }

    public static String label(ApplicationStatus status) {
        return status.name().replace('_', ' ');
    }

    public static String label(DocumentStatus status) {
        return status.name().replace('_', ' ');
    }

    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return "$" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public static String fmt(Instant instant) {
        return instant == null ? "" : DATE_FMT.format(instant) + " UTC";
    }

    /** Human age like "2d 4h" used for the underwriter queue SLA column. */
    public static String age(Instant since) {
        if (since == null) {
            return "";
        }
        Duration d = Duration.between(since, Instant.now());
        long days = d.toDays();
        long hours = d.toHoursPart();
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        return d.toHours() + "h " + d.toMinutesPart() + "m";
    }

    public static long missingDocCount(LoanApplication app) {
        return com.loanportal.security.CurrentUser.inTransaction(() ->
                Document.count("application.id = ?1 and documentType.required = true and status != ?2", app.id, DocumentStatus.ACCEPTED));
    }

    public static Instant slaSince(LoanApplication app) {
        return app.submittedAt != null ? app.submittedAt : app.createdAt;
    }

    public static String maskTaxId(String taxId) {
        if (taxId == null || taxId.length() < 4) {
            return "****";
        }
        return "*".repeat(taxId.length() - 4) + taxId.substring(taxId.length() - 4);
    }
}
