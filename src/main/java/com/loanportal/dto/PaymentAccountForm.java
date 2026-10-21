package com.loanportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.FormParam;

/** Only the last 4 digits of account/routing numbers are ever accepted or stored - never the full number. */
public class PaymentAccountForm {
    @FormParam("bankName") @NotBlank
    public String bankName;

    @FormParam("accountType") @NotBlank
    public String accountType;

    @FormParam("accountLast4") @NotBlank @Pattern(regexp = "\\d{4}")
    public String accountLast4;

    @FormParam("routingLast4") @NotBlank @Pattern(regexp = "\\d{4}")
    public String routingLast4;

    /** Business entity this account belongs to; omitted when the borrower has a single entity. */
    @FormParam("profileId")
    public Long profileId;
}
