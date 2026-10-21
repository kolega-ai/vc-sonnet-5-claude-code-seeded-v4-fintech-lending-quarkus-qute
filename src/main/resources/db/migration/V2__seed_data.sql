-- Seed data. All demo passwords are BCrypt hashes of the credentials documented in README.md.
-- Password source (cost 10): see README "Demo credentials" table.

INSERT INTO app_user (email, password_hash, full_name, role, active) VALUES
  ('admin@loanportal.test', '$2a$10$3zZZdN0fjijLJZA8Ypsk5.j.rjydn7qmxS5lLNvawejsc.HYSE29u', 'Alex Kim', 'ADMIN', true),
  ('uw.morgan@loanportal.test', '$2a$10$xuRWL3bbZ66smxMGzWarG.FQvYPUwYMZTDhF174lbGfRCylMK6ebK', 'Morgan Ellis', 'UNDERWRITER', true),
  ('uw.priya@loanportal.test', '$2a$10$xuRWL3bbZ66smxMGzWarG.FQvYPUwYMZTDhF174lbGfRCylMK6ebK', 'Priya Raman', 'UNDERWRITER', true),
  ('support.dana@loanportal.test', '$2a$10$rw9pnF2uBIqOqg1fCHIJUOGHe0sKn94wx42YOuyMKF0Lp9ymgO/Be', 'Dana Ruiz', 'SUPPORT_SPECIALIST', true),
  ('owner@brightleafbakery.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Jamie Alvarez', 'BORROWER', true),
  ('owner@northpineroofing.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Sam Whitfield', 'BORROWER', true),
  ('owner@cedarcreekcafe.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Riley Chen', 'BORROWER', true),
  ('owner@ironworksfitness.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Taylor Brooks', 'BORROWER', true),
  ('owner@sunvalleylandscaping.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Casey Nguyen', 'BORROWER', true),
  ('owner@bluewaterplumbing.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Jordan Patel', 'BORROWER', true),
  ('owner@harvestmoonmarket.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Avery Thompson', 'BORROWER', true),
  ('owner@granitepeakauto.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Drew Sullivan', 'BORROWER', true),
  ('owner@lakesidedaycare.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Morgan Cole', 'BORROWER', true),
  ('owner@westendbookshop.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Sydney Fox', 'BORROWER', true),
  ('owner@urbanpetgrooming.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Reese Martin', 'BORROWER', true),
  ('owner@ridgelinebrewing.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Quinn Foster', 'BORROWER', true),
  ('owner@harborviewdetailing.test', '$2a$10$dRbz81PH4PstyRsp3EknZ.uuW.iN5Jq18ZH0SIaOpATjdw0KemnA6', 'Charlie Reyes', 'BORROWER', true);

INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Brightleaf Bakery', 'LLC', '84-1000001', 4, 42000000, '118 Maple St', 'Asheville', 'NC', '28801', '828-555-0101' FROM app_user WHERE email='owner@brightleafbakery.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'North Pine Roofing', 'S-Corp', '84-1000002', 8, 118000000, '2200 Industrial Way', 'Boise', 'ID', '83701', '208-555-0102' FROM app_user WHERE email='owner@northpineroofing.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Cedar Creek Cafe', 'Sole Proprietorship', '84-1000003', 2, 21000000, '55 River Rd', 'Bend', 'OR', '97701', '541-555-0103' FROM app_user WHERE email='owner@cedarcreekcafe.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Ironworks Fitness', 'LLC', '84-1000004', 6, 64000000, '900 Fitness Ave', 'Denver', 'CO', '80202', '303-555-0104' FROM app_user WHERE email='owner@ironworksfitness.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Sun Valley Landscaping', 'LLC', '84-1000005', 5, 53000000, '77 Meadow Ln', 'Ketchum', 'ID', '83340', '208-555-0105' FROM app_user WHERE email='owner@sunvalleylandscaping.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Bluewater Plumbing', 'LLC', '84-1000006', 11, 89000000, '410 Harbor St', 'Tacoma', 'WA', '98402', '253-555-0106' FROM app_user WHERE email='owner@bluewaterplumbing.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Harvest Moon Market', 'LLC', '84-1000007', 3, 31000000, '15 Grange Rd', 'Burlington', 'VT', '05401', '802-555-0107' FROM app_user WHERE email='owner@harvestmoonmarket.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Granite Peak Auto', 'S-Corp', '84-1000008', 9, 97000000, '620 Motor Pkwy', 'Bozeman', 'MT', '59715', '406-555-0108' FROM app_user WHERE email='owner@granitepeakauto.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Lakeside Daycare', 'LLC', '84-1000009', 7, 41000000, '303 Lakeshore Dr', 'Madison', 'WI', '53703', '608-555-0109' FROM app_user WHERE email='owner@lakesidedaycare.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'West End Bookshop', 'Sole Proprietorship', '84-1000010', 1, 9000000, '48 Elm St', 'Providence', 'RI', '02903', '401-555-0110' FROM app_user WHERE email='owner@westendbookshop.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Urban Pet Grooming', 'LLC', '84-1000011', 4, 26000000, '12 Collar Ct', 'Austin', 'TX', '78701', '512-555-0111' FROM app_user WHERE email='owner@urbanpetgrooming.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Ridgeline Brewing Co.', 'LLC', '84-1000012', 6, 76000000, '801 Hoptown Rd', 'Asheville', 'NC', '28806', '828-555-0112' FROM app_user WHERE email='owner@ridgelinebrewing.test';
INSERT INTO borrower_profile (user_id, business_name, business_type, tax_id, years_in_business, annual_revenue_cents, address_line1, city, state, postal_code, contact_phone)
SELECT id, 'Harborview Detailing', 'Sole Proprietorship', '84-1000013', 2, 14000000, '9 Dock St', 'Portland', 'ME', '04101', '207-555-0113' FROM app_user WHERE email='owner@harborviewdetailing.test';

INSERT INTO business_owner (borrower_profile_id, full_name, title, ownership_pct, ssn_last4, date_of_birth)
SELECT id, 'Jamie Alvarez', 'CEO', 100.00, '4421', '1985-03-14' FROM borrower_profile WHERE business_name='Brightleaf Bakery';
INSERT INTO business_owner (borrower_profile_id, full_name, title, ownership_pct, ssn_last4, date_of_birth)
SELECT id, 'Sam Whitfield', 'President', 60.00, '1187', '1979-07-02' FROM borrower_profile WHERE business_name='North Pine Roofing';
INSERT INTO business_owner (borrower_profile_id, full_name, title, ownership_pct, ssn_last4, date_of_birth)
SELECT id, 'Robin Whitfield', 'VP Operations', 40.00, '2298', '1981-11-19' FROM borrower_profile WHERE business_name='North Pine Roofing';
INSERT INTO business_owner (borrower_profile_id, full_name, title, ownership_pct, ssn_last4, date_of_birth)
SELECT id, 'Riley Chen', 'Owner', 100.00, '7734', '1992-01-08' FROM borrower_profile WHERE business_name='Cedar Creek Cafe';
INSERT INTO business_owner (borrower_profile_id, full_name, title, ownership_pct, ssn_last4, date_of_birth)
SELECT id, 'Taylor Brooks', 'Managing Member', 100.00, '3350', '1988-05-27' FROM borrower_profile WHERE business_name='Ironworks Fitness';
INSERT INTO business_owner (borrower_profile_id, full_name, title, ownership_pct, ssn_last4, date_of_birth)
SELECT id, 'Casey Nguyen', 'Owner', 100.00, '9012', '1990-09-30' FROM borrower_profile WHERE business_name='Sun Valley Landscaping';

INSERT INTO lending_product (name, description, min_amount, max_amount, min_term_months, max_term_months, base_rate_pct, active) VALUES
  ('Working Capital Line', 'Short-term revolving capital for day-to-day operations.', 5000.00, 150000.00, 6, 24, 9.50, true),
  ('Equipment Financing', 'Fixed-term loan to purchase or refinance business equipment.', 10000.00, 500000.00, 12, 60, 7.25, true),
  ('SBA-Style Term Loan', 'Long-term loan for expansion, acquisition, or major capital projects.', 25000.00, 2000000.00, 24, 120, 6.75, true),
  ('Seasonal Bridge Loan', 'Short bridge financing for seasonal cash-flow gaps.', 5000.00, 75000.00, 3, 12, 11.00, true),
  ('Legacy Microloan (Retired)', 'Discontinued small-dollar product retained for historical records.', 1000.00, 15000.00, 6, 18, 12.50, false);

INSERT INTO document_type_template (code, name, description, required, active, validity_days) VALUES
  ('gov_id', 'Government-issued ID', 'Photo ID for the primary business owner.', true, true, 1825),
  ('bank_statement_3mo', 'Bank Statements (last 3 months)', 'Most recent three months of business bank statements.', true, true, 90),
  ('tax_return_2yr', 'Business Tax Returns (2 years)', 'Most recent two years of filed business tax returns.', true, true, 365),
  ('business_license', 'Business License', 'Current state or local business operating license.', true, true, 365),
  ('articles_of_incorporation', 'Articles of Incorporation / Organization', 'Formation documents for the business entity.', true, true, null),
  ('voided_check', 'Voided Check', 'For ACH disbursement setup.', false, true, null),
  ('insurance_certificate', 'Certificate of Insurance', 'Proof of current business liability insurance.', false, true, 365);

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 45000.00, 18, 'Purchase a second commercial oven and expand catering operations.', bp.annual_revenue_cents, 'IN_REVIEW', 'MEDIUM', uw.id, now() - interval '5 days', now() - interval '6 days', now() - interval '1 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Brightleaf Bakery' AND lp.name='Working Capital Line' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 220000.00, 48, 'Buy two new roofing crew trucks and lift equipment.', bp.annual_revenue_cents, 'APPROVED', 'LOW', uw.id, now() - interval '20 days', now() - interval '22 days', now() - interval '15 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='North Pine Roofing' AND lp.name='Equipment Financing' AND uw.email='uw.priya@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 18000.00, 9, 'Bridge financing through the slow winter season.', bp.annual_revenue_cents, 'REJECTED', 'HIGH', uw.id, now() - interval '30 days', now() - interval '31 days', now() - interval '26 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Cedar Creek Cafe' AND lp.name='Seasonal Bridge Loan' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 60000.00, 24, 'Renovate studio space and buy new cardio equipment.', bp.annual_revenue_cents, 'COUNTEROFFERED', 'MEDIUM', uw.id, now() - interval '10 days', now() - interval '11 days', now() - interval '3 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Ironworks Fitness' AND lp.name='Equipment Financing' AND uw.email='uw.priya@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 35000.00, 12, 'Purchase new mowers and a trailer for expanding client base.', bp.annual_revenue_cents, 'SUBMITTED', 'UNSCORED', now() - interval '2 days', now() - interval '2 days', now() - interval '2 days'
FROM borrower_profile bp, lending_product lp
WHERE bp.business_name='Sun Valley Landscaping' AND lp.name='Working Capital Line';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 90000.00, 36, 'Fleet expansion: 3 new service vans.', bp.annual_revenue_cents, 'NEEDS_DOCUMENTS', 'MEDIUM', uw.id, now() - interval '8 days', now() - interval '9 days', now() - interval '2 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Bluewater Plumbing' AND lp.name='Equipment Financing' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, created_at, updated_at)
SELECT bp.id, lp.id, 12000.00, 6, 'Restock inventory ahead of harvest season.', bp.annual_revenue_cents, 'DRAFT', 'UNSCORED', now() - interval '1 days', now() - interval '1 days'
FROM borrower_profile bp, lending_product lp
WHERE bp.business_name='Harvest Moon Market' AND lp.name='Seasonal Bridge Loan';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 350000.00, 84, 'Acquire adjacent lot and expand service bays.', bp.annual_revenue_cents, 'IN_REVIEW', 'LOW', uw.id, now() - interval '4 days', now() - interval '5 days', now() - interval '1 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Granite Peak Auto' AND lp.name='SBA-Style Term Loan' AND uw.email='uw.priya@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 28000.00, 12, 'Playground equipment and safety upgrades.', bp.annual_revenue_cents, 'APPROVED', 'LOW', uw.id, now() - interval '40 days', now() - interval '41 days', now() - interval '33 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Lakeside Daycare' AND lp.name='Working Capital Line' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, created_at, updated_at)
SELECT bp.id, lp.id, 9000.00, 6, 'Initial inventory for shop opening.', bp.annual_revenue_cents, 'DRAFT', 'UNSCORED', now() - interval '3 hours', now() - interval '3 hours'
FROM borrower_profile bp, lending_product lp
WHERE bp.business_name='West End Bookshop' AND lp.name='Seasonal Bridge Loan';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 22000.00, 18, 'New grooming tables and a second van.', bp.annual_revenue_cents, 'WITHDRAWN', 'UNSCORED', uw.id, now() - interval '15 days', now() - interval '16 days', now() - interval '14 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Urban Pet Grooming' AND lp.name='Working Capital Line' AND uw.email='uw.priya@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 500000.00, 96, 'Build out a new taproom and canning line.', bp.annual_revenue_cents, 'IN_REVIEW', 'HIGH', uw.id, now() - interval '6 days', now() - interval '7 days', now() - interval '1 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Ridgeline Brewing Co.' AND lp.name='SBA-Style Term Loan' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, created_at, updated_at)
SELECT bp.id, lp.id, 7000.00, 6, 'Pressure washer and detailing supplies.', bp.annual_revenue_cents, 'SUBMITTED', 'UNSCORED', now() - interval '1 days', now() - interval '1 days'
FROM borrower_profile bp, lending_product lp
WHERE bp.business_name='Harborview Detailing' AND lp.name='Working Capital Line';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 65000.00, 24, 'Second bakery location build-out.', bp.annual_revenue_cents, 'REJECTED', 'HIGH', uw.id, now() - interval '50 days', now() - interval '51 days', now() - interval '45 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='Brightleaf Bakery' AND lp.name='SBA-Style Term Loan' AND uw.email='uw.priya@loanportal.test';

INSERT INTO loan_application (borrower_profile_id, lending_product_id, requested_amount, requested_term_months, purpose, annual_revenue_cents, status, risk_tier, assigned_underwriter_id, submitted_at, created_at, updated_at)
SELECT bp.id, lp.id, 140000.00, 36, 'Refinance existing equipment loan at a better rate.', bp.annual_revenue_cents, 'COUNTEROFFERED', 'MEDIUM', uw.id, now() - interval '12 days', now() - interval '13 days', now() - interval '2 days'
FROM borrower_profile bp, lending_product lp, app_user uw
WHERE bp.business_name='North Pine Roofing' AND lp.name='SBA-Style Term Loan' AND uw.email='uw.morgan@loanportal.test';

-- Required documents per application (mirrors what ApplicationWorkflowService seeds on create)
INSERT INTO document (application_id, document_type_id, status, stored_path, original_filename, content_type, size_bytes, uploaded_by, reviewed_by, rejection_reason, expires_at, created_at, updated_at)
SELECT la.id, dt.id, 'ACCEPTED', NULL, 'gov_id_scan.pdf', 'application/pdf', 245000, bp.user_id, uw.id, NULL, now() + interval '4 years', now() - interval '5 days', now() - interval '4 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt, app_user uw
WHERE bp.business_name='Brightleaf Bakery' AND la.requested_amount = 45000.00 AND dt.code='gov_id' AND uw.email='uw.morgan@loanportal.test';
INSERT INTO document (application_id, document_type_id, status, uploaded_by, reviewed_by, expires_at, created_at, updated_at)
SELECT la.id, dt.id, 'UPLOADED', bp.user_id, NULL, NULL, now() - interval '4 days', now() - interval '4 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Brightleaf Bakery' AND la.requested_amount = 45000.00 AND dt.code='bank_statement_3mo';
INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'REQUESTED', now() - interval '6 days', now() - interval '6 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Brightleaf Bakery' AND la.requested_amount = 45000.00 AND dt.code IN ('tax_return_2yr','business_license','articles_of_incorporation');

INSERT INTO document (application_id, document_type_id, status, uploaded_by, reviewed_by, expires_at, created_at, updated_at)
SELECT la.id, dt.id, 'ACCEPTED', bp.user_id, uw.id, now() + interval '3 years', now() - interval '20 days', now() - interval '18 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt, app_user uw
WHERE bp.business_name='North Pine Roofing' AND la.status='APPROVED' AND dt.code IN ('gov_id','bank_statement_3mo','tax_return_2yr','business_license','articles_of_incorporation') AND uw.email='uw.priya@loanportal.test';

INSERT INTO document (application_id, document_type_id, status, rejection_reason, created_at, updated_at)
SELECT la.id, dt.id, 'REJECTED', 'Statement image is too low-resolution to verify; please resubmit a clearer scan.', now() - interval '29 days', now() - interval '27 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Cedar Creek Cafe' AND dt.code='bank_statement_3mo';
INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'REQUESTED', now() - interval '30 days', now() - interval '30 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Cedar Creek Cafe' AND dt.code IN ('gov_id','tax_return_2yr','business_license','articles_of_incorporation');

INSERT INTO document (application_id, document_type_id, status, uploaded_by, reviewed_by, expires_at, created_at, updated_at)
SELECT la.id, dt.id, 'ACCEPTED', bp.user_id, uw.id, now() + interval '2 years', now() - interval '10 days', now() - interval '8 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt, app_user uw
WHERE bp.business_name='Ironworks Fitness' AND dt.code IN ('gov_id','business_license') AND uw.email='uw.priya@loanportal.test';
INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'UPLOADED', now() - interval '9 days', now() - interval '9 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Ironworks Fitness' AND dt.code IN ('bank_statement_3mo','tax_return_2yr','articles_of_incorporation');

INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'REQUESTED', now() - interval '2 days', now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Sun Valley Landscaping' AND dt.required = true;

INSERT INTO document (application_id, document_type_id, status, expires_at, created_at, updated_at)
SELECT la.id, dt.id, 'EXPIRED', now() - interval '2 days', now() - interval '9 days', now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Bluewater Plumbing' AND dt.code='gov_id';
INSERT INTO document (application_id, document_type_id, status, rejection_reason, created_at, updated_at)
SELECT la.id, dt.id, 'NEEDS_RESUBMISSION', 'Bank statements provided are for the wrong account; please upload the business operating account statements.', now() - interval '9 days', now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Bluewater Plumbing' AND dt.code='bank_statement_3mo';
INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'REQUESTED', now() - interval '9 days', now() - interval '9 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Bluewater Plumbing' AND dt.code IN ('tax_return_2yr','business_license','articles_of_incorporation');

INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'REQUESTED', now() - interval '1 days', now() - interval '1 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Harvest Moon Market' AND dt.required = true;

INSERT INTO document (application_id, document_type_id, status, uploaded_by, reviewed_by, expires_at, created_at, updated_at)
SELECT la.id, dt.id, 'ACCEPTED', bp.user_id, uw.id, now() + interval '3 years', now() - interval '5 days', now() - interval '3 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt, app_user uw
WHERE bp.business_name='Granite Peak Auto' AND dt.code IN ('gov_id','bank_statement_3mo','business_license') AND uw.email='uw.priya@loanportal.test';
INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'UPLOADED', now() - interval '4 days', now() - interval '4 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Granite Peak Auto' AND dt.code IN ('tax_return_2yr','articles_of_incorporation');

INSERT INTO document (application_id, document_type_id, status, uploaded_by, reviewed_by, expires_at, created_at, updated_at)
SELECT la.id, dt.id, 'ACCEPTED', bp.user_id, uw.id, now() + interval '3 years', now() - interval '40 days', now() - interval '35 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt, app_user uw
WHERE bp.business_name='Lakeside Daycare' AND dt.code IN ('gov_id','bank_statement_3mo','tax_return_2yr','business_license','articles_of_incorporation') AND uw.email='uw.morgan@loanportal.test';

INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'REQUESTED', now() - interval '1 days', now() - interval '1 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Ridgeline Brewing Co.' AND dt.required = true;
UPDATE document SET status='UPLOADED' WHERE application_id = (SELECT la.id FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Ridgeline Brewing Co.') AND document_type_id = (SELECT id FROM document_type_template WHERE code='gov_id');

INSERT INTO document (application_id, document_type_id, status, created_at, updated_at)
SELECT la.id, dt.id, 'REQUESTED', now() - interval '1 days', now() - interval '1 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Harborview Detailing' AND dt.required = true;

INSERT INTO document (application_id, document_type_id, status, rejection_reason, created_at, updated_at)
SELECT la.id, dt.id, 'REJECTED', 'Tax returns provided do not match the business tax ID on file.', now() - interval '50 days', now() - interval '46 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt
WHERE bp.business_name='Brightleaf Bakery' AND la.status = 'REJECTED' AND dt.code = 'tax_return_2yr';

INSERT INTO document (application_id, document_type_id, status, uploaded_by, reviewed_by, expires_at, created_at, updated_at)
SELECT la.id, dt.id, 'ACCEPTED', bp.user_id, uw.id, now() + interval '3 years', now() - interval '12 days', now() - interval '10 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id = bp.id, document_type_template dt, app_user uw
WHERE bp.business_name='North Pine Roofing' AND la.status='COUNTEROFFERED' AND dt.code IN ('gov_id','bank_statement_3mo','tax_return_2yr','business_license','articles_of_incorporation') AND uw.email='uw.morgan@loanportal.test';

INSERT INTO underwriting_note (application_id, author_id, note, created_at)
SELECT la.id, uw.id, 'Financials look solid; waiting on the remaining bank statement before final decision.', now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw
WHERE bp.business_name='Brightleaf Bakery' AND la.requested_amount = 45000.00 AND uw.email='uw.morgan@loanportal.test';
INSERT INTO underwriting_note (application_id, author_id, note, created_at)
SELECT la.id, uw.id, 'Strong revenue trend YoY, low DTI. Recommend approval at standard rate.', now() - interval '18 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw
WHERE bp.business_name='North Pine Roofing' AND la.status='APPROVED' AND uw.email='uw.priya@loanportal.test';
INSERT INTO underwriting_note (application_id, author_id, note, created_at)
SELECT la.id, uw.id, 'Cash flow too thin given seasonality; declining per policy 4.2.', now() - interval '27 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw
WHERE bp.business_name='Cedar Creek Cafe' AND uw.email='uw.morgan@loanportal.test';
INSERT INTO underwriting_note (application_id, author_id, note, created_at)
SELECT la.id, uw.id, 'Offering reduced amount at higher rate given limited operating history at this location.', now() - interval '3 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw
WHERE bp.business_name='Ironworks Fitness' AND uw.email='uw.priya@loanportal.test';
INSERT INTO underwriting_note (application_id, author_id, note, created_at)
SELECT la.id, uw.id, 'Requested updated bank statements; borrower notified via portal message.', now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw
WHERE bp.business_name='Bluewater Plumbing' AND uw.email='uw.morgan@loanportal.test';
INSERT INTO underwriting_note (application_id, author_id, note, created_at)
SELECT la.id, uw.id, 'Large ask relative to revenue; escalate to senior underwriter before decision.', now() - interval '1 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw
WHERE bp.business_name='Granite Peak Auto' AND uw.email='uw.priya@loanportal.test';
INSERT INTO underwriting_note (application_id, author_id, note, created_at)
SELECT la.id, uw.id, 'Clean file, repeat borrower in good standing. Fast-track approved.', now() - interval '33 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw
WHERE bp.business_name='Lakeside Daycare' AND uw.email='uw.morgan@loanportal.test';
INSERT INTO underwriting_note (application_id, author_id, note, created_at)
SELECT la.id, uw.id, 'High leverage already on the balance sheet; flagged high risk tier pending further review.', now() - interval '1 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw
WHERE bp.business_name='Ridgeline Brewing Co.' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Hi, just wanted to check on the status of our application - anything else you need from us?', now() - interval '3 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Brightleaf Bakery' AND la.requested_amount=45000.00;
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, uw.id, 'Thanks for checking in! We just need one more bank statement and we can move to a decision.', now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Brightleaf Bakery' AND la.requested_amount=45000.00 AND uw.email='uw.morgan@loanportal.test';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Congratulations on the approval - when can we expect funds?', now() - interval '14 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='North Pine Roofing' AND la.status='APPROVED';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, uw.id, 'Funds are scheduled to disburse within 2 business days of your signed agreement.', now() - interval '13 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='North Pine Roofing' AND la.status='APPROVED' AND uw.email='uw.priya@loanportal.test';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'We understand the decision, thank you for the clear explanation.', now() - interval '25 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Cedar Creek Cafe';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Would a slightly smaller loan amount change the outcome?', now() - interval '3 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Ironworks Fitness';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, uw.id, 'We sent a counteroffer with adjusted terms - please review on the Decision tab.', now() - interval '3 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Ironworks Fitness' AND uw.email='uw.priya@loanportal.test';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, uw.id, 'Please upload statements from the correct business operating account when you have a chance.', now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Bluewater Plumbing' AND uw.email='uw.morgan@loanportal.test';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Apologies, uploading the correct statements now.', now() - interval '1 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Bluewater Plumbing';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Let us know if there is anything else needed on our end for the expansion loan.', now() - interval '4 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Granite Peak Auto';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, spec.id, 'Thanks for the update - I have flagged this for the underwriting team to review shortly.', now() - interval '4 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user spec WHERE bp.business_name='Granite Peak Auto' AND spec.email='support.dana@loanportal.test';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Excited to get started on the daycare renovations, thank you!', now() - interval '32 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Lakeside Daycare';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'We are ready whenever you need additional documentation for the taproom build-out.', now() - interval '1 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Ridgeline Brewing Co.';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Just submitted - excited to get this rolling!', now() - interval '1 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Harborview Detailing';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, spec.id, 'Welcome aboard! Your application is in queue for initial review.', now() - interval '20 hours'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user spec WHERE bp.business_name='Harborview Detailing' AND spec.email='support.dana@loanportal.test';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Sorry we could not move forward with this one - appreciate the feedback.', now() - interval '44 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Brightleaf Bakery' AND la.status='REJECTED';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Reviewing the counteroffer terms now, will respond by end of week.', now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='North Pine Roofing' AND la.status='COUNTEROFFERED';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, bp.user_id, 'Actually we will hold off on submitting this one for now.', now() - interval '14 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Urban Pet Grooming';
INSERT INTO message (application_id, sender_id, body, created_at)
SELECT la.id, uw.id, 'No problem, your withdrawal has been processed. Reach out any time to restart.', now() - interval '14 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Urban Pet Grooming' AND uw.email='uw.priya@loanportal.test';

INSERT INTO decision (application_id, decision_type, risk_tier, reasons, decided_by, decided_at)
SELECT la.id, 'APPROVE', 'LOW', 'Strong revenue growth, low leverage, excellent payment history on prior financing.', uw.id, now() - interval '15 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='North Pine Roofing' AND la.status='APPROVED' AND uw.email='uw.priya@loanportal.test';

INSERT INTO decision (application_id, decision_type, risk_tier, reasons, decided_by, decided_at)
SELECT la.id, 'REJECT', 'HIGH', 'Insufficient cash reserves relative to seasonal revenue swings; does not meet minimum DSCR policy threshold.', uw.id, now() - interval '26 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Cedar Creek Cafe' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO decision (application_id, decision_type, risk_tier, reasons, counteroffer_amount, counteroffer_rate_pct, counteroffer_term_months, decided_by, decided_at)
SELECT la.id, 'COUNTEROFFER', 'MEDIUM', 'Approved at reduced amount and adjusted rate given limited time at current location.', 40000.00, 8.50, 24, uw.id, now() - interval '3 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Ironworks Fitness' AND uw.email='uw.priya@loanportal.test';

INSERT INTO decision (application_id, decision_type, risk_tier, reasons, decided_by, decided_at)
SELECT la.id, 'APPROVE', 'LOW', 'Established repeat borrower, clean documentation, strong debt service coverage.', uw.id, now() - interval '33 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Lakeside Daycare' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO decision (application_id, decision_type, risk_tier, reasons, decided_by, decided_at)
SELECT la.id, 'REJECT', 'HIGH', 'Tax returns could not be verified against the business tax ID; potential documentation fraud risk.', uw.id, now() - interval '45 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Brightleaf Bakery' AND la.status='REJECTED' AND uw.email='uw.priya@loanportal.test';

INSERT INTO decision (application_id, decision_type, risk_tier, reasons, counteroffer_amount, counteroffer_rate_pct, counteroffer_term_months, decided_by, decided_at)
SELECT la.id, 'COUNTEROFFER', 'MEDIUM', 'Reduced term and adjusted rate to better match projected cash flow during refinance.', 125000.00, 7.75, 30, uw.id, now() - interval '2 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='North Pine Roofing' AND la.status='COUNTEROFFERED' AND uw.email='uw.morgan@loanportal.test';

INSERT INTO payment_account (borrower_profile_id, bank_name, account_type, account_last4, routing_last4)
SELECT id, 'First Commerce Bank', 'CHECKING', '4821', '0021' FROM borrower_profile WHERE business_name='Brightleaf Bakery';
INSERT INTO payment_account (borrower_profile_id, bank_name, account_type, account_last4, routing_last4)
SELECT id, 'Cascade Regional Bank', 'CHECKING', '7734', '0119' FROM borrower_profile WHERE business_name='North Pine Roofing';
INSERT INTO payment_account (borrower_profile_id, bank_name, account_type, account_last4, routing_last4)
SELECT id, 'Summit Business Bank', 'CHECKING', '2290', '0044' FROM borrower_profile WHERE business_name='Ironworks Fitness';

INSERT INTO notification (user_id, message, link, read, created_at)
SELECT bp.user_id, 'Your application #' || la.id || ' status changed to IN REVIEW', '/applications/' || la.id, true, now() - interval '5 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Brightleaf Bakery' AND la.requested_amount=45000.00;
INSERT INTO notification (user_id, message, link, read, created_at)
SELECT bp.user_id, 'A decision has been recorded on your application #' || la.id || ': APPROVE', '/applications/' || la.id, false, now() - interval '15 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='North Pine Roofing' AND la.status='APPROVED';
INSERT INTO notification (user_id, message, link, read, created_at)
SELECT bp.user_id, 'A decision has been recorded on your application #' || la.id || ': REJECT', '/applications/' || la.id, false, now() - interval '26 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Cedar Creek Cafe';
INSERT INTO notification (user_id, message, link, read, created_at)
SELECT bp.user_id, 'A decision has been recorded on your application #' || la.id || ': COUNTEROFFER', '/applications/' || la.id, false, now() - interval '3 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id WHERE bp.business_name='Ironworks Fitness';
INSERT INTO notification (user_id, message, link, read, created_at)
SELECT uw.id, 'Document uploaded for application #' || la.id, '/underwriter/applications/' || la.id, false, now() - interval '4 days'
FROM loan_application la JOIN borrower_profile bp ON la.borrower_profile_id=bp.id, app_user uw WHERE bp.business_name='Granite Peak Auto' AND uw.email='uw.priya@loanportal.test';

INSERT INTO outbox_email (to_address, subject, body, status, created_at) VALUES
  ('owner@brightleafbakery.test', 'Application status update', 'Your application status changed to IN_REVIEW.', 'SENT', now() - interval '5 days'),
  ('owner@northpineroofing.test', 'Your loan was approved', 'Congratulations, your application has been approved.', 'SENT', now() - interval '15 days');

INSERT INTO webhook_log (event_type, payload, status, attempted_at) VALUES
  ('credit_bureau.pull_requested', '{"applicationId": 1, "bureau": "mock-bureau"}', 'DELIVERED', now() - interval '5 days'),
  ('core_banking.disbursement_scheduled', '{"applicationId": 2}', 'DELIVERED', now() - interval '14 days');

INSERT INTO borrower_lead (business_name, contact_email, contact_phone, status, import_batch, created_at) VALUES
  ('Maple Street Deli', 'lead1@example.test', '555-010-0001', 'NEW', 'seed-batch-1', now() - interval '7 days'),
  ('Coastal Kayak Rentals', 'lead2@example.test', '555-010-0002', 'NEW', 'seed-batch-1', now() - interval '7 days'),
  ('Downtown Dry Cleaners', 'lead3@example.test', '555-010-0003', 'NEW', 'seed-batch-1', now() - interval '7 days');
