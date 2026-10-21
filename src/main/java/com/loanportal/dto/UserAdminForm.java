package com.loanportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.FormParam;

/** Admin-only user create/edit form. Password is optional on edit (blank = unchanged). */
public class UserAdminForm {
    @FormParam("email") @NotBlank @Email
    public String email;

    @FormParam("fullName") @NotBlank
    public String fullName;

    @FormParam("role") @NotBlank
    public String role;

    @FormParam("password")
    public String password;

    @FormParam("active")
    public boolean active;
}
