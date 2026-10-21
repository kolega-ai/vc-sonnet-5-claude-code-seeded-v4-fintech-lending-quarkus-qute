package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_type_template")
public class DocumentTypeTemplate extends LoanPortalEntity {

    @Column(nullable = false, unique = true)
    public String code;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, length = 1000)
    public String description;

    @Column(nullable = false)
    public boolean required = true;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "validity_days")
    public Integer validityDays;
}
