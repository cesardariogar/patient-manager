-- PATIENT SERVICE DATABASE
CREATE DATABASE patient_service_db;
ALTER DATABASE patient_service_db OWNER TO patient_admin;
\connect patient_service_db;
GRANT ALL PRIVILEGES
    ON DATABASE patient_service_db
    TO patient_admin;


-- AUTH SERVICE DATABASE
CREATE DATABASE auth_service_db;
ALTER DATABASE auth_service_db OWNER TO auth_admin;
\connect auth_service_db;
GRANT ALL PRIVILEGES
    ON DATABASE auth_service_db
    TO auth_admin;
