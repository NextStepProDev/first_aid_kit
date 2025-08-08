CREATE TABLE drugs_form (
    drug_form_id SERIAL PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);