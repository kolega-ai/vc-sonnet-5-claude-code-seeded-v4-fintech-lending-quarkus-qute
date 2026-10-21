package com.loanportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.FormParam;
import java.math.BigDecimal;

/** Underwriter decision submission. Only underwriter/admin roles may ever reach the service method that consumes this. */
public class DecisionForm {
    @FormParam("decisionType")
    @NotBlank
    public String decisionType;

    @FormParam("riskTier")
    @NotBlank
    public String riskTier;

    @FormParam("reasons")
    @NotBlank @Size(max = 2000)
    public String reasons;

    @FormParam("counterofferAmount")
    public BigDecimal counterofferAmount;

    @FormParam("counterofferRatePct")
    public BigDecimal counterofferRatePct;

    @FormParam("counterofferTermMonths")
    public Integer counterofferTermMonths;
}
