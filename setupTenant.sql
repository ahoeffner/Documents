-- Run once per tenant schema.
-- Usage: psql -v schema=<tenant_name> -f setup_tenant.sql
-- Requires setup_public.sql to have been run first.

CREATE SCHEMA IF NOT EXISTS documents;



CREATE TABLE documents.folders
(
    id   serial      NOT NULL PRIMARY KEY,
    pid  integer,
    name varchar(40) NOT NULL
);


CREATE TABLE documents.links
(
    id serial      NOT NULL PRIMARY KEY,
    fldid integer  NOT NULL,
    docid integer  NOT NULL
);


ALTER TABLE documents.links ADD CONSTRAINT fldkey
    FOREIGN KEY (fldid) REFERENCES documents.folders (id) ON DELETE CASCADE;;

ALTER TABLE documents.links ADD CONSTRAINT dockey
    FOREIGN KEY (docid) REFERENCES documents.documents (id) ON DELETE CASCADE;;


CREATE TABLE documents.documents
(
    id        serial       NOT NULL PRIMARY KEY,
    fldid     integer      NOT NULL,
    date      date         NOT NULL,
    title     varchar(40)  NOT NULL,
    text      text,
    extref    varchar(160),
    doctext   text,
    lexvector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(doctext, ''))) STORED,
    content   bytea
);

ALTER TABLE documents.documents ADD CONSTRAINT dockey
    FOREIGN KEY (fldid) REFERENCES documents.folders (id);

-- Full-text search index
CREATE INDEX documents_lex ON documents.documents USING gin(lexvector);


CREATE TABLE documents.textchunks
(
    docid     integer       NOT NULL,
    line      integer       NOT NULL,
    section	  varchar(30)   NOT NULL,
    text      text          NOT NULL,
    embedding public.halfvec(3072),
    PRIMARY KEY (docid, section, line)
);

ALTER TABLE documents.textchunks ADD CONSTRAINT doctext
    FOREIGN KEY (docid) REFERENCES documents.documents (id)
    ON DELETE CASCADE;

CREATE INDEX textchunks_embedding ON documents.textchunks USING hnsw (embedding halfvec_cosine_ops)
WITH (m = 32, ef_construction = 128); -- Boosted for high-dimensional accuracy


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