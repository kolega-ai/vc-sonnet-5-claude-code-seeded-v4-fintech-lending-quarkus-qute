package com.loanportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.FormParam;

public class DocTypeForm {
    @FormParam("code") @NotBlank
    public String code;

    @FormParam("name") @NotBlank
    public String name;

    @FormParam("description") @NotBlank
    public String description;

    @FormParam("required")
    public boolean required;

    @FormParam("active")
    public boolean active;

    @FormParam("validityDays")
    public Integer validityDays;
}
