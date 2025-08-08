CREATE TABLE drugs (
    drug_id SERIAL PRIMARY KEY,
    drug_name TEXT NOT NULL,
    drug_form_id INTEGER NOT NULL,
    expiration_date TIMESTAMPTZ,
    drug_description TEXT,
    CONSTRAINT chk_drugs_description_len CHECK (char_length(drug_description) <= 2000),
    CONSTRAINT fk_drugs_form
        FOREIGN KEY (drug_form_id)
        REFERENCES drugs_form(drug_form_id)
);