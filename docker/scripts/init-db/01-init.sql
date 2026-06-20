-- PATIENT SERVICE DATABASE
CREATE DATABASE patient_service;
ALTER DATABASE patient_service OWNER TO patient_admin;
\connect patient_service;
GRANT ALL PRIVILEGES
    ON DATABASE patient_service
    TO patient_admin;


-- AUTH SERVICE DATABASE
CREATE DATABASE auth_service;
ALTER DATABASE auth_service OWNER TO auth_admin;
\connect auth_service;
GRANT ALL PRIVILEGES
    ON DATABASE auth_service
    TO auth_admin;
