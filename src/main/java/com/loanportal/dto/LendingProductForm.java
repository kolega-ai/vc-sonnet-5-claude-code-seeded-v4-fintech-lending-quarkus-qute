package com.loanportal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import java.math.BigDecimal;

public class LendingProductForm {
    @FormParam("name") @NotBlank
    public String name;

    @FormParam("description") @NotBlank
    public String description;

    @FormParam("minAmount") @NotNull @DecimalMin("0.00")
    public BigDecimal minAmount;

    @FormParam("maxAmount") @NotNull @DecimalMin("0.01")
    public BigDecimal maxAmount;

    @FormParam("minTermMonths") @NotNull @Min(1)
    public Integer minTermMonths;

    @FormParam("maxTermMonths") @NotNull @Min(1)
    public Integer maxTermMonths;

    @FormParam("baseRatePct") @NotNull @DecimalMin("0.00")
    public BigDecimal baseRatePct;

    @FormParam("active")
    public boolean active;
}
