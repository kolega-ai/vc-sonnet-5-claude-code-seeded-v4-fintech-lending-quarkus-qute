package com.loanportal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.FormParam;
import java.math.BigDecimal;

/** Loan application wizard submission. status/riskTier/assignedUnderwriter are never bindable here. */
public class ApplicationForm {
    @FormParam("lendingProductId")
    @NotNull
    public Long lendingProductId;

    @FormParam("requestedAmount")
    @NotNull @DecimalMin("1000.00")
    public BigDecimal requestedAmount;

    @FormParam("requestedTermMonths")
    @NotNull @Min(1)
    public Integer requestedTermMonths;

    @FormParam("purpose")
    @NotBlank @Size(max = 2000)
    public String purpose;

    @FormParam("annualRevenue")
    public BigDecimal annualRevenue;

    @FormParam("riskTier")
    public String riskTier;
}
