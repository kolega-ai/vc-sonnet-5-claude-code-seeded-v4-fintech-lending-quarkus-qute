package com.loanportal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.FormParam;
import java.math.BigDecimal;

public class BusinessOwnerForm {
    @FormParam("fullName") @NotBlank
    public String fullName;

    @FormParam("title") @NotBlank
    public String title;

    @FormParam("ownershipPct") @NotNull @DecimalMin("0.01") @DecimalMax("100.00")
    public BigDecimal ownershipPct;

    @FormParam("ssnLast4") @NotBlank @Pattern(regexp = "\\d{4}")
    public String ssnLast4;

    @FormParam("dateOfBirth")
    public String dateOfBirth;
}
