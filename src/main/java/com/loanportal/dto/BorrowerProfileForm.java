package com.loanportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.FormParam;

public class BorrowerProfileForm {
    @FormParam("businessName") @NotBlank @Size(max = 255)
    public String businessName;

    @FormParam("businessType") @NotBlank @Size(max = 120)
    public String businessType;

    @FormParam("addressLine1") @NotBlank @Size(max = 255)
    public String addressLine1;

    @FormParam("addressLine2") @Size(max = 255)
    public String addressLine2;

    @FormParam("city") @NotBlank @Size(max = 120)
    public String city;

    @FormParam("state") @NotBlank @Size(min = 2, max = 2)
    public String state;

    @FormParam("postalCode") @NotBlank @Size(max = 16)
    public String postalCode;

    @FormParam("contactPhone") @NotBlank @Size(max = 32)
    public String contactPhone;
}
