CREATE INDEX IF NOT EXISTS idx_drug_expiration_date ON drugs (expiration_date);

CREATE INDEX IF NOT EXISTS idx_drug_lower_name ON drugs (LOWER(drug_name));

CREATE INDEX IF NOT EXISTS idx_drug_form_id ON drugs (drug_form_id);

CREATE INDEX IF NOT EXISTS idx_drugs_form_lower_name ON drugs_form (LOWER(name));