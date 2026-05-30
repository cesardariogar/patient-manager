-- Create the table if it doesn't already exist
CREATE TABLE IF NOT EXISTS patient(
    id              UUID PRIMARY KEY    NOT NULL,
    name            VARCHAR(255)        NOT NULL,
    last_name       VARCHAR(255)        NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    address         VARCHAR(255)        NOT NULL,
    date_of_birth   DATE                NOT NULL,
    register_date   DATE                NOT NULL
);

-- Insert data only if the patient does not already exist (based on email)
INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'b1a7e8c2-1234-4f56-9abc-1a2b3c4d5e6f', 'John', 'Doe', 'john.doe@example.com', '123 Main St', '1985-04-12', '2024-06-01'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'john.doe@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'e1d2c3b4-1111-2222-3333-444455556666', 'Michael', 'Johnson', 'michael.johnson@example.com', '101 Maple St', '1982-07-15', '2024-06-04'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'michael.johnson@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'f2e3d4c5-2222-3333-4444-555566667777', 'Emily', 'Davis', 'emily.davis@example.com', '202 Birch Ln', '1995-03-22', '2024-06-05'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'emily.davis@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'a3b4c5d6-3333-4444-5555-666677778888', 'David', 'Wilson', 'david.wilson@example.com', '303 Cedar Ct', '1970-11-30', '2024-06-06'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'david.wilson@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'b4c5d6e7-4444-5555-6666-777788889999', 'Sarah', 'Martinez', 'sarah.martinez@example.com', '404 Spruce Dr', '1988-02-10', '2024-06-07'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'sarah.martinez@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'c5d6e7f8-5555-6666-7777-888899990000', 'Chris', 'Lee', 'chris.lee@example.com', '505 Walnut Ave', '1992-08-19', '2024-06-08'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'chris.lee@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'd6e7f8a9-6666-7777-8888-999900001111', 'Jessica', 'Taylor', 'jessica.taylor@example.com', '606 Chestnut Blvd', '1980-05-25', '2024-06-09'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'jessica.taylor@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'e7f8a9b0-7777-8888-9999-000011112222', 'Matthew', 'Anderson', 'matthew.anderson@example.com', '707 Willow Way', '1975-09-14', '2024-06-10'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'matthew.anderson@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'f8a9b0c1-8888-9999-0000-111122223333', 'Ashley', 'Thomas', 'ashley.thomas@example.com', '808 Aspen Pl', '1998-12-03', '2024-06-11'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'ashley.thomas@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'a9b0c1d2-9999-0000-1111-222233334444', 'Joshua', 'Moore', 'joshua.moore@example.com', '909 Poplar Rd', '1983-06-27', '2024-06-12'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'joshua.moore@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'b0c1d2e3-0000-1111-2222-333344445555', 'Amanda', 'Jackson', 'amanda.jackson@example.com', '1001 Magnolia Cir', '1991-10-08', '2024-06-13'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'amanda.jackson@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT 'd7659a30-8c0b-46de-b1c8-729d934450c7', 'Dari', 'Yonki', 'dari.yonki@example.com', '1424 Lago ontareo', '1988-06-07', '2025-09-25'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'dari.yonki@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT '42aeaaf3-6991-464f-85bc-dddbafca2d25', 'Jose', 'Peri', 'jose.peri@example.com', '531 perreo', '1993-03-27', '2024-06-12'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'jose.peri@example.com');

INSERT INTO patient (id, name, last_name, email, address, date_of_birth, register_date)
SELECT '840165dd-a572-417f-be3a-3e1a406d62b6', 'Daniel', 'Revos', 'daniel.revos@example.com', '023 gideon street', '1977-03-09', '2023-03-15'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE email = 'daniel.revos@example.com');
