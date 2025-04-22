CREATE TABLE drugs (
    drugs_id SERIAL PRIMARY KEY,
    drugs_name TEXT NOT NULL,
    drugs_form_id INTEGER NOT NULL,
    expiration_date TIMESTAMPTZ,
    drugs_description TEXT,
    CONSTRAINT fk_drugs_form
        FOREIGN KEY (drugs_form_id)
        REFERENCES drugs_form(drugs_form_id)
);