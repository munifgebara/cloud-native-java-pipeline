-- DB do seu sistema
CREATE USER stella WITH PASSWORD 'stella';
CREATE DATABASE stella OWNER stella;
GRANT ALL PRIVILEGES ON DATABASE stella TO stella;

-- Extensao pgvector (usada pela migration V0018). Criada aqui como superusuario,
-- pois o usuario "stella" nao tem privilegio para CREATE EXTENSION. A migration
-- roda "create extension if not exists vector" e apenas confirma a existencia.
\connect stella
CREATE EXTENSION IF NOT EXISTS vector;
\connect postgres

-- DB do Keycloak
CREATE USER keycloak WITH PASSWORD 'keycloak';
CREATE DATABASE keycloak OWNER keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

