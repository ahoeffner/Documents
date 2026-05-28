-- Run once per tenant schema.
-- Usage: psql -v schema=<tenant_name> -f setup_tenant.sql
-- Requires setup_public.sql to have been run first.

CREATE SCHEMA IF NOT EXISTS :schema;

SET search_path TO :schema, public;


DROP TABLE IF EXISTS textchunks;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS languages;


CREATE TABLE languages
(
    id   varchar(10)  NOT NULL PRIMARY KEY,
    lang varchar(15)  NOT NULL
);

INSERT INTO languages (id, lang) VALUES ('DA', 'danish');
INSERT INTO languages (id, lang) VALUES ('EN', 'english');


CREATE TABLE categories
(
    id   serial      NOT NULL PRIMARY KEY,
    name varchar(15) NOT NULL
);

INSERT INTO categories (name) VALUES ('Receipts');


CREATE TABLE documents
(
    id      serial       NOT NULL PRIMARY KEY,
    catid   integer      NOT NULL,
    date    date         NOT NULL,
    title   varchar(40)  NOT NULL,
    text    text,
    extref  varchar(160),
    content bytea
);

ALTER TABLE documents ADD CONSTRAINT doccat
    FOREIGN KEY (catid) REFERENCES categories (id);


CREATE TABLE textchunks
(
    docid     integer NOT NULL,
    line      integer NOT NULL,
    text      text    NOT NULL,
    lang      varchar NOT NULL DEFAULT 'danish',
    lexvector tsvector,
    embedding vector(768),
    PRIMARY KEY (docid, line)
);

ALTER TABLE textchunks ADD CONSTRAINT doctext
    FOREIGN KEY (docid) REFERENCES documents (id)
    ON DELETE CASCADE;

-- Full-text search index
CREATE INDEX textchunks_lex ON textchunks USING gin(lexvector);

-- Vector similarity index (HNSW, cosine distance — best recall for semantic search)
CREATE INDEX textchunks_embedding ON textchunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);


CREATE TRIGGER ginvec
    BEFORE INSERT OR UPDATE ON textchunks
    FOR EACH ROW EXECUTE FUNCTION public.ginvec();

CREATE TRIGGER embeddings
    BEFORE INSERT OR UPDATE ON textchunks
    FOR EACH ROW EXECUTE FUNCTION public.embeddings();
