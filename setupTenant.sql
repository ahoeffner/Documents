-- Run once per tenant schema.
-- Usage: psql -v schema=<tenant_name> -f setup_tenant.sql
-- Requires setup_public.sql to have been run first.

CREATE SCHEMA IF NOT EXISTS documents;



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
    name varchar(15) NOT NULL
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
    docid     integer NOT NULL,
    line      integer NOT NULL,
    text      text    NOT NULL,
    lang      varchar NOT NULL DEFAULT 'danish',
    lexvector tsvector,
    embedding public.vector(768),
    PRIMARY KEY (docid, line)
);

ALTER TABLE documents.textchunks ADD CONSTRAINT doctext
    FOREIGN KEY (docid) REFERENCES documents.documents (id)
    ON DELETE CASCADE;

-- Full-text search index
CREATE INDEX textchunks_lex ON documents.textchunks USING gin(lexvector);

-- Vector similarity index (HNSW, cosine distance — best recall for semantic search)
CREATE INDEX textchunks_embedding ON documents.textchunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);


CREATE TRIGGER ginvec
    BEFORE INSERT OR UPDATE ON documents.textchunks
    FOR EACH ROW EXECUTE FUNCTION public.ginvec();

CREATE TRIGGER embeddings
    BEFORE INSERT OR UPDATE ON documents.textchunks
    FOR EACH ROW EXECUTE FUNCTION public.embeddings();
