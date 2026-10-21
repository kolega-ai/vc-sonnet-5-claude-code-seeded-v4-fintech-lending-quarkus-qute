package com.loanportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.FormParam;

/** Public self-registration form for a new borrower account. Role is never accepted here - it is always BORROWER. */
public class RegisterForm {
    @FormParam("email")
    @NotBlank @Email
    public String email;

    @FormParam("password")
    @NotBlank @Size(min = 10, max = 128)
    public String password;

    @FormParam("fullName")
    @NotBlank @Size(max = 255)
    public String fullName;

    @FormParam("businessName")
    @NotBlank @Size(max = 255)
    public String businessName;

    @FormParam("businessType")
    @NotBlank @Size(max = 120)
    public String businessType;

    @FormParam("taxId")
    @NotBlank @Size(max = 32)
    public String taxId;

    @FormParam("addressLine1")
    @NotBlank @Size(max = 255)
    public String addressLine1;

    @FormParam("city")
    @NotBlank @Size(max = 120)
    public String city;

    @FormParam("state")
    @NotBlank @Size(min = 2, max = 2)
    public String state;

    @FormParam("postalCode")
    @NotBlank @Size(max = 16)
    public String postalCode;

    @FormParam("contactPhone")
    @NotBlank @Size(max = 32)
    public String contactPhone;
}
