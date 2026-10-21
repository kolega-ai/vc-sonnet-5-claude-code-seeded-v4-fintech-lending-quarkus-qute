package com.loanportal.service;

import com.loanportal.dto.DocTypeForm;
import com.loanportal.dto.LendingProductForm;
import com.loanportal.dto.UserAdminForm;
import com.loanportal.model.DocumentTypeTemplate;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

/** Admin-only catalog management: lending products, document type templates, user accounts. */
@ApplicationScoped
public class AdminCatalogService {

    @Inject
    AuditService auditService;

    @Transactional
    public LendingProduct saveProduct(Long id, LendingProductForm form, User actor) {
        LendingProduct product = id == null ? new LendingProduct() : LendingProduct.findById(id);
        if (product == null) {
            throw new BadRequestException("Product not found");
        }
        if (form.minAmount.compareTo(form.maxAmount) > 0) {
            throw new BadRequestException("Minimum amount must be <= maximum amount");
        }
        if (form.minTermMonths > form.maxTermMonths) {
            throw new BadRequestException("Minimum term must be <= maximum term");
        }
        String action = id == null ? "LENDING_PRODUCT_CREATED" : "LENDING_PRODUCT_UPDATED";
        product.name = form.name;
        product.description = form.description;
        product.minAmount = form.minAmount;
        product.maxAmount = form.maxAmount;
        product.minTermMonths = form.minTermMonths;
        product.maxTermMonths = form.maxTermMonths;
        product.baseRatePct = form.baseRatePct;
        product.active = form.active;
        if (id == null) {
            product.persist();
        }
        auditService.record(actor, action, "LendingProduct", product.id, null, form.name, "admin catalog management");
        return product;
    }

    @Transactional
    public DocumentTypeTemplate saveDocType(Long id, DocTypeForm form, User actor) {
        DocumentTypeTemplate type = id == null ? new DocumentTypeTemplate() : DocumentTypeTemplate.findById(id);
        if (type == null) {
            throw new BadRequestException("Document type not found");
        }
        String action = id == null ? "DOC_TYPE_CREATED" : "DOC_TYPE_UPDATED";
        type.code = form.code;
        type.name = form.name;
        type.description = form.description;
        type.required = form.required;
        type.active = form.active;
        type.validityDays = form.validityDays;
        if (id == null) {
            type.persist();
        }
        auditService.record(actor, action, "DocumentTypeTemplate", type.id, null, form.code, "admin catalog management");
        return type;
    }

    @Transactional
    public User saveUser(Long id, UserAdminForm form, User actor) {
        Role role;
        try {
            role = Role.valueOf(form.role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown role");
        }
        User user = id == null ? new User() : User.findById(id);
        if (user == null) {
            throw new BadRequestException("User not found");
        }
        User existingByEmail = User.findByEmail(form.email);
        if (existingByEmail != null && !existingByEmail.id.equals(id)) {
            throw new BadRequestException("Email already in use");
        }
        String action = id == null ? "ADMIN_USER_CREATED" : "ADMIN_USER_UPDATED";
        String before = id == null ? null : (user.email + "|" + user.role + "|" + user.active);
        user.email = form.email.trim().toLowerCase();
        user.fullName = form.fullName;
        user.role = role;
        user.active = form.active;
        if (form.password != null && !form.password.isBlank()) {
            user.password = BcryptUtil.bcryptHash(form.password);
        } else if (id == null) {
            throw new BadRequestException("Password is required for a new user");
        }
        if (id == null) {
            user.persist();
        }
        auditService.record(actor, action, "User", user.id, before, user.email + "|" + user.role + "|" + user.active, "admin user management");
        return user;
    }
}
