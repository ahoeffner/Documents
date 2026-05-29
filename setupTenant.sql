-- Run once per tenant schema.
-- Usage: psql -v schema=<tenant_name> -f setup_tenant.sql
-- Requires setup_public.sql to have been run first.

CREATE SCHEMA IF NOT EXISTS slotsdalen;



CREATE TABLE documents.languages
(
    id   varchar(10)  NOT NULL PRIMARY KEY,
    lang varchar(15)  NOT NULL
);

INSERT INTO documents.languages (id, lang) VALUES ('DA', 'danish');
INSERT INTO documents.languages (id, lang) VALUES ('EN', 'english');


CREATE TABLE documents.folders
(
    id   serial      NOT NULL PRIMARY KEY,
    pid  integer,
    name varchar(40) NOT NULL
);


CREATE TABLE documents.documents
(
    id      serial       NOT NULL PRIMARY KEY,
    fldid   integer      NOT NULL,
    date    date         NOT NULL,
    title   varchar(40)  NOT NULL,
    text    text,
    extref  varchar(160),
    content bytea
);

ALTER TABLE documents.documents ADD CONSTRAINT doccat
    FOREIGN KEY (fldid) REFERENCES documents.folders (id);


CREATE TABLE documents.textchunks
(
    docid     integer       NOT NULL,
    line      integer       NOT NULL,
    section	  varchar(30)   NOT NULL,
    lang      varchar       NOT NULL DEFAULT 'danish',
    text      text          NOT NULL,
    lexvector tsvector,
    embedding public.halfvec(3072),
    PRIMARY KEY (docid, section, line)
);

ALTER TABLE documents.textchunks ADD CONSTRAINT doctext
    FOREIGN KEY (docid) REFERENCES documents.documents (id)
    ON DELETE CASCADE;

-- Full-text search index
CREATE INDEX textchunks_lex ON documents.textchunks USING gin(lexvector);

CREATE INDEX textchunks_embedding ON documents.textchunks USING hnsw (embedding halfvec_cosine_ops)
WITH (m = 32, ef_construction = 128); -- Boosted for high-dimensional accuracy


CREATE TRIGGER ginvec
    BEFORE INSERT OR UPDATE ON documents.textchunks
    FOR EACH ROW EXECUTE FUNCTION public.ginvec();

CREATE TRIGGER embeddings
    BEFORE INSERT OR UPDATE ON documents.textchunks
    FOR EACH ROW EXECUTE FUNCTION public.embeddings();


-- 2. Grant rights on all EXISTING tables, sequences, and functions
GRANT ALL PRIVILEGES ON SCHEMA slotsdalen TO alex;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA slotsdalen TO alex;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA slotsdalen TO alex;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA slotsdalen TO alex;

-- 3. Grant rights on all FUTURE tables and sequences automatically
ALTER DEFAULT PRIVILEGES IN SCHEMA slotsdalen
GRANT ALL PRIVILEGES ON TABLES TO alex;

ALTER DEFAULT PRIVILEGES IN SCHEMA slotsdalen
GRANT ALL PRIVILEGES ON SEQUENCES TO alex;